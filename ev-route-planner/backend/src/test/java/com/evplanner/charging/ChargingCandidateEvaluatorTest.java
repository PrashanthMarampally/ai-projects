package com.evplanner.charging;

import com.evplanner.energy.BasicEnergyConsumptionModel;
import com.evplanner.journey.GeoPoint;
import com.evplanner.routing.RouteResult;
import com.evplanner.routing.RouteStationPositionCalculator;
import com.evplanner.station.ChargingConnector;
import com.evplanner.station.ChargingStation;
import com.evplanner.vehicle.Vehicle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChargingCandidateEvaluatorTest {

    private final ChargingCandidateEvaluator evaluator =
            new ChargingCandidateEvaluator(
                    new BasicEnergyConsumptionModel(),
                    new ChargingStationEligibilityService(),
                    new RouteStationPositionCalculator());

    private final Vehicle vehicle = new Vehicle(
            "Test EV",
            BigDecimal.valueOf(60),
            BigDecimal.valueOf(60),
            BigDecimal.valueOf(400),
            BigDecimal.valueOf(15),
            "CCS2");

    @Test
    void shouldCalculateChargeNeededToFinishJourney() {
        RouteResult route = route(300, 30);
        ChargingStation station = station("S1", 17.0, 78.0666667, 60);

        List<ChargingStopCandidate> candidates = evaluator.evaluate(
                vehicle,
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(15),
                route,
                List.of(station));

        assertEquals(1, candidates.size());
        ChargingStopCandidate candidate = candidates.get(0);

        // Station is about one third of the route: arrival SOC is 25%.
        assertEquals(0, candidate.arrivalSocPercent()
                .compareTo(BigDecimal.valueOf(25.00)));

        // The remaining 200 km needs 50% SOC, plus 15% destination reserve.
        // Required charge is therefore 65% - 25% = 40%.
        assertEquals(0, candidate.requiredChargePercent()
                .compareTo(BigDecimal.valueOf(40.00)));

        assertEquals(0, candidate.chargingPowerKw()
                .compareTo(BigDecimal.valueOf(60)));
    }

    @Test
    void shouldRejectStationWhenDestinationCannotBeReachedEvenAtFullCharge() {
        RouteResult route = route(500, 30);
        ChargingStation station = station("S1", 17.0, 78.10, 60);

        List<ChargingStopCandidate> candidates = evaluator.evaluate(
                vehicle,
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(15),
                route,
                List.of(station));

        assertTrue(candidates.isEmpty());
    }

    @Test
    void shouldIgnoreIncompatibleStation() {
        RouteResult route = route(300, 30);
        ChargingStation station = station("S1", 17.0, 78.10, 60);
        station.addConnector(new ChargingConnector(
                station, "TYPE2", BigDecimal.valueOf(60), 2, 2));

        // Replace the CCS2 connector created by station() with only Type 2.
        station.getConnectors().remove(0);

        List<ChargingStopCandidate> candidates = evaluator.evaluate(
                vehicle,
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(15),
                route,
                List.of(station));

        assertTrue(candidates.isEmpty());
    }

    @Test
    void shouldRankByEstimatedChargingTimeThenDetour() {
        RouteResult route = route(300, 30);

        ChargingStation earlier = station("EARLY", 17.0, 78.05, 30);
        ChargingStation later = station("LATE", 17.0, 78.12, 60);

        List<ChargingStopCandidate> candidates = evaluator.evaluate(
                vehicle,
                BigDecimal.valueOf(60),
                BigDecimal.valueOf(15),
                route,
                List.of(earlier, later));

        assertEquals(2, candidates.size());
        assertEquals("LATE", candidates.get(0).station().getExternalId());
        assertEquals("EARLY", candidates.get(1).station().getExternalId());
    }

    private RouteResult route(double distanceKm, int durationMinutes) {
        return new RouteResult(
                BigDecimal.valueOf(distanceKm),
                durationMinutes,
                List.of(
                        point(17.0, 78.0),
                        point(17.0, 78.10),
                        point(17.0, 78.20)
                ));
    }

    private ChargingStation station(
            String externalId,
            double latitude,
            double longitude,
            double powerKw) {

        ChargingStation station = new ChargingStation(
                externalId,
                externalId,
                BigDecimal.valueOf(latitude),
                BigDecimal.valueOf(longitude),
                null);

        station.addConnector(new ChargingConnector(
                station,
                "CCS2",
                BigDecimal.valueOf(powerKw),
                2,
                2));

        return station;
    }

    private GeoPoint point(double latitude, double longitude) {
        return new GeoPoint(
                BigDecimal.valueOf(latitude),
                BigDecimal.valueOf(longitude));
    }
}
