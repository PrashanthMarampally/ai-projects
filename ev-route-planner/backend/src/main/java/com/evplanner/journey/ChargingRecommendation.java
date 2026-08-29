package com.evplanner.journey;

import java.math.BigDecimal;

public record ChargingRecommendation(
        Long stationId,
        BigDecimal arrivalSocPercent,
        BigDecimal targetSocPercent,
        Integer estimatedChargingMinutes,
        BigDecimal reliabilityScore
) {
}