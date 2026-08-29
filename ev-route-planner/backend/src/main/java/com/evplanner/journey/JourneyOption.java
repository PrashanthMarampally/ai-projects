package com.evplanner.journey;

import java.math.BigDecimal;
import java.util.List;

public record JourneyOption(
        BigDecimal distanceKm,
        Integer estimatedDurationMinutes,
        List<ChargingRecommendation> chargingRecommendations,
        BigDecimal minimumSocPercent,
        BigDecimal overallScore
) {
}