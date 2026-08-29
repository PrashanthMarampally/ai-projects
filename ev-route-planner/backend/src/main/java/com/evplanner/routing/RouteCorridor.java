package com.evplanner.routing;

import com.evplanner.journey.GeoPoint;

import java.util.List;

public record RouteCorridor(
        List<GeoPoint> points,
        double radiusKm
) {
    public RouteCorridor {
        if (points == null || points.size() < 2) {
            throw new IllegalArgumentException(
                    "Route corridor requires at least two points"
            );
        }

        if (radiusKm <= 0) {
            throw new IllegalArgumentException(
                    "Route corridor radius must be greater than zero"
            );
        }
    }
}