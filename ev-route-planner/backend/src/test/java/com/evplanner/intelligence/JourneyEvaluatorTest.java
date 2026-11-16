package com.evplanner.intelligence;

import com.evplanner.charging.MultiStopChargingPlan;
import com.evplanner.journey.ChargingStopPlan;
import com.evplanner.journey.GeoPoint;
import com.evplanner.journey.JourneyRequest;
import com.evplanner.routing.RouteResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JourneyEvaluatorTest {

    private JourneyEvaluator evaluator;

    private static final GeoPoint ORIGIN = new GeoPoint(
            BigDecimal.valueOf(17.3850), BigDecimal.valueOf(78.4867));
    private static final GeoPoint DESTINATION = new GeoPoint(
            BigDecimal.valueOf(12.9716), BigDecimal.valueOf(77.5946));

    private JourneyRequest requestWithMinSoc(int minSocPercent) {
        return new JourneyRequest(
                ORIGIN,
                DESTINATION,
                1L,
                BigDecimal.valueOf(80),
                BigDecimal.valueOf(minSocPercent));
    }

    @BeforeEach
    void setUp() {
        evaluator = new JourneyEvaluator();
    }

    // -------------------------------------------------------------------------
    // No-charging scenario
    // -------------------------------------------------------------------------

    @Test
    void noChargingRouteScoresHigherTimeEfficiencyThanChargingRoute() {
        // No-charging route: all drive time, no overhead
        RouteResult shortRoute = new RouteResult(
                BigDecimal.valueOf(200), 180, List.of());
        JourneyRequest request = requestWithMinSoc(15);

        JourneyScore noCharge = evaluator.evaluate(shortRoute, request, null,
                BigDecimal.valueOf(55));

        // Charging route with the same drive time + 45 min charging
        ChargingStopPlan stop = makeStop(1L, "Stop", 150);
        MultiStopChargingPlan plan = new MultiStopChargingPlan(
                List.of(stop), BigDecimal.valueOf(45), BigDecimal.ZERO);
        RouteResult sameRoute = new RouteResult(
                BigDecimal.valueOf(200), 180, List.of());

        JourneyScore withCharge = evaluator.evaluate(sameRoute, request, plan,
                BigDecimal.valueOf(30));

        assertTrue(
                noCharge.timeEfficiency().compareTo(withCharge.timeEfficiency()) > 0,
                "No charging → better time efficiency than same route with charging");
    }

    @Test
    void shorterTripScoresHigherTimeEfficiencyThanLongerTrip() {
        JourneyRequest request = requestWithMinSoc(15);

        RouteResult fastRoute = new RouteResult(
                BigDecimal.valueOf(200), 180, List.of());
        RouteResult slowRoute = new RouteResult(
                BigDecimal.valueOf(300), 360, List.of());

        JourneyScore fast = evaluator.evaluate(fastRoute, request, null,
                BigDecimal.valueOf(60));
        JourneyScore slow = evaluator.evaluate(slowRoute, request, null,
                BigDecimal.valueOf(40));

        assertTrue(
                fast.timeEfficiency().compareTo(slow.timeEfficiency()) > 0,
                "Shorter/faster trip → better time efficiency");
    }

    @Test
    void noChargingRouteScoresFullChargingExperience() {
        RouteResult route = new RouteResult(
                BigDecimal.valueOf(200), 180, List.of());
        JourneyRequest request = requestWithMinSoc(15);

        JourneyScore score = evaluator.evaluate(route, request, null,
                BigDecimal.valueOf(55));

        assertEquals(0, BigDecimal.valueOf(100).compareTo(score.chargingExperience()),
                "No charging → charging experience must be 100");
    }

    @Test
    void overallScoreIsWeightedSum() {
        RouteResult route = new RouteResult(
                BigDecimal.valueOf(200), 180, List.of());
        JourneyRequest request = requestWithMinSoc(15);
        BigDecimal arrivalSoc = BigDecimal.valueOf(55);

        JourneyScore score = evaluator.evaluate(route, request, null, arrivalSoc);

        double expected =
                JourneyScore.WEIGHT_TIME       * score.timeEfficiency().doubleValue()
                + JourneyScore.WEIGHT_BUFFER     * score.arrivalBuffer().doubleValue()
                + JourneyScore.WEIGHT_EXPERIENCE * score.chargingExperience().doubleValue()
                + JourneyScore.WEIGHT_SAFETY     * score.safety().doubleValue();

        assertEquals(
                score.overall().doubleValue(),
                expected,
                0.01,
                "overall must equal weighted sum of components");
    }

    // -------------------------------------------------------------------------
    // Arrival buffer
    // -------------------------------------------------------------------------

    @Test
    void arrivalBufferIsZeroWhenAtMinimum() {
        RouteResult route = new RouteResult(
                BigDecimal.valueOf(200), 180, List.of());
        JourneyRequest request = requestWithMinSoc(20);

        JourneyScore score = evaluator.evaluate(route, request, null,
                BigDecimal.valueOf(20));  // exactly at minimum

        assertEquals(0,
                BigDecimal.ZERO.compareTo(score.arrivalBuffer()),
                "arriving exactly at minimum → buffer score must be 0");
    }

    @Test
    void arrivalBufferIsMaxWhenAtFull() {
        RouteResult route = new RouteResult(
                BigDecimal.valueOf(100), 60, List.of());
        JourneyRequest request = requestWithMinSoc(10);

        JourneyScore score = evaluator.evaluate(route, request, null,
                BigDecimal.valueOf(100));  // arriving at 100%

        assertEquals(0,
                BigDecimal.valueOf(100).compareTo(score.arrivalBuffer()),
                "arriving at 100% SOC → buffer score must be 100");
    }

    // -------------------------------------------------------------------------
    // Safety
    // -------------------------------------------------------------------------

    @Test
    void safetyDropsWhenArrivalSocIsLow() {
        RouteResult route = new RouteResult(
                BigDecimal.valueOf(200), 180, List.of());
        JourneyRequest request = requestWithMinSoc(5);

        JourneyScore highSoc  = evaluator.evaluate(route, request, null, BigDecimal.valueOf(60));
        JourneyScore lowSoc   = evaluator.evaluate(route, request, null, BigDecimal.valueOf(8));

        assertTrue(
                highSoc.safety().compareTo(lowSoc.safety()) > 0,
                "higher arrival SOC → higher safety score");
    }

    @Test
    void safetyIsZeroAtOrBelowFloor() {
        RouteResult route = new RouteResult(
                BigDecimal.valueOf(200), 180, List.of());
        JourneyRequest request = requestWithMinSoc(5);

        JourneyScore score = evaluator.evaluate(route, request, null,
                BigDecimal.valueOf(5));  // exactly at SAFETY_FLOOR_PERCENT

        assertEquals(0,
                BigDecimal.ZERO.compareTo(score.safety()),
                "SOC at floor → safety score must be 0");
    }

    // -------------------------------------------------------------------------
    // Time efficiency with charging
    // -------------------------------------------------------------------------

    @Test
    void timeEfficiencyDecreasesWithChargingTime() {
        RouteResult route = new RouteResult(
                BigDecimal.valueOf(300), 240, List.of());
        JourneyRequest request = requestWithMinSoc(15);

        ChargingStopPlan stop = new ChargingStopPlan(
                1L, "Test Station",
                BigDecimal.valueOf(150), BigDecimal.valueOf(150),
                BigDecimal.valueOf(15), BigDecimal.valueOf(70),
                BigDecimal.valueOf(25), BigDecimal.valueOf(100),
                45, null, null);

        MultiStopChargingPlan plan = new MultiStopChargingPlan(
                List.of(stop),
                BigDecimal.valueOf(45),   // 45 min charging
                BigDecimal.valueOf(5));    // 5 km detour

        JourneyScore withCharging = evaluator.evaluate(route, request, plan,
                BigDecimal.valueOf(30));

        assertTrue(
                withCharging.timeEfficiency().compareTo(BigDecimal.valueOf(100)) < 0,
                "charging time must reduce time efficiency below 100");
    }

    @Test
    void chargingExperienceIsLowerWithMoreStops() {
        RouteResult route = new RouteResult(
                BigDecimal.valueOf(600), 360, List.of());
        JourneyRequest request = requestWithMinSoc(15);

        ChargingStopPlan stop1 = makeStop(1L, "Stop A", 50);
        ChargingStopPlan stop2 = makeStop(2L, "Stop B", 50);

        MultiStopChargingPlan oneStop = new MultiStopChargingPlan(
                List.of(stop1), BigDecimal.valueOf(30), BigDecimal.ZERO);
        MultiStopChargingPlan twoStops = new MultiStopChargingPlan(
                List.of(stop1, stop2), BigDecimal.valueOf(60), BigDecimal.ZERO);

        JourneyScore s1 = evaluator.evaluate(route, request, oneStop, BigDecimal.valueOf(25));
        JourneyScore s2 = evaluator.evaluate(route, request, twoStops, BigDecimal.valueOf(25));

        assertTrue(
                s1.chargingExperience().compareTo(s2.chargingExperience()) > 0,
                "fewer stops → better charging experience");
    }

    @Test
    void chargingExperienceIsBetterWithFasterChargers() {
        RouteResult route = new RouteResult(
                BigDecimal.valueOf(300), 180, List.of());
        JourneyRequest request = requestWithMinSoc(15);

        ChargingStopPlan slowStop  = makeStopWithPower(1L, "Slow",  22);   // AC
        ChargingStopPlan fastStop  = makeStopWithPower(2L, "Fast", 150);   // DC

        MultiStopChargingPlan slowPlan = new MultiStopChargingPlan(
                List.of(slowStop), BigDecimal.valueOf(60), BigDecimal.ZERO);
        MultiStopChargingPlan fastPlan = new MultiStopChargingPlan(
                List.of(fastStop), BigDecimal.valueOf(15), BigDecimal.ZERO);

        JourneyScore sSlow = evaluator.evaluate(route, request, slowPlan, BigDecimal.valueOf(30));
        JourneyScore sFast = evaluator.evaluate(route, request, fastPlan, BigDecimal.valueOf(30));

        assertTrue(
                sFast.chargingExperience().compareTo(sSlow.chargingExperience()) > 0,
                "faster charger → better charging experience");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ChargingStopPlan makeStop(Long id, String name, int powerKw) {
        return makeStopWithPower(id, name, powerKw);
    }

    private ChargingStopPlan makeStopWithPower(Long id, String name, int powerKw) {
        return new ChargingStopPlan(
                id, name,
                BigDecimal.valueOf(150), BigDecimal.valueOf(150),
                BigDecimal.valueOf(15), BigDecimal.valueOf(70),
                BigDecimal.valueOf(20), BigDecimal.valueOf(powerKw),
                30, null, null);
    }
}
