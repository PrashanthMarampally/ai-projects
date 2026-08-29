package com.evplanner.journey;

import java.math.BigDecimal;

public record JourneyRequest(
        GeoPoint origin,
        GeoPoint destination,
        Long vehicleId,
        BigDecimal currentSocPercent,
        BigDecimal minimumArrivalSocPercent
) {
}