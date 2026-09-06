package com.evplanner.intelligence;

import com.evplanner.energy.EnergyCalculationResult;
import com.evplanner.energy.EnergyConsumptionModel;
import com.evplanner.journey.GeoPoint;
import com.evplanner.journey.JourneyPlan;
import com.evplanner.journey.JourneyRequest;
import com.evplanner.routing.RouteResult;
import com.evplanner.routing.RoutingProvider;
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
import static org.mockito.ArgumentMatchers.anyDouble;

class DefaultJourneyIntelligenceEngineTest {

    @Test
    void shouldCreateJourneyPlanUsingVehicleAndEnergyModel() {

        VehicleRepository vehicleRepository =
                mock(VehicleRepository.class);

        EnergyConsumptionModel energyModel =
                new com.evplanner.energy.BasicEnergyConsumptionModel();

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

        RoutingProvider routingProvider = mock(RoutingProvider.class);
        RouteResult fastRoute = new RouteResult(
                BigDecimal.valueOf(275),
                330,
                List.of(request.origin(),
                        new GeoPoint(BigDecimal.valueOf(15.1783), BigDecimal.valueOf(78.04065)),
                        request.destination()));
        RouteResult shorterRoute = new RouteResult(
                BigDecimal.valueOf(250),
                340,
                List.of(request.origin(),
                        new GeoPoint(BigDecimal.valueOf(15.1783), BigDecimal.valueOf(78.03065)),
                        request.destination()));
        when(routingProvider.calculateRoutes(any(), any()))
                .thenReturn(List.of(fastRoute, shorterRoute));

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
                        routingProvider,
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
    @Test
    void shouldEvaluateMultipleRoutesAndRecommendHighestScoringRoute() {

        VehicleRepository vehicleRepository = mock(VehicleRepository.class);
        Vehicle vehicle = new Vehicle(
                "Test EV",
                BigDecimal.valueOf(60),
                BigDecimal.valueOf(57),
                BigDecimal.valueOf(400),
                BigDecimal.valueOf(14.25),
                "CCS2"
        );
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));

        JourneyRequest request = new JourneyRequest(
                new GeoPoint(BigDecimal.valueOf(17.3850), BigDecimal.valueOf(78.4867)),
                new GeoPoint(BigDecimal.valueOf(16.5062), BigDecimal.valueOf(80.6480)),
                1L,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(15)
        );

        RouteResult slowerRoute = new RouteResult(
                BigDecimal.valueOf(280),
                360,
                List.of(request.origin(), request.destination()));
        RouteResult fasterRoute = new RouteResult(
                BigDecimal.valueOf(300),
                300,
                List.of(request.origin(), request.destination()));

        RoutingProvider routingProvider = mock(RoutingProvider.class);
        when(routingProvider.calculateRoutes(request.origin(), request.destination()))
                .thenReturn(List.of(slowerRoute, fasterRoute));

        RouteStationFinder routeStationFinder = mock(RouteStationFinder.class);
        when(routeStationFinder.findNearRoute(any(), anyDouble())).thenReturn(List.of());

        EnergyConsumptionModel energyModel = new com.evplanner.energy.BasicEnergyConsumptionModel();
        ChargingCandidateEvaluator chargingCandidateEvaluator =
                new ChargingCandidateEvaluator(
                        energyModel,
                        new ChargingStationEligibilityService(),
                        new RouteStationPositionCalculator());

        JourneyIntelligenceEngine engine = new DefaultJourneyIntelligenceEngine(
                routingProvider,
                vehicleRepository,
                energyModel,
                new BasicChargingStrategy(),
                routeStationFinder,
                chargingCandidateEvaluator);

        JourneyPlan plan = engine.plan(request);

        assertEquals(2, plan.options().size());
        assertEquals(300, plan.recommendedOption().distanceKm().intValue());
        assertEquals(300, plan.recommendedOption().estimatedDurationMinutes());
        assertEquals(plan.options().get(0), plan.recommendedOption());
    }

}
