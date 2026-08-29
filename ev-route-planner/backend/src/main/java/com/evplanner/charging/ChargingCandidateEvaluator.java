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

        return stations.stream()
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
                .filter(candidate ->
                        candidate != null)
                .sorted(
                        Comparator.comparing(
                                ChargingStopCandidate::distanceFromOriginKm))
                .toList();
    }

    private ChargingStopCandidate evaluateStation(
            Vehicle vehicle,
            BigDecimal currentSocPercent,
            BigDecimal minimumArrivalSocPercent,
            RouteResult route,
            ChargingStation station) {

        RouteStationPosition position =
                positionCalculator.calculate(
                        route,
                        station
                );

        EnergyCalculationResult energy =
                energyConsumptionModel.calculate(
                        vehicle,
                        position.distanceFromOriginKm(),
                        currentSocPercent,
                        minimumArrivalSocPercent
                );

        if (!energy.journeyPossible()) {
            return null;
        }

        BigDecimal arrivalSoc =
                energy.arrivalSocPercent()
                        .setScale(2, RoundingMode.HALF_UP);

        BigDecimal requiredCharge =
                minimumArrivalSocPercent
                        .subtract(arrivalSoc)
                        .max(BigDecimal.ZERO)
                        .setScale(2, RoundingMode.HALF_UP);

        BigDecimal chargingPowerKw =
                determineChargingPower(station);

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