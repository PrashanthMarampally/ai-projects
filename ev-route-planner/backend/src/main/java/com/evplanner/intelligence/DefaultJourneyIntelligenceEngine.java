package com.evplanner.intelligence;

import com.evplanner.charging.ChargingCandidateEvaluator;
import com.evplanner.charging.ChargingDecision;
import com.evplanner.charging.ChargingStrategy;
import com.evplanner.charging.ChargingStopCandidate;
import com.evplanner.charging.MultiStopChargingPlan;
import com.evplanner.charging.MultiStopChargingPlanner;
import com.evplanner.energy.EnergyCalculationResult;
import com.evplanner.energy.EnergyConsumptionModel;
import com.evplanner.journey.ChargingStopPlan;
import com.evplanner.journey.JourneyOption;
import com.evplanner.journey.JourneyPlan;
import com.evplanner.journey.JourneyRequest;
import com.evplanner.routing.RouteResult;
import com.evplanner.routing.RoutingProvider;
import com.evplanner.station.ChargingStation;
import com.evplanner.station.RouteStationFinder;
import com.evplanner.vehicle.Vehicle;
import com.evplanner.vehicle.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
public class DefaultJourneyIntelligenceEngine
        implements JourneyIntelligenceEngine {

    private static final double DEFAULT_STATION_CORRIDOR_RADIUS_KM = 50.0;

    private final double stationCorridorRadiusKm;
    private final RoutingProvider routingProvider;
    private final VehicleRepository vehicleRepository;
    private final EnergyConsumptionModel energyConsumptionModel;
    private final ChargingStrategy chargingStrategy;
    private final RouteStationFinder routeStationFinder;
    private final MultiStopChargingPlanner multiStopChargingPlanner;

    /**
     * Backward-compatible constructor primarily useful for unit tests.
     *
     * The production Spring constructor below injects the configurable
     * station corridor radius.
     */
    public DefaultJourneyIntelligenceEngine(
            RoutingProvider routingProvider,
            VehicleRepository vehicleRepository,
            EnergyConsumptionModel energyConsumptionModel,
            ChargingStrategy chargingStrategy,
            RouteStationFinder routeStationFinder,
            ChargingCandidateEvaluator chargingCandidateEvaluator) {

        this(
                routingProvider,
                vehicleRepository,
                energyConsumptionModel,
                chargingStrategy,
                routeStationFinder,
                chargingCandidateEvaluator,
                new MultiStopChargingPlanner(
                        energyConsumptionModel,
                        new com.evplanner.charging.ChargingStationEligibilityService(),
                        new com.evplanner.routing.RouteStationPositionCalculator()),
                DEFAULT_STATION_CORRIDOR_RADIUS_KM);
    }

    /**
     * Backward-compatible constructor for existing tests and callers that
     * explicitly provide the MultiStopChargingPlanner.
     */
    public DefaultJourneyIntelligenceEngine(
            RoutingProvider routingProvider,
            VehicleRepository vehicleRepository,
            EnergyConsumptionModel energyConsumptionModel,
            ChargingStrategy chargingStrategy,
            RouteStationFinder routeStationFinder,
            ChargingCandidateEvaluator chargingCandidateEvaluator,
            MultiStopChargingPlanner multiStopChargingPlanner) {

        this(
                routingProvider,
                vehicleRepository,
                energyConsumptionModel,
                chargingStrategy,
                routeStationFinder,
                chargingCandidateEvaluator,
                multiStopChargingPlanner,
                DEFAULT_STATION_CORRIDOR_RADIUS_KM);
    }

    /**
     * Production Spring constructor.
     *
     * The station corridor radius is configurable through:
     *
     * evplanner.routing.station-corridor-radius-km
     */
    @Autowired
    public DefaultJourneyIntelligenceEngine(
            RoutingProvider routingProvider,
            VehicleRepository vehicleRepository,
            EnergyConsumptionModel energyConsumptionModel,
            ChargingStrategy chargingStrategy,
            RouteStationFinder routeStationFinder,
            ChargingCandidateEvaluator chargingCandidateEvaluator,
            MultiStopChargingPlanner multiStopChargingPlanner,
            @Value("${evplanner.routing.station-corridor-radius-km:50.0}")
            double stationCorridorRadiusKm) {

        if (stationCorridorRadiusKm <= 0) {
            throw new IllegalArgumentException(
                    "Station corridor radius must be greater than zero");
        }

        this.stationCorridorRadiusKm = stationCorridorRadiusKm;
        this.routingProvider = routingProvider;
        this.vehicleRepository = vehicleRepository;
        this.energyConsumptionModel = energyConsumptionModel;
        this.chargingStrategy = chargingStrategy;
        this.routeStationFinder = routeStationFinder;
        this.multiStopChargingPlanner = multiStopChargingPlanner;
    }

    @Override
    public JourneyPlan plan(JourneyRequest request) {

        validate(request);

        Vehicle vehicle =
                vehicleRepository.findById(request.vehicleId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Vehicle not found: " +
                                                request.vehicleId()));

        List<RouteResult> routes =
                routingProvider.calculateRoutes(
                        request.origin(),
                        request.destination());

        if (routes == null || routes.isEmpty()) {
            throw new IllegalArgumentException(
                    "No routes found for the requested journey");
        }

        List<JourneyOption> options =
                routes.stream()
                        .filter(route -> route != null)
                        .map(route ->
                                evaluateRoute(
                                        vehicle,
                                        request,
                                        route))
                        .filter(option -> option != null)
                        .sorted(
                                Comparator.comparing(
                                        JourneyOption::overallScore,
                                        Comparator.reverseOrder()))
                        .toList();

        if (options.isEmpty()) {
            throw new IllegalArgumentException(
                    "No valid routes found for the requested journey");
        }

        return new JourneyPlan(
                options,
                options.get(0));
    }

    private JourneyOption evaluateRoute(
            Vehicle vehicle,
            JourneyRequest request,
            RouteResult route) {

        List<ChargingStation> stations =
                routeStationFinder.findNearRoute(
                        route,
                        stationCorridorRadiusKm);

        EnergyCalculationResult energy =
                energyConsumptionModel.calculate(
                        vehicle,
                        route.distanceKm(),
                        request.currentSocPercent(),
                        request.minimumArrivalSocPercent());

        ChargingDecision chargingDecision =
                chargingStrategy.evaluate(
                        energy,
                        request.currentSocPercent(),
                        request.minimumArrivalSocPercent());

        /*
         * Route can be completed without charging.
         */
        if (!chargingDecision.chargingRequired()) {

            return buildJourneyOption(
                    route,
                    energy,
                    chargingDecision,
                    List.of(),
                    null,
                    energy.arrivalSocPercent());
        }

        MultiStopChargingPlan chargingPlan =
                multiStopChargingPlanner.plan(
                        vehicle,
                        request.currentSocPercent(),
                        request.minimumArrivalSocPercent(),
                        route,
                        stations);

        if (chargingPlan == null ||
                chargingPlan.chargingStops().isEmpty()) {

            return null;
        }

        /*
         * Calculate the actual destination SOC after applying the
         * selected charging plan.
         */
        BigDecimal plannedArrivalSoc =
                calculatePlannedArrivalSoc(
                        vehicle,
                        route,
                        chargingPlan);

        return buildJourneyOption(
                route,
                energy,
                chargingDecision,
                List.of(),
                chargingPlan,
                plannedArrivalSoc);
    }

    private JourneyOption buildJourneyOption(
            RouteResult route,
            EnergyCalculationResult energy,
            ChargingDecision chargingDecision,
            List<ChargingStopCandidate> chargingCandidates,
            MultiStopChargingPlan chargingPlan,
            BigDecimal plannedArrivalSoc) {

        List<ChargingStopPlan> chargingStops =
                chargingPlan != null
                        ? chargingPlan.chargingStops()
                        : List.of();

        BigDecimal score =
                calculateInitialScore(
                        route,
                        energy,
                        chargingDecision,
                        chargingStops,
                        chargingPlan);

        return new JourneyOption(
                route.distanceKm(),
                route.durationMinutes(),
                chargingStops,
                plannedArrivalSoc,
                score);
    }

    /**
     * Calculates the actual SOC at the destination using the final
     * charging stop in the selected charging plan.
     *
     * targetSocPercent represents the departure SOC from the final
     * charging station.
     */
    private BigDecimal calculatePlannedArrivalSoc(
            Vehicle vehicle,
            RouteResult route,
            MultiStopChargingPlan chargingPlan) {

        List<ChargingStopPlan> stops =
                chargingPlan.chargingStops();

        if (stops == null || stops.isEmpty()) {
            return null;
        }

        ChargingStopPlan lastStop =
                stops.get(stops.size() - 1);

        BigDecimal remainingDistanceKm =
                route.distanceKm()
                        .subtract(
                                lastStop.distanceFromOriginKm())
                        .max(BigDecimal.ZERO);

        EnergyCalculationResult finalLeg =
                energyConsumptionModel.calculate(
                        vehicle,
                        remainingDistanceKm,
                        lastStop.targetSocPercent(),
                        BigDecimal.ZERO);

        return finalLeg.arrivalSocPercent()
                .setScale(
                        2,
                        RoundingMode.HALF_UP);
    }

    private BigDecimal calculateInitialScore(
            RouteResult route,
            EnergyCalculationResult energy,
            ChargingDecision chargingDecision,
            List<ChargingStopPlan> chargingStops,
            MultiStopChargingPlan chargingPlan) {

        /*
         * Higher is better.
         *
         * Driving time is the primary baseline and estimated charging
         * time is added when the route requires charging.
         */
        BigDecimal totalMinutes =
                BigDecimal.valueOf(
                        route.durationMinutes());

        if (chargingDecision.chargingRequired()) {

            int chargingMinutes =
                    chargingPlan != null
                            ? chargingPlan.totalChargingMinutes()
                            .setScale(
                                    0,
                                    RoundingMode.CEILING)
                            .intValue()
                            : chargingStops.stream()
                            .map(
                                    ChargingStopPlan::
                                            estimatedChargingMinutes)
                            .filter(
                                    java.util.Objects::
                                            nonNull)
                            .mapToInt(
                                    Integer::intValue)
                            .sum();

            totalMinutes =
                    totalMinutes.add(
                            BigDecimal.valueOf(
                                    chargingMinutes));
        }

        /*
         * Temporary scoring model.
         *
         * This will be replaced later by the full journey scoring model.
         */
        return BigDecimal.valueOf(1000)
                .subtract(totalMinutes)
                .max(BigDecimal.ZERO)
                .setScale(
                        2,
                        RoundingMode.HALF_UP);
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
                request.currentSocPercent()
                        .compareTo(BigDecimal.ZERO) < 0 ||
                request.currentSocPercent()
                        .compareTo(BigDecimal.valueOf(100)) > 0) {

            throw new IllegalArgumentException(
                    "Current SOC must be between 0 and 100");
        }

        if (request.minimumArrivalSocPercent() == null ||
                request.minimumArrivalSocPercent()
                        .compareTo(BigDecimal.ZERO) < 0 ||
                request.minimumArrivalSocPercent()
                        .compareTo(BigDecimal.valueOf(100)) > 0) {

            throw new IllegalArgumentException(
                    "Minimum arrival SOC must be between 0 and 100");
        }
    }
}