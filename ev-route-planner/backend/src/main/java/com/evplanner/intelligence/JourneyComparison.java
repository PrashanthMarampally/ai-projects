package com.evplanner.intelligence;

import com.evplanner.journey.JourneyOption;

import java.math.BigDecimal;

/**
 * Side-by-side comparison of two journey options.
 *
 * <p>Each delta field is computed as (optionB - optionA), so a positive
 * value means option B is better in that dimension.
 */
public record JourneyComparison(

        JourneyOption optionA,
        JourneyOption optionB,

        /** Positive means B has better time efficiency. */
        BigDecimal timeEfficiencyDelta,

        /** Positive means B arrives with more SOC buffer. */
        BigDecimal arrivalBufferDelta,

        /** Positive means B offers a better charging experience. */
        BigDecimal chargingExperienceDelta,

        /** Positive means B is safer (higher minimum SOC). */
        BigDecimal safetyDelta,

        /** Positive means B has a higher overall score. */
        BigDecimal overallDelta
) {

    /**
     * Compares two journey options using their embedded score breakdowns.
     *
     * @throws IllegalArgumentException when either option lacks a score breakdown
     */
    public static JourneyComparison of(JourneyOption a, JourneyOption b) {

        JourneyScore sa = a.scoreBreakdown();
        JourneyScore sb = b.scoreBreakdown();

        if (sa == null || sb == null) {
            throw new IllegalArgumentException(
                    "Both options must have a score breakdown to compare");
        }

        return new JourneyComparison(
                a,
                b,
                sb.timeEfficiency().subtract(sa.timeEfficiency()),
                sb.arrivalBuffer().subtract(sa.arrivalBuffer()),
                sb.chargingExperience().subtract(sa.chargingExperience()),
                sb.safety().subtract(sa.safety()),
                sb.overall().subtract(sa.overall()));
    }

    /** Returns {@code true} when option B outscores option A overall. */
    public boolean isBBetter() {
        return overallDelta.compareTo(BigDecimal.ZERO) > 0;
    }
}
