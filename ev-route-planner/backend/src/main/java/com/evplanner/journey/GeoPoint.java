package com.evplanner.journey;

import java.math.BigDecimal;

public record GeoPoint(
        BigDecimal latitude,
        BigDecimal longitude
) {
}