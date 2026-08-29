package com.evplanner.intelligence;

import com.evplanner.energy.EnergyCalculationResult;
import com.evplanner.energy.EnergyConsumptionModel;
import com.evplanner.journey.GeoPoint;
import com.evplanner.journey.JourneyPlan;
import com.evplanner.journey.JourneyRequest;
import com.evplanner.routing.FakeRoutingProvider;
import com.evplanner.vehicle.Vehicle;
import com.evplanner.vehicle.VehicleRepository;
import com.evplanner.charging.BasicChargingStrategy;
import com.evplanner.charging.ChargingStrategy;
import com.evplanner.station.RouteStationFinder;
import com.evplanner.charging.ChargingCandidateEvaluator;
import com.evplanner.charging.ChargingStationEligibilityService;
import com.evplanner.routing.RouteStationPositionCalculator;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;

class DefaultJourneyIntelligenceEngineTest {

    @Test
    void shouldCreateJourneyPlanUsingVehicleAndEnergyModel() {

        VehicleRepository vehicleRepository =
                mock(VehicleRepository.class);

        EnergyConsumptionModel energyModel =
                mock(EnergyConsumptionModel.class);

        Vehicle vehicle = new Vehicle(
                "Test EV",
                BigDecimal.valueOf(60),
                BigDecimal.valueOf(57),
                BigDecimal.valueOf(400),
                BigDecimal.valueOf(14.25),
                "CCS2"
        );

        when(vehicleRepository.findById(1L))
                .thenReturn(Optional.of(vehicle));

        when(energyModel.calculate(
                vehicle,
                BigDecimal.valueOf(275),
                BigDecimal.valueOf(85),
                BigDecimal.valueOf(15)
        )).thenReturn(
                new EnergyCalculationResult(
                        BigDecimal.valueOf(39.1875),
                        BigDecimal.valueOf(16.25),
                        true
                )
        );

        JourneyRequest request = new JourneyRequest(
                new GeoPoint(
                        BigDecimal.valueOf(17.3850),
                        BigDecimal.valueOf(78.4867)
                ),
                new GeoPoint(
                        BigDecimal.valueOf(16.5062),
                        BigDecimal.valueOf(80.6480)
                ),
                1L,
                BigDecimal.valueOf(85),
                BigDecimal.valueOf(15)
        );

        RouteStationFinder routeStationFinder =
                mock(RouteStationFinder.class);

        when(routeStationFinder.findNearRoute(any(), anyDouble()))
                .thenReturn(List.of());

        ChargingCandidateEvaluator chargingCandidateEvaluator =
                new ChargingCandidateEvaluator(
                        energyModel,
                        new ChargingStationEligibilityService(),
                        new RouteStationPositionCalculator()
                );

        JourneyIntelligenceEngine engine =
                new DefaultJourneyIntelligenceEngine(
                        new FakeRoutingProvider(),
                        vehicleRepository,
                        energyModel,
                        new BasicChargingStrategy(),
                        routeStationFinder,
                        chargingCandidateEvaluator
                );

        JourneyPlan plan = engine.plan(request);

        assertNotNull(plan);
        assertNotNull(plan.recommendedOption());

        assertEquals(
                0,
                plan.recommendedOption()
                        .distanceKm()
                        .compareTo(BigDecimal.valueOf(275))
        );

        assertEquals(
                330,
                plan.recommendedOption()
                        .estimatedDurationMinutes()
        );

        assertEquals(
                0,
                plan.recommendedOption()
                        .minimumSocPercent()
                        .compareTo(BigDecimal.valueOf(16.25))
        );
    }
}