package com.evplanner.journey;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.util.List;

public record JourneyOption(
        BigDecimal distanceKm,
        Integer estimatedDurationMinutes,
        List<ChargingStopPlan> chargingStops,
        BigDecimal minimumSocPercent,
        BigDecimal overallScore
) {
    /** Backward-compatible view for existing callers. */
    @JsonIgnore
    public List<ChargingRecommendation> chargingRecommendations() {
        return chargingStops.stream()
                .map(stop -> new ChargingRecommendation(
                        stop.stationId(),
                        stop.arrivalSocPercent(),
                        stop.targetSocPercent(),
                        stop.estimatedChargingMinutes(),
                        BigDecimal.valueOf(100)))
                .toList();
    }
}
