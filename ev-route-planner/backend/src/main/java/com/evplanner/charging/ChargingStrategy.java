package com.evplanner.charging;

import com.evplanner.energy.EnergyCalculationResult;

import java.math.BigDecimal;

public interface ChargingStrategy {

    ChargingDecision evaluate(
            EnergyCalculationResult energyCalculation,
            BigDecimal currentSocPercent,
            BigDecimal minimumArrivalSocPercent
    );
}