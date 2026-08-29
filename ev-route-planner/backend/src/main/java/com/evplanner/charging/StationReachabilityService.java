package com.evplanner.charging;

import com.evplanner.energy.EnergyCalculationResult;
import com.evplanner.energy.EnergyConsumptionModel;
import com.evplanner.station.ChargingStation;
import com.evplanner.vehicle.Vehicle;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class StationReachabilityService {

    private final EnergyConsumptionModel energyConsumptionModel;

    public StationReachabilityService(
            EnergyConsumptionModel energyConsumptionModel) {

        this.energyConsumptionModel = energyConsumptionModel;
    }

    public ChargingStationCandidate evaluate(
            Vehicle vehicle,
            ChargingStation station,
            BigDecimal distanceFromOriginKm,
            BigDecimal currentSocPercent,
            BigDecimal minimumArrivalSocPercent) {

        EnergyCalculationResult result =
                energyConsumptionModel.calculate(
                        vehicle,
                        distanceFromOriginKm,
                        currentSocPercent,
                        minimumArrivalSocPercent
                );

        BigDecimal arrivalSoc =
                result.arrivalSocPercent()
                        .setScale(2, RoundingMode.HALF_UP);

        boolean reachable =
                result.journeyPossible();

        BigDecimal requiredCharge =
                minimumArrivalSocPercent
                        .subtract(arrivalSoc)
                        .max(BigDecimal.ZERO)
                        .setScale(2, RoundingMode.HALF_UP);

        return new ChargingStationCandidate(
                station,
                distanceFromOriginKm,
                arrivalSoc,
                requiredCharge,
                reachable
        );
    }
}