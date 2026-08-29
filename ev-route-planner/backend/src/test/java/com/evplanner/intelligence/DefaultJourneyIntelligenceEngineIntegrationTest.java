package com.evplanner.intelligence;

import com.evplanner.charging.BasicChargingStrategy;
import com.evplanner.energy.BasicEnergyConsumptionModel;
import com.evplanner.journey.GeoPoint;
import com.evplanner.journey.JourneyPlan;
import com.evplanner.journey.JourneyRequest;
import com.evplanner.routing.RouteResult;
import com.evplanner.routing.RoutingProvider;
import com.evplanner.vehicle.Vehicle;
import com.evplanner.vehicle.VehicleRepository;
import com.evplanner.station.RouteStationFinder;
import com.evplanner.charging.ChargingCandidateEvaluator;
import com.evplanner.charging.ChargingStationEligibilityService;
import com.evplanner.routing.RouteStationPositionCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import com.evplanner.journey.GeoPoint;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefaultJourneyIntelligenceEngineIntegrationTest {

    @Test
    void shouldPlanJourneyWithoutChargingWhenBatteryIsSufficient() {

        RoutingProvider routingProvider =
                mock(RoutingProvider.class);

        VehicleRepository vehicleRepository =
                mock(VehicleRepository.class);

        Vehicle vehicle =
                new Vehicle(
                        "Test EV",
                        BigDecimal.valueOf(60),
                        BigDecimal.valueOf(55),
                        BigDecimal.valueOf(400),
                        BigDecimal.valueOf(15),
                        "CCS2"
                );

        when(vehicleRepository.findById(1L))
                .thenReturn(Optional.of(vehicle));

//        when(routingProvider.calculateRoute(
//                any(),
//                any()))
//                .thenReturn(
//                        new RouteResult(
//                                BigDecimal.valueOf(200),
//                                150
//                        )
//                );
        GeoPoint origin =
                new GeoPoint(
                        BigDecimal.valueOf(17.3850),
                        BigDecimal.valueOf(78.4867));

        GeoPoint destination =
                new GeoPoint(
                        BigDecimal.valueOf(12.9716),
                        BigDecimal.valueOf(77.5946));

        GeoPoint midpoint =
                new GeoPoint(
                        BigDecimal.valueOf(15.1783),
                        BigDecimal.valueOf(78.04065));

        when(routingProvider.calculateRoute(
                any(),
                any()))
                .thenReturn(
                        new RouteResult(
                                BigDecimal.valueOf(200),
                                150,
                                List.of(
                                        origin,
                                        midpoint,
                                        destination
                                )
                        ));
        RouteStationFinder routeStationFinder =
                mock(RouteStationFinder.class);

        when(routeStationFinder.findNearRoute(any(), anyDouble()))
                .thenReturn(List.of());

        BasicEnergyConsumptionModel energyModel =
                new BasicEnergyConsumptionModel();

        ChargingCandidateEvaluator chargingCandidateEvaluator =
                new ChargingCandidateEvaluator(
                        energyModel,
                        new ChargingStationEligibilityService(),
                        new RouteStationPositionCalculator()
                );

        var engine =
                new DefaultJourneyIntelligenceEngine(
                        routingProvider,
                        vehicleRepository,
                        energyModel,
                        new BasicChargingStrategy(),
                        routeStationFinder,
                        chargingCandidateEvaluator
                );

        JourneyRequest request =
                new JourneyRequest(
                        new GeoPoint(
                                BigDecimal.valueOf(17.3850),
                                BigDecimal.valueOf(78.4867)
                        ),
                        new GeoPoint(
                                BigDecimal.valueOf(12.9716),
                                BigDecimal.valueOf(77.5946)
                        ),
                        1L,
                        BigDecimal.valueOf(80),
                        BigDecimal.valueOf(15)
                );

        JourneyPlan plan =
                engine.plan(request);

        assertNotNull(plan);
        assertNotNull(plan.recommendedOption());

        assertEquals(
                0,
                plan.recommendedOption()
                        .chargingRecommendations()
                        .size()
        );

        assertTrue(
                plan.recommendedOption()
                        .minimumSocPercent()
                        .compareTo(BigDecimal.valueOf(15)) > 0
        );

        verify(routingProvider)
                .calculateRoute(
                        request.origin(),
                        request.destination()
                );

        verify(vehicleRepository)
                .findById(1L);
    }
}