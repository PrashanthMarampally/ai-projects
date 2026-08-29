package com.evplanner.routing;

import com.evplanner.journey.GeoPoint;

import java.math.BigDecimal;
import java.util.List;

public record RouteResult(
        BigDecimal distanceKm,
        int durationMinutes,
        List<GeoPoint> geometry
) {
}