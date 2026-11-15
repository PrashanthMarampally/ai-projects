package com.evplanner.intelligence;

import com.evplanner.charging.MultiStopChargingPlan;
import com.evplanner.journey.ChargingStopPlan;
import com.evplanner.journey.JourneyRequest;
import com.evplanner.routing.RouteResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Scores a single journey option across four dimensions and produces
 * a {@link JourneyScore} that drives option ranking and comparison.
 */
@Service
public class JourneyEvaluator {

    /**
     * Any charger at or above this power level scores full marks for
     * charging experience. 150 kW is a common DC fast-charger ceiling
     * in India today.
     */
    private static final double REFERENCE_CHARGER_KW = 150.0;

    /**
     * Maximum number of stops the planner will ever produce; used to
     * normalise the stop-count penalty in the experience dimension.
     */
    private static final int MAX_STOPS = 4;

    /**
     * Below this SOC percentage the safety score reaches zero.
     * Arriving at a station with less than 5 % is considered maximum risk.
     */
    private static final double SAFETY_FLOOR_PERCENT = 5.0;

    /**
     * Evaluates a planned journey option and returns its score breakdown.
     *
     * @param route            the route being evaluated
     * @param request          the original journey request (supplies the minimum
     *                         arrival SOC preference)
     * @param chargingPlan     the charging plan chosen for this route, or {@code null}
     *                         when no charging is required
     * @param plannedArrivalSoc estimated SOC at the destination after any charging
     */
    public JourneyScore evaluate(
            RouteResult route,
            JourneyRequest request,
            MultiStopChargingPlan chargingPlan,
            BigDecimal plannedArrivalSoc) {

        BigDecimal timeScore       = scoreTimeEfficiency(route, chargingPlan);
        BigDecimal bufferScore     = scoreArrivalBuffer(plannedArrivalSoc, request.minimumArrivalSocPercent());
        BigDecimal experienceScore = scoreChargingExperience(chargingPlan);
        BigDecimal safetyScore     = scoreSafety(chargingPlan, plannedArrivalSoc);

        BigDecimal overall = BigDecimal.valueOf(
                JourneyScore.WEIGHT_TIME       * timeScore.doubleValue()
                        + JourneyScore.WEIGHT_BUFFER     * bufferScore.doubleValue()
                        + JourneyScore.WEIGHT_EXPERIENCE * experienceScore.doubleValue()
                        + JourneyScore.WEIGHT_SAFETY     * safetyScore.doubleValue())
                .setScale(2, RoundingMode.HALF_UP);

        return new JourneyScore(
                round(timeScore),
                round(bufferScore),
                round(experienceScore),
                round(safetyScore),
                overall);
    }

    // -------------------------------------------------------------------------
    // Dimension: time efficiency
    // -------------------------------------------------------------------------

    /**
     * Ratio of pure drive time to total journey time (drive + charging +
     * detour to reach charging stations).  A route that needs no charging
     * scores 100.  The penalty grows with charging minutes and detour distance.
     */
    private BigDecimal scoreTimeEfficiency(
            RouteResult route,
            MultiStopChargingPlan plan) {

        if (plan == null || plan.chargingStops().isEmpty()) {
            return BigDecimal.valueOf(100);
        }

        double driveMinutes    = route.durationMinutes();
        double chargingMinutes = plan.totalChargingMinutes().doubleValue();

        // Convert detour kilometres to minutes using the route's average speed.
        double distanceKm   = route.distanceKm().doubleValue();
        double avgSpeedKmh  = driveMinutes > 0 ? distanceKm / (driveMinutes / 60.0) : 60.0;
        double detourMinutes = avgSpeedKmh > 0
                ? plan.totalDetourKm().doubleValue() / avgSpeedKmh * 60.0
                : 0.0;

        double totalMinutes = driveMinutes + chargingMinutes + detourMinutes;
        double ratio = totalMinutes > 0 ? driveMinutes / totalMinutes : 1.0;

        return BigDecimal.valueOf(Math.min(100.0, ratio * 100.0));
    }

    // -------------------------------------------------------------------------
    // Dimension: arrival SOC buffer
    // -------------------------------------------------------------------------

    /**
     * How much above the requested minimum SOC the vehicle is projected to
     * arrive at the destination.  A generous buffer means the journey can
     * absorb unexpected range losses without stress.
     */
    private BigDecimal scoreArrivalBuffer(
            BigDecimal plannedArrivalSoc,
            BigDecimal minimumArrivalSoc) {

        if (plannedArrivalSoc == null) {
            return BigDecimal.ZERO;
        }

        double arrival = plannedArrivalSoc.doubleValue();
        double minimum = minimumArrivalSoc.doubleValue();
        double usableRange = 100.0 - minimum;

        if (usableRange <= 0) {
            return BigDecimal.valueOf(100);
        }

        double buffer = arrival - minimum;
        double score  = Math.max(0.0, Math.min(1.0, buffer / usableRange)) * 100.0;
        return BigDecimal.valueOf(score);
    }

    // -------------------------------------------------------------------------
    // Dimension: charging experience
    // -------------------------------------------------------------------------

    /**
     * Combines the number of stops required (fewer is better) with the
     * average charging power available (faster chargers are better).
     *
     * <p>Stop-count penalty: 60 % weight.
     * <p>Charger speed bonus:  40 % weight.
     */
    private BigDecimal scoreChargingExperience(MultiStopChargingPlan plan) {

        if (plan == null || plan.chargingStops().isEmpty()) {
            return BigDecimal.valueOf(100);
        }

        List<ChargingStopPlan> stops = plan.chargingStops();
        int numStops = stops.size();

        double stopScore = 1.0 - (double) numStops / MAX_STOPS;

        double avgPowerKw = stops.stream()
                .filter(s -> s.chargingPowerKw() != null)
                .mapToDouble(s -> s.chargingPowerKw().doubleValue())
                .average()
                .orElse(0.0);

        double powerScore = Math.min(1.0, avgPowerKw / REFERENCE_CHARGER_KW);

        double score = (0.6 * stopScore + 0.4 * powerScore) * 100.0;
        return BigDecimal.valueOf(Math.max(0.0, score));
    }

    // -------------------------------------------------------------------------
    // Dimension: safety
    // -------------------------------------------------------------------------

    /**
     * Minimum SOC reached at any point in the journey — including arrival at
     * each charging station and arrival at the destination.  Arriving nearly
     * empty is risky (the station might be broken, the route might be longer
     * than expected).
     */
    private BigDecimal scoreSafety(
            MultiStopChargingPlan plan,
            BigDecimal plannedArrivalSoc) {

        double minSoc;

        if (plan == null || plan.chargingStops().isEmpty()) {
            minSoc = plannedArrivalSoc != null ? plannedArrivalSoc.doubleValue() : 0.0;
        } else {
            double minStopArrival = plan.chargingStops().stream()
                    .filter(s -> s.arrivalSocPercent() != null)
                    .mapToDouble(s -> s.arrivalSocPercent().doubleValue())
                    .min()
                    .orElse(0.0);

            minSoc = plannedArrivalSoc != null
                    ? Math.min(minStopArrival, plannedArrivalSoc.doubleValue())
                    : minStopArrival;
        }

        double score = Math.max(0.0,
                Math.min(1.0, (minSoc - SAFETY_FLOOR_PERCENT) / (100.0 - SAFETY_FLOOR_PERCENT)))
                * 100.0;

        return BigDecimal.valueOf(score);
    }

    // -------------------------------------------------------------------------

    private BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
