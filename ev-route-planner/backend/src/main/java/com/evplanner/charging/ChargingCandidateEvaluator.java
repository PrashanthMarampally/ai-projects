package com.evplanner.charging;

import com.evplanner.energy.EnergyCalculationResult;
import com.evplanner.energy.EnergyConsumptionModel;
import com.evplanner.station.ChargingConnector;
import com.evplanner.station.ChargingStation;
import com.evplanner.vehicle.Vehicle;
import com.evplanner.routing.RouteResult;
import com.evplanner.routing.RouteStationPosition;
import com.evplanner.routing.RouteStationPositionCalculator;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
public class ChargingCandidateEvaluator {

    private final EnergyConsumptionModel energyConsumptionModel;
    private final ChargingStationEligibilityService eligibilityService;
    private final RouteStationPositionCalculator positionCalculator;

    public ChargingCandidateEvaluator(
            EnergyConsumptionModel energyConsumptionModel,
            ChargingStationEligibilityService eligibilityService,
            RouteStationPositionCalculator positionCalculator) {

        this.energyConsumptionModel = energyConsumptionModel;
        this.eligibilityService = eligibilityService;
        this.positionCalculator = positionCalculator;
    }

    public List<ChargingStopCandidate> evaluate(
            Vehicle vehicle,
            BigDecimal currentSocPercent,
            BigDecimal minimumArrivalSocPercent,
            RouteResult route,
            List<ChargingStation> stations) {

        if (stations == null || stations.isEmpty()) {
            return List.of();
        }

        return stations.stream()
                .filter(station -> station != null)
                .filter(station ->
                        eligibilityService.isEligible(
                                vehicle,
                                station))
                .map(station ->
                        evaluateStation(
                                vehicle,
                                currentSocPercent,
                                minimumArrivalSocPercent,
                                route,
                                station))
                .filter(candidate -> candidate != null)
                .sorted(candidateComparator(route, vehicle))
                .toList();
    }

    private ChargingStopCandidate evaluateStation(
            Vehicle vehicle,
            BigDecimal currentSocPercent,
            BigDecimal minimumArrivalSocPercent,
            RouteResult route,
            ChargingStation station) {

        RouteStationPosition position =
                positionCalculator.calculate(route, station);

        // First determine whether the vehicle can physically reach the station
        // while maintaining the required reserve.
        EnergyCalculationResult stationEnergy =
                energyConsumptionModel.calculate(
                        vehicle,
                        position.distanceFromOriginKm(),
                        currentSocPercent,
                        minimumArrivalSocPercent
                );

        if (!stationEnergy.journeyPossible()) {
            return null;
        }

        BigDecimal arrivalSoc =
                stationEnergy.arrivalSocPercent()
                        .setScale(2, RoundingMode.HALF_UP);

        // Calculate the energy required from the station to the destination.
        // Energy consumption is independent of the starting SOC, so 100% is
        // used simply to obtain the required energy for the remaining distance.
        EnergyCalculationResult remainingEnergy =
                energyConsumptionModel.calculate(
                        vehicle,
                        position.distanceToDestinationKm(),
                        BigDecimal.valueOf(100),
                        BigDecimal.ZERO
                );

        BigDecimal usableBatteryKwh = vehicle.getUsableBatteryCapacityKwh();
        if (usableBatteryKwh == null || usableBatteryKwh.signum() <= 0) {
            usableBatteryKwh = vehicle.getBatteryCapacityKwh();
        }
        if (usableBatteryKwh == null || usableBatteryKwh.signum() <= 0) {
            return null;
        }

        BigDecimal remainingEnergyPercent =
                remainingEnergy.energyRequiredKwh()
                        .divide(usableBatteryKwh, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

        // Required SOC at the charging station is the energy needed for the
        // remaining route plus the required destination reserve.
        BigDecimal targetSoc =
                remainingEnergyPercent
                        .add(minimumArrivalSocPercent);

        // Even a 100% charge at this station cannot make the destination
        // if the required SOC exceeds 100%.
        if (targetSoc.compareTo(BigDecimal.valueOf(100)) > 0) {
            return null;
        }

        BigDecimal requiredCharge =
                targetSoc
                        .subtract(arrivalSoc)
                        .max(BigDecimal.ZERO)
                        .setScale(2, RoundingMode.HALF_UP);

        BigDecimal chargingPowerKw = determineChargingPower(station);
        if (chargingPowerKw.signum() <= 0) {
            return null;
        }

        return new ChargingStopCandidate(
                station,
                position.distanceFromOriginKm(),
                position.distanceToDestinationKm(),
                arrivalSoc,
                requiredCharge,
                chargingPowerKw
        );
    }

    private Comparator<ChargingStopCandidate> candidateComparator(
            RouteResult route,
            Vehicle vehicle) {

        BigDecimal batteryCapacityKwh = vehicle.getUsableBatteryCapacityKwh();
        if (batteryCapacityKwh == null || batteryCapacityKwh.signum() <= 0) {
            batteryCapacityKwh = vehicle.getBatteryCapacityKwh();
        }
        final BigDecimal battery = batteryCapacityKwh;

        return Comparator
                // Optimize total additional trip time: charging time plus
                // the time represented by the station's route detour.
                .comparing((ChargingStopCandidate candidate) ->
                        estimatedAdditionalTripMinutes(
                                candidate, battery, route))
                // If total time is equal, prefer the smaller physical detour.
                .thenComparing(candidate ->
                        positionCalculator.calculate(
                                route,
                                candidate.station()).detourKm())
                // Prefer a healthier arrival SOC when the time and detour tie.
                .thenComparing(
                        ChargingStopCandidate::arrivalSocPercent,
                        Comparator.reverseOrder())
                // Finally prefer higher charging power.
                .thenComparing(
                        ChargingStopCandidate::chargingPowerKw,
                        Comparator.reverseOrder());
    }

    private BigDecimal estimatedAdditionalTripMinutes(
            ChargingStopCandidate candidate,
            BigDecimal batteryCapacityKwh,
            RouteResult route) {

        BigDecimal chargingMinutes =
                estimatedChargingMinutes(
                        candidate,
                        batteryCapacityKwh);

        if (route.distanceKm() == null ||
                route.distanceKm().signum() <= 0 ||
                route.durationMinutes() <= 0) {
            return chargingMinutes;
        }

        BigDecimal averageDriveMinutesPerKm =
                BigDecimal.valueOf(route.durationMinutes())
                        .divide(
                                route.distanceKm(),
                                6,
                                RoundingMode.HALF_UP);

        BigDecimal detourMinutes =
                positionCalculator.calculate(
                                route,
                                candidate.station())
                        .detourKm()
                        .multiply(averageDriveMinutesPerKm);

        return chargingMinutes.add(detourMinutes);
    }

    private BigDecimal estimatedChargingMinutes(
            ChargingStopCandidate candidate,
            BigDecimal batteryCapacityKwh) {

        if (batteryCapacityKwh == null ||
                batteryCapacityKwh.signum() <= 0) {
            return BigDecimal.valueOf(Double.MAX_VALUE);
        }

        BigDecimal energyRequiredKwh =
                batteryCapacityKwh
                        .multiply(candidate.requiredChargePercent())
                        .divide(
                                BigDecimal.valueOf(100),
                                4,
                                RoundingMode.HALF_UP);

        return energyRequiredKwh
                .divide(
                        candidate.chargingPowerKw(),
                        4,
                        RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(60));
    }

    private BigDecimal determineChargingPower(
            ChargingStation station) {

        return station.getConnectors()
                .stream()
                .filter(connector ->
                        connector.getPowerKw() != null)
                .filter(connector ->
                        connector.getPowerKw().signum() > 0)
                .map(ChargingConnector::getPowerKw)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }
}