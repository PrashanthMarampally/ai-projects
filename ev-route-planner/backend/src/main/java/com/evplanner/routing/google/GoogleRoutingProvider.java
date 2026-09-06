package com.evplanner.routing.google;

import com.evplanner.journey.GeoPoint;
import com.evplanner.routing.RouteResult;
import com.evplanner.routing.RoutingProvider;
import tools.jackson.databind.JsonNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "evplanner.routing.provider", havingValue = "google")
@EnableConfigurationProperties(GoogleRoutingProperties.class)
public class GoogleRoutingProvider implements RoutingProvider {

    private final GoogleRoutesClient client;

    public GoogleRoutingProvider(GoogleRoutesClient client) {
        this.client = client;
    }

    @Override
    public RouteResult calculateRoute(GeoPoint origin, GeoPoint destination) {
        List<RouteResult> routes = calculateRoutes(origin, destination);
        if (routes.isEmpty()) {
            throw new GoogleRoutingException("Google Routes API returned no routes");
        }
        return routes.get(0);
    }

    @Override
    public List<RouteResult> calculateRoutes(GeoPoint origin, GeoPoint destination) {
        JsonNode response = client.computeRoutes(origin, destination);
        JsonNode routes = response.path("routes");

        if (!routes.isArray()) {
            throw new GoogleRoutingException("Google Routes API response does not contain routes");
        }

        List<RouteResult> result = new ArrayList<>();
        for (JsonNode route : routes) {
            result.add(toRouteResult(route));
        }
        return result;
    }

    private RouteResult toRouteResult(JsonNode route) {
        long distanceMeters = route.path("distanceMeters").asLong(-1);
        if (distanceMeters < 0) {
            throw new GoogleRoutingException("Google route is missing distanceMeters");
        }

        String duration = route.path("duration").asText(null);
        if (duration == null || !duration.endsWith("s")) {
            throw new GoogleRoutingException("Google route is missing a valid duration");
        }

        double seconds = Double.parseDouble(duration.substring(0, duration.length() - 1));
        int durationMinutes = (int) Math.ceil(seconds / 60.0);

        List<GeoPoint> geometry = parseGeoJsonLineString(
                route.path("polyline").path("geoJsonLinestring"));

        return new RouteResult(
                BigDecimal.valueOf(distanceMeters)
                        .divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP),
                durationMinutes,
                geometry);
    }

    private List<GeoPoint> parseGeoJsonLineString(JsonNode lineString) {
        JsonNode coordinates = lineString.path("coordinates");
        if (!coordinates.isArray() || coordinates.size() < 2) {
            throw new GoogleRoutingException("Google route does not contain a usable geometry");
        }

        List<GeoPoint> points = new ArrayList<>(coordinates.size());
        for (JsonNode coordinate : coordinates) {
            if (!coordinate.isArray() || coordinate.size() < 2) {
                throw new GoogleRoutingException("Invalid GeoJSON route coordinate");
            }

            // GeoJSON is [longitude, latitude]. Our domain is latitude, longitude.
            points.add(new GeoPoint(
                    coordinate.get(1).decimalValue(),
                    coordinate.get(0).decimalValue()));
        }
        return points;
    }
}
