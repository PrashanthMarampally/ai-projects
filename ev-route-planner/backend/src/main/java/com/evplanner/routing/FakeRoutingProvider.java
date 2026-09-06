package com.evplanner.routing;

import com.evplanner.journey.GeoPoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@ConditionalOnProperty(name = "evplanner.routing.provider", havingValue = "fake", matchIfMissing = true)
public class FakeRoutingProvider implements RoutingProvider {

    @Override
    public RouteResult calculateRoute(
            GeoPoint origin,
            GeoPoint destination) {

        return buildRoute(origin, destination, 275, 330, 0.0);
    }

    @Override
    public List<RouteResult> calculateRoutes(
            GeoPoint origin,
            GeoPoint destination) {

        // Synthetic alternatives used by the MVP until Google/Mappls is wired in.
        return List.of(
                buildRoute(origin, destination, 275, 330, 0.0),
                buildRoute(origin, destination, 260, 350, 0.015),
                buildRoute(origin, destination, 290, 300, -0.015)
        );
    }

    private RouteResult buildRoute(
            GeoPoint origin,
            GeoPoint destination,
            double distanceKm,
            int durationMinutes,
            double longitudeOffset) {

        GeoPoint midpoint = new GeoPoint(
                origin.latitude()
                        .add(destination.latitude())
                        .divide(BigDecimal.valueOf(2)),
                origin.longitude()
                        .add(destination.longitude())
                        .divide(BigDecimal.valueOf(2))
                        .add(BigDecimal.valueOf(longitudeOffset))
        );

        return new RouteResult(
                BigDecimal.valueOf(distanceKm),
                durationMinutes,
                List.of(origin, midpoint, destination)
        );
    }
}
