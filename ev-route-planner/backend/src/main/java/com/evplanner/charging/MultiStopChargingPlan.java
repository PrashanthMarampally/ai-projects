package com.evplanner.charging;

import com.evplanner.journey.ChargingStopPlan;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.util.List;

/** Result of multi-stop charging optimization for one route. */
public record MultiStopChargingPlan(
        List<ChargingStopPlan> chargingStops,
        BigDecimal totalChargingMinutes,
        BigDecimal totalDetourKm
) {
    /** Backward-compatible accessor for existing internal callers/tests. */
    @JsonIgnore
    public List<ChargingStopPlan> chargingRecommendations() {
        return chargingStops;
    }
}
