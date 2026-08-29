package com.evplanner.routing;

import com.evplanner.journey.GeoPoint;
import com.evplanner.station.ChargingStation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class RouteStationPositionCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    private final GeometryFactory geometryFactory =
            new GeometryFactory();

    public RouteStationPosition calculate(
            RouteResult route,
            ChargingStation station) {

        double accumulatedDistanceKm = 0.0;
        double bestDistanceToRouteKm = Double.MAX_VALUE;
        double bestDistanceFromOriginKm = 0.0;

        for (int i = 0; i < route.geometry().size(); i++) {

            GeoPoint routePoint = route.geometry().get(i);

            double distanceToStationKm =
                    haversineKm(
                            routePoint.latitude().doubleValue(),
                            routePoint.longitude().doubleValue(),
                            station.getLatitude().doubleValue(),
                            station.getLongitude().doubleValue()
                    );

            if (distanceToStationKm < bestDistanceToRouteKm) {
                bestDistanceToRouteKm = distanceToStationKm;
                bestDistanceFromOriginKm = accumulatedDistanceKm;
            }

            if (i < route.geometry().size() - 1) {
                GeoPoint nextPoint = route.geometry().get(i + 1);

                accumulatedDistanceKm +=
                        haversineKm(
                                routePoint.latitude().doubleValue(),
                                routePoint.longitude().doubleValue(),
                                nextPoint.latitude().doubleValue(),
                                nextPoint.longitude().doubleValue()
                        );
            }
        }

        double totalRouteDistanceKm =
                route.distanceKm().doubleValue();

        double distanceFromOriginKm =
                Math.min(
                        bestDistanceFromOriginKm,
                        totalRouteDistanceKm
                );

        double distanceToDestinationKm =
                Math.max(
                        0.0,
                        totalRouteDistanceKm - distanceFromOriginKm
                );

        return new RouteStationPosition(
                station,
                BigDecimal.valueOf(distanceFromOriginKm)
                        .setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(distanceToDestinationKm)
                        .setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(bestDistanceToRouteKm)
                        .setScale(2, RoundingMode.HALF_UP)
        );
    }

    private LineString createRouteLine(
            RouteResult route) {

        Coordinate[] coordinates =
                route.geometry()
                        .stream()
                        .map(point ->
                                new Coordinate(
                                        point.longitude().doubleValue(),
                                        point.latitude().doubleValue()
                                ))
                        .toArray(Coordinate[]::new);

        return geometryFactory.createLineString(coordinates);
    }

    private double calculateMinimumDistanceKm(
            java.util.List<GeoPoint> routePoints,
            ChargingStation station) {

        double minimum = Double.MAX_VALUE;

        for (GeoPoint point : routePoints) {

            double distance =
                    haversineKm(
                            point.latitude().doubleValue(),
                            point.longitude().doubleValue(),
                            station.getLatitude().doubleValue(),
                            station.getLongitude().doubleValue()
                    );

            minimum = Math.min(minimum, distance);
        }

        return minimum;
    }

    private double haversineKm(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

        double latDistance =
                Math.toRadians(lat2 - lat1);

        double lonDistance =
                Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(latDistance / 2)
                        * Math.sin(latDistance / 2)
                        +
                        Math.cos(Math.toRadians(lat1))
                                * Math.cos(Math.toRadians(lat2))
                                * Math.sin(lonDistance / 2)
                                * Math.sin(lonDistance / 2);

        double c =
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}