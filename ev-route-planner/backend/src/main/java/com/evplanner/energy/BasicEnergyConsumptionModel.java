package com.evplanner.energy;

import com.evplanner.vehicle.Vehicle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class BasicEnergyConsumptionModel
        implements EnergyConsumptionModel {

    @Override
    public EnergyCalculationResult calculate(
            Vehicle vehicle,
            BigDecimal distanceKm,
            BigDecimal currentSocPercent,
            BigDecimal minimumArrivalSocPercent) {

        BigDecimal energyRequired = distanceKm
                .multiply(vehicle.getConsumptionKwhPer100Km())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        BigDecimal usableBattery =
                vehicle.getUsableBatteryCapacityKwh();

        BigDecimal currentEnergy = usableBattery
                .multiply(currentSocPercent)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        BigDecimal arrivalEnergy =
                currentEnergy.subtract(energyRequired);

        BigDecimal arrivalSoc = arrivalEnergy
                .divide(usableBattery, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        boolean possible =
                arrivalSoc.compareTo(minimumArrivalSocPercent) >= 0;

        return new EnergyCalculationResult(
                energyRequired,
                arrivalSoc,
                possible
        );
    }
}