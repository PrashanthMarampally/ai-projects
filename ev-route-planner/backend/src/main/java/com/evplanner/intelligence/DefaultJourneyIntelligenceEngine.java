package com.evplanner.intelligence;

import com.evplanner.charging.ChargingDecision;
import com.evplanner.charging.ChargingStrategy;
import com.evplanner.energy.EnergyCalculationResult;
import com.evplanner.energy.EnergyConsumptionModel;
import com.evplanner.journey.ChargingRecommendation;
import com.evplanner.journey.JourneyOption;
import com.evplanner.journey.JourneyPlan;
import com.evplanner.journey.JourneyRequest;
import com.evplanner.routing.RouteResult;
import com.evplanner.routing.RoutingProvider;
import com.evplanner.vehicle.Vehicle;
import com.evplanner.vehicle.VehicleRepository;
import com.evplanner.station.RouteStationFinder;
import com.evplanner.routing.RouteStationPosition;
import org.springframework.stereotype.Service;
import com.evplanner.station.ChargingStation;
import com.evplanner.charging.ChargingCandidateEvaluator;
import com.evplanner.charging.ChargingStopCandidate;
import java.util.List;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DefaultJourneyIntelligenceEngine
        implements JourneyIntelligenceEngine {

    private final RoutingProvider routingProvider;
    private final VehicleRepository vehicleRepository;
    private final EnergyConsumptionModel energyConsumptionModel;
    private final ChargingStrategy chargingStrategy;
    private final RouteStationFinder routeStationFinder;
    private final ChargingCandidateEvaluator chargingCandidateEvaluator;

    public DefaultJourneyIntelligenceEngine(
            RoutingProvider routingProvider,
            VehicleRepository vehicleRepository,
            EnergyConsumptionModel energyConsumptionModel,
            ChargingStrategy chargingStrategy,
            RouteStationFinder routeStationFinder,
            ChargingCandidateEvaluator chargingCandidateEvaluator) {

        this.routingProvider = routingProvider;
        this.vehicleRepository = vehicleRepository;
        this.energyConsumptionModel = energyConsumptionModel;
        this.chargingStrategy = chargingStrategy;
        this.routeStationFinder = routeStationFinder;
        this.chargingCandidateEvaluator = chargingCandidateEvaluator;
    }

    @Override
    public JourneyPlan plan(JourneyRequest request) {

        validate(request);

        Vehicle vehicle =
                vehicleRepository.findById(request.vehicleId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Vehicle not found: "
                                                + request.vehicleId()
                                ));

        RouteResult route =
                routingProvider.calculateRoute(
                        request.origin(),
                        request.destination()
                );

        List<ChargingStation> stations =
                routeStationFinder.findNearRoute(
                        route,
                        10.0
                );

        List<ChargingStopCandidate> chargingCandidates =
                chargingCandidateEvaluator.evaluate(
                        vehicle,
                        request.currentSocPercent(),
                        request.minimumArrivalSocPercent(),
                        route,
                        stations
                );

        EnergyCalculationResult energy =
                energyConsumptionModel.calculate(
                        vehicle,
                        route.distanceKm(),
                        request.currentSocPercent(),
                        request.minimumArrivalSocPercent()
                );

        ChargingDecision chargingDecision =
                chargingStrategy.evaluate(
                        energy,
                        request.currentSocPercent(),
                        request.minimumArrivalSocPercent()
                );

        JourneyOption option =
                buildJourneyOption(
                        vehicle,
                        route,
                        energy,
                        chargingDecision,
                        chargingCandidates
                );

        return new JourneyPlan(
                List.of(option),
                option
        );
    }

    private JourneyOption buildJourneyOption(
            Vehicle vehicle,
            RouteResult route,
            EnergyCalculationResult energy,
            ChargingDecision chargingDecision,
            List<ChargingStopCandidate> chargingCandidates) {

        List<ChargingRecommendation> recommendations =
                chargingCandidates.stream()
                        .map(candidate ->
                                toChargingRecommendation(
                                        vehicle,
                                        candidate))
                        .toList();

        BigDecimal score =
                calculateInitialScore(
                        route,
                        energy,
                        chargingDecision
                );

        return new JourneyOption(
                route.distanceKm(),
                route.durationMinutes(),
                recommendations,
                energy.arrivalSocPercent(),
                score
        );
    }

    private BigDecimal calculateInitialScore(
            RouteResult route,
            EnergyCalculationResult energy,
            ChargingDecision chargingDecision) {

        /*
         * Initial baseline score.
         *
         * This is intentionally simple. The proper score will
         * later combine:
         *
         * - driving time
         * - charging time
         * - detour
         * - availability/reliability
         * - reserve margin
         * - user preferences
         */
        if (chargingDecision.chargingRequired()) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(100)
                .subtract(
                        route.distanceKm()
                                .divide(
                                        BigDecimal.TEN,
                                        2,
                                        java.math.RoundingMode.HALF_UP
                                )
                )
                .max(BigDecimal.ZERO);
    }

    private void validate(JourneyRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Journey request must not be null");
        }

        if (request.origin() == null) {
            throw new IllegalArgumentException(
                    "Origin must not be null");
        }

        if (request.destination() == null) {
            throw new IllegalArgumentException(
                    "Destination must not be null");
        }

        if (request.vehicleId() == null) {
            throw new IllegalArgumentException(
                    "Vehicle ID must not be null");
        }

        if (request.currentSocPercent() == null ||
                request.currentSocPercent().compareTo(BigDecimal.ZERO) < 0 ||
                request.currentSocPercent().compareTo(BigDecimal.valueOf(100)) > 0) {

            throw new IllegalArgumentException(
                    "Current SOC must be between 0 and 100");
        }

        if (request.minimumArrivalSocPercent() == null ||
                request.minimumArrivalSocPercent().compareTo(BigDecimal.ZERO) < 0 ||
                request.minimumArrivalSocPercent().compareTo(BigDecimal.valueOf(100)) > 0) {

            throw new IllegalArgumentException(
                    "Minimum arrival SOC must be between 0 and 100");
        }
    }
    private ChargingRecommendation toChargingRecommendation(
            Vehicle vehicle,
            ChargingStopCandidate candidate) {

        BigDecimal batteryCapacityKwh =
                vehicle.getUsableBatteryCapacityKwh();

        if (batteryCapacityKwh == null) {
            batteryCapacityKwh =
                    vehicle.getBatteryCapacityKwh();
        }

        BigDecimal energyRequiredKwh =
                batteryCapacityKwh
                        .multiply(candidate.requiredChargePercent())
                        .divide(
                                BigDecimal.valueOf(100),
                                2,
                                java.math.RoundingMode.HALF_UP
                        );

        BigDecimal chargingHours =
                energyRequiredKwh.divide(
                        candidate.chargingPowerKw(),
                        4,
                        java.math.RoundingMode.HALF_UP
                );

        int chargingMinutes =
                chargingHours
                        .multiply(BigDecimal.valueOf(60))
                        .setScale(
                                0,
                                java.math.RoundingMode.CEILING
                        )
                        .intValue();

        BigDecimal targetSoc =
                candidate.arrivalSocPercent()
                        .add(candidate.requiredChargePercent())
                        .min(BigDecimal.valueOf(100));

        return new ChargingRecommendation(
                candidate.station().getId(),
                candidate.arrivalSocPercent(),
                targetSoc,
                chargingMinutes,
                BigDecimal.valueOf(100)
        );
    }
}