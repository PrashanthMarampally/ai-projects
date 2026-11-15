package com.evplanner.intelligence;

import java.math.BigDecimal;

/**
 * Breakdown of how a journey option scored across four dimensions.
 *
 * All component scores are 0–100 (higher is better).
 * The overall score is the weighted sum of the components.
 *
 * Weights:
 *   time efficiency   40 %
 *   arrival buffer    25 %
 *   charging exp.     20 %
 *   safety            15 %
 */
public record JourneyScore(

        /** Drive time / total journey time ratio, penalised for charging and detour. */
        BigDecimal timeEfficiency,

        /** Margin of SOC above the requested minimum at the destination. */
        BigDecimal arrivalBuffer,

        /** Fewer stops and faster chargers produce a higher score. */
        BigDecimal chargingExperience,

        /** Lowest SOC reached at any point – proxy for range-anxiety risk. */
        BigDecimal safety,

        /** Weighted overall score. */
        BigDecimal overall
) {
    static final double WEIGHT_TIME       = 0.40;
    static final double WEIGHT_BUFFER     = 0.25;
    static final double WEIGHT_EXPERIENCE = 0.20;
    static final double WEIGHT_SAFETY     = 0.15;
}
