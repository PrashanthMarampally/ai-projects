package com.evplanner.charging;

import com.evplanner.energy.EnergyCalculationResult;
import com.evplanner.energy.EnergyConsumptionModel;
import com.evplanner.journey.ChargingStopPlan;
import com.evplanner.routing.RouteResult;
import com.evplanner.routing.RouteStationPosition;
import com.evplanner.routing.RouteStationPositionCalculator;
import com.evplanner.station.ChargingStation;
import com.evplanner.vehicle.Vehicle;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Bounded multi-stop charging optimizer for a single route.
 *
 * The MVP searches possible forward station sequences. At an existing
 * charging station it charges only the amount needed to reach the next node
 * while preserving the configured destination reserve.
 *
 * The planner distinguishes between:
 *
 * - SOC when arriving at the charging station
 * - SOC when departing the charging station after charging
 * - SOC when arriving at the destination
 *
 * This keeps ChargingStopPlan SOC values semantically correct.
 */
@Service
public class MultiStopChargingPlanner {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final int MAX_STOPS = 4;

    private final EnergyConsumptionModel energyConsumptionModel;
    private final ChargingStationEligibilityService eligibilityService;
    private final RouteStationPositionCalculator positionCalculator;

    public MultiStopChargingPlanner(
            EnergyConsumptionModel energyConsumptionModel,
            ChargingStationEligibilityService eligibilityService,
            RouteStationPositionCalculator positionCalculator) {

        this.energyConsumptionModel = energyConsumptionModel;
        this.eligibilityService = eligibilityService;
        this.positionCalculator = positionCalculator;
    }

    public MultiStopChargingPlan plan(
            Vehicle vehicle,
            BigDecimal currentSocPercent,
            BigDecimal minimumArrivalSocPercent,
            RouteResult route,
            List<ChargingStation> stations) {

        if (stations == null || stations.isEmpty()) {
            return null;
        }

        List<PositionedStation> candidates = stations.stream()
                .filter(station -> station != null)
                .filter(station ->
                        eligibilityService.isEligible(vehicle, station))
                .map(station -> new PositionedStation(
                        station,
                        positionCalculator.calculate(route, station)))
                .filter(candidate ->
                        candidate.position()
                                .distanceFromOriginKm()
                                .compareTo(route.distanceKm()) < 0)
                .sorted(Comparator.comparing(candidate ->
                        candidate.position()
                                .distanceFromOriginKm()))
                .toList();

        SearchResult result = search(
                vehicle,
                currentSocPercent,
                minimumArrivalSocPercent,
                route,
                candidates,
                ZERO,
                null,
                List.of(),
                ZERO,
                ZERO,
                new HashSet<>());

        if (result == null) {
            return null;
        }

        return new MultiStopChargingPlan(
                List.copyOf(result.recommendations()),
                result.totalChargingMinutes()
                        .setScale(2, RoundingMode.HALF_UP),
                result.totalDetourKm()
                        .setScale(2, RoundingMode.HALF_UP));
    }

    private SearchResult search(
            Vehicle vehicle,
            BigDecimal currentSocPercent,
            BigDecimal minimumArrivalSocPercent,
            RouteResult route,
            List<PositionedStation> stations,
            BigDecimal currentDistanceKm,
            PositionedStation currentStation,
            List<ChargingStopPlan> recommendations,
            BigDecimal totalChargingMinutes,
            BigDecimal totalDetourKm,
            Set<Long> visited) {

        BigDecimal destinationDistance =
                route.distanceKm()
                        .subtract(currentDistanceKm)
                        .max(ZERO);

        /*
         * Try to reach the destination from the current node.
         */
        Transition toDestination = transition(
                vehicle,
                currentSocPercent,
                minimumArrivalSocPercent,
                destinationDistance,
                currentStation);

        if (toDestination.reachable()) {

            List<ChargingStopPlan> finalRecommendations =
                    addChargingRecommendation(
                            recommendations,
                            currentStation,
                            toDestination);

            return new SearchResult(
                    finalRecommendations,
                    totalChargingMinutes.add(
                            toDestination.chargingMinutes()),
                    totalDetourKm);
        }

        if (recommendations.size() >= MAX_STOPS) {
            return null;
        }

        /*
         * At the origin there is no charging station, so the first station
         * must be reachable using the current SOC.
         */
        SearchResult best = null;

        for (PositionedStation next : stations) {

            if (next.position()
                    .distanceFromOriginKm()
                    .compareTo(currentDistanceKm) <= 0) {
                continue;
            }

            Long stationId = next.station().getId();

            if (stationId != null &&
                    visited.contains(stationId)) {
                continue;
            }

            BigDecimal legDistance =
                    next.position()
                            .distanceFromOriginKm()
                            .subtract(currentDistanceKm);

            Transition transition = transition(
                    vehicle,
                    currentSocPercent,
                    minimumArrivalSocPercent,
                    legDistance,
                    currentStation);

            if (!transition.reachable()) {
                continue;
            }

            List<ChargingStopPlan> nextRecommendations =
                    addChargingRecommendation(
                            recommendations,
                            currentStation,
                            transition);

            Set<Long> nextVisited =
                    new HashSet<>(visited);

            if (stationId != null) {
                nextVisited.add(stationId);
            }

            SearchResult candidate = search(
                    vehicle,
                    transition.destinationArrivalSocPercent(),
                    minimumArrivalSocPercent,
                    route,
                    stations,
                    next.position().distanceFromOriginKm(),
                    next,
                    nextRecommendations,
                    totalChargingMinutes.add(
                            transition.chargingMinutes()),
                    totalDetourKm.add(
                            next.position().detourKm()),
                    nextVisited);

            if (candidate != null &&
                    isBetter(candidate, best)) {

                best = candidate;
            }
        }

        return best;
    }

    /**
     * Adds a charging stop for the current station when charging was required
     * to reach the next node.
     *
     * Important:
     *
     * arrivalSocPercent  = SOC when the vehicle arrives at this station
     * targetSocPercent   = SOC after charging / departure SOC
     */
    private List<ChargingStopPlan> addChargingRecommendation(
            List<ChargingStopPlan> recommendations,
            PositionedStation currentStation,
            Transition transition) {

        if (currentStation == null ||
                transition.chargingMinutes()
                        .compareTo(ZERO) <= 0) {

            return recommendations;
        }

        List<ChargingStopPlan> result =
                new ArrayList<>(recommendations);

        result.add(
                new ChargingStopPlan(
                        currentStation.station().getId(),
                        currentStation.station().getName(),
                        currentStation.position()
                                .distanceFromOriginKm(),
                        currentStation.position()
                                .distanceToDestinationKm(),

                        /*
                         * SOC on arrival at the charging station.
                         */
                        transition.stationArrivalSocPercent(),

                        /*
                         * SOC after charging / departure.
                         */
                        transition.targetSocPercent(),

                        transition.energyToAddKwh(),
                        transition.chargingPowerKw(),
                        transition.chargingMinutes()
                                .setScale(
                                        0,
                                        RoundingMode.CEILING)
                                .intValue()));

        return result;
    }

    /**
     * Calculates whether a leg is reachable and, when necessary, how much
     * charging is required at the current station.
     */
    private Transition transition(
            Vehicle vehicle,
            BigDecimal currentSocPercent,
            BigDecimal minimumArrivalSocPercent,
            BigDecimal distanceKm,
            PositionedStation currentStation) {

        EnergyCalculationResult energy =
                energyConsumptionModel.calculate(
                        vehicle,
                        distanceKm,
                        currentSocPercent,
                        minimumArrivalSocPercent);

        BigDecimal destinationArrivalWithoutCharging =
                energy.arrivalSocPercent()
                        .setScale(
                                2,
                                RoundingMode.HALF_UP);

        /*
         * The entire leg can be completed without charging.
         */
        if (energy.journeyPossible()) {

            return new Transition(
                    true,

                    /*
                     * The current SOC is also the station arrival SOC
                     * for this transition.
                     */
                    currentSocPercent,

                    /*
                     * No charging, therefore departure SOC is unchanged.
                     */
                    currentSocPercent,

                    /*
                     * SOC when arriving at the next node/destination.
                     */
                    destinationArrivalWithoutCharging,

                    ZERO,
                    ZERO,
                    ZERO);
        }

        /*
         * The origin is not a charging node in the MVP.
         *
         * Therefore, if the first station/destination is out of range,
         * this path cannot be used.
         */
        if (currentStation == null) {
            return Transition.unreachable();
        }

        BigDecimal battery =
                vehicle.getUsableBatteryCapacityKwh();

        if (battery == null ||
                battery.signum() <= 0) {

            battery =
                    vehicle.getBatteryCapacityKwh();
        }

        if (battery == null ||
                battery.signum() <= 0) {

            return Transition.unreachable();
        }

        /*
         * Energy required for this leg expressed as SOC percentage.
         */
        BigDecimal energyPercent =
                energy.energyRequiredKwh()
                        .divide(
                                battery,
                                6,
                                RoundingMode.HALF_UP)
                        .multiply(ONE_HUNDRED);

        /*
         * We need enough SOC at departure to:
         *
         * energy required for the leg
         * +
         * minimum arrival reserve.
         */
        BigDecimal targetSoc =
                energyPercent
                        .add(minimumArrivalSocPercent);

        if (targetSoc.compareTo(ONE_HUNDRED) > 0) {
            return Transition.unreachable();
        }

        /*
         * Amount of SOC that must be added at this station.
         */
        BigDecimal requiredCharge =
                targetSoc
                        .subtract(currentSocPercent)
                        .max(ZERO)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP);

        BigDecimal power =
                determineChargingPower(
                        currentStation.station());

        if (power.signum() <= 0) {
            return Transition.unreachable();
        }

        BigDecimal energyToAdd =
                battery
                        .multiply(requiredCharge)
                        .divide(
                                ONE_HUNDRED,
                                6,
                                RoundingMode.HALF_UP);

        BigDecimal chargingMinutes =
                energyToAdd
                        .divide(
                                power,
                                6,
                                RoundingMode.HALF_UP)
                        .multiply(
                                BigDecimal.valueOf(60));

        /*
         * SOC after charging and before leaving the station.
         */
        BigDecimal departureSoc =
                currentSocPercent
                        .add(requiredCharge)
                        .min(ONE_HUNDRED)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP);

        /*
         * SOC after completing this leg.
         *
         * This is the SOC used when the recursive search arrives
         * at the next station or destination.
         */
        BigDecimal destinationArrivalSoc =
                departureSoc
                        .subtract(energyPercent)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP);

        return new Transition(
                true,

                /*
                 * SOC when arriving at the charging station.
                 */
                currentSocPercent
                        .setScale(
                                2,
                                RoundingMode.HALF_UP),

                /*
                 * SOC after charging / departure.
                 */
                departureSoc,

                /*
                 * SOC at the end of this leg.
                 */
                destinationArrivalSoc,

                chargingMinutes,
                energyToAdd,
                power);
    }

    private boolean isBetter(
            SearchResult candidate,
            SearchResult currentBest) {

        if (currentBest == null) {
            return true;
        }

        int time =
                candidate.totalChargingMinutes()
                        .compareTo(
                                currentBest.totalChargingMinutes());

        if (time != 0) {
            return time < 0;
        }

        int detour =
                candidate.totalDetourKm()
                        .compareTo(
                                currentBest.totalDetourKm());

        if (detour != 0) {
            return detour < 0;
        }

        return candidate.recommendations().size()
                < currentBest.recommendations().size();
    }

    private BigDecimal determineChargingPower(
            ChargingStation station) {

        return station.getConnectors()
                .stream()
                .filter(connector ->
                        connector.getPowerKw() != null)
                .filter(connector ->
                        connector.getPowerKw().signum() > 0)
                .map(connector ->
                        connector.getPowerKw())
                .max(BigDecimal::compareTo)
                .orElse(ZERO);
    }

    private record PositionedStation(
            ChargingStation station,
            RouteStationPosition position) {
    }

    private record Transition(
            boolean reachable,

            /*
             * SOC when arriving at the current charging station.
             */
            BigDecimal stationArrivalSocPercent,

            /*
             * SOC after charging / leaving the current station.
             */
            BigDecimal targetSocPercent,

            /*
             * SOC after completing the current leg.
             * This becomes the current SOC at the next recursive node.
             */
            BigDecimal destinationArrivalSocPercent,

            BigDecimal chargingMinutes,
            BigDecimal energyToAddKwh,
            BigDecimal chargingPowerKw) {

        private static Transition unreachable() {

            return new Transition(
                    false,
                    ZERO, // stationArrivalSocPercent
                    ZERO, // targetSocPercent
                    ZERO, // destinationArrivalSocPercent
                    ZERO, // chargingMinutes
                    ZERO, // energyToAddKwh
                    ZERO  // chargingPowerKw
            );
        }
    }

    private record SearchResult(
            List<ChargingStopPlan> recommendations,
            BigDecimal totalChargingMinutes,
            BigDecimal totalDetourKm) {
    }
}
