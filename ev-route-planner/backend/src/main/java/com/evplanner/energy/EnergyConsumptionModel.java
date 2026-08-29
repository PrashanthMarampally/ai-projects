package com.evplanner.energy;

import com.evplanner.vehicle.Vehicle;
import java.math.BigDecimal;

public interface EnergyConsumptionModel {

    EnergyCalculationResult calculate(
            Vehicle vehicle,
            BigDecimal distanceKm,
            BigDecimal currentSocPercent,
            BigDecimal minimumArrivalSocPercent
    );
}