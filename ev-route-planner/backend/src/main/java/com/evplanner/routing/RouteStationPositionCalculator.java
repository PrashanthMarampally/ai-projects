package com.evplanner.routing;

import com.evplanner.journey.GeoPoint;
import com.evplanner.station.ChargingStation;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Locates a charging station against the route polyline.
 *
 * The route provider gives us a sequence of geographic points.  For each
 * route segment we project the station onto that segment using a local
 * equirectangular projection.  This is considerably more accurate than
 * choosing the nearest route vertex, especially when route points are far
 * apart.
 */
@Service
public class RouteStationPositionCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0088;
    private static final double METERS_PER_DEGREE_LATITUDE = 111.32;

    public RouteStationPosition calculate(
            RouteResult route,
            ChargingStation station) {

        validate(route, station);

        List<GeoPoint> routePoints = route.geometry();
        double stationLat = station.getLatitude().doubleValue();
        double stationLon = station.getLongitude().doubleValue();

        double geometryDistanceKm = 0.0;
        double bestDistanceToRouteKm = Double.MAX_VALUE;
        double bestDistanceFromOriginKm = 0.0;

        for (int i = 0; i < routePoints.size() - 1; i++) {
            GeoPoint start = routePoints.get(i);
            GeoPoint end = routePoints.get(i + 1);

            double segmentLengthKm = haversineKm(
                    start.latitude().doubleValue(),
                    start.longitude().doubleValue(),
                    end.latitude().doubleValue(),
                    end.longitude().doubleValue());

            Projection projection = projectOntoSegment(
                    start.latitude().doubleValue(),
                    start.longitude().doubleValue(),
                    end.latitude().doubleValue(),
                    end.longitude().doubleValue(),
                    stationLat,
                    stationLon);

            double distanceToRouteKm = haversineKm(
                    projection.latitude(),
                    projection.longitude(),
                    stationLat,
                    stationLon);

            double distanceFromGeometryOriginKm =
                    geometryDistanceKm + segmentLengthKm * projection.fraction();

            if (distanceToRouteKm < bestDistanceToRouteKm) {
                bestDistanceToRouteKm = distanceToRouteKm;
                bestDistanceFromOriginKm = distanceFromGeometryOriginKm;
            }

            geometryDistanceKm += segmentLengthKm;
        }

        if (geometryDistanceKm <= 0.0) {
            throw new IllegalArgumentException("Route geometry must contain distinct points");
        }

        // The routing provider's reported distance is authoritative.  Scale
        // the polyline position so that geometry-based position and provider
        // distance remain consistent when the two differ slightly.
        double providerDistanceKm = route.distanceKm().doubleValue();
        double scale = providerDistanceKm / geometryDistanceKm;
        double distanceFromOriginKm = Math.max(
                0.0,
                Math.min(
                        bestDistanceFromOriginKm * scale,
                        providerDistanceKm));

        double distanceToDestinationKm = Math.max(
                0.0,
                providerDistanceKm - distanceFromOriginKm);

        return new RouteStationPosition(
                station,
                round(distanceFromOriginKm),
                round(distanceToDestinationKm),
                round(bestDistanceToRouteKm));
    }

    private Projection projectOntoSegment(
            double startLat,
            double startLon,
            double endLat,
            double endLon,
            double stationLat,
            double stationLon) {

        // Local equirectangular projection around the station latitude.
        // Accuracy is more than adequate for the small station/route
        // corridor distances used by the MVP.
        double cosLat = Math.cos(Math.toRadians(stationLat));
        double metersPerDegreeLon = METERS_PER_DEGREE_LATITUDE * cosLat;

        double startX = startLon * metersPerDegreeLon;
        double startY = startLat * METERS_PER_DEGREE_LATITUDE;
        double endX = endLon * metersPerDegreeLon;
        double endY = endLat * METERS_PER_DEGREE_LATITUDE;
        double stationX = stationLon * metersPerDegreeLon;
        double stationY = stationLat * METERS_PER_DEGREE_LATITUDE;

        double dx = endX - startX;
        double dy = endY - startY;
        double segmentLengthSquared = dx * dx + dy * dy;

        if (segmentLengthSquared == 0.0) {
            return new Projection(startLat, startLon, 0.0);
        }

        double fraction = ((stationX - startX) * dx
                + (stationY - startY) * dy) / segmentLengthSquared;
        fraction = Math.max(0.0, Math.min(1.0, fraction));

        return new Projection(
                startLat + (endLat - startLat) * fraction,
                startLon + (endLon - startLon) * fraction,
                fraction);
    }

    private BigDecimal round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void validate(RouteResult route, ChargingStation station) {
        if (route == null) {
            throw new IllegalArgumentException("Route must not be null");
        }
        if (route.geometry() == null || route.geometry().size() < 2) {
            throw new IllegalArgumentException(
                    "Route geometry must contain at least two points");
        }
        if (route.distanceKm() == null || route.distanceKm().signum() < 0) {
            throw new IllegalArgumentException(
                    "Route distance must not be null or negative");
        }
        if (station == null
                || station.getLatitude() == null
                || station.getLongitude() == null) {
            throw new IllegalArgumentException(
                    "Charging station coordinates must not be null");
        }
    }

    private double haversineKm(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                        + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2)
                        * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private record Projection(
            double latitude,
            double longitude,
            double fraction) {
    }
}
