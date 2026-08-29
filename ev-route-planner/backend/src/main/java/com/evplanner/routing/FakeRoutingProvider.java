package com.evplanner.routing;

import com.evplanner.journey.GeoPoint;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class FakeRoutingProvider implements RoutingProvider {

    @Override
    public RouteResult calculateRoute(
            GeoPoint origin,
            GeoPoint destination) {

        GeoPoint midpoint = new GeoPoint(
                origin.latitude()
                        .add(destination.latitude())
                        .divide(BigDecimal.valueOf(2)),
                origin.longitude()
                        .add(destination.longitude())
                        .divide(BigDecimal.valueOf(2))
        );

        return new RouteResult(
                BigDecimal.valueOf(275),
                330,
                List.of(
                        origin,
                        midpoint,
                        destination
                )
        );
    }
}