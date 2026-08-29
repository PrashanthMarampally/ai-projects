package com.evplanner.energy;

import java.math.BigDecimal;

public record EnergyCalculationResult(
        BigDecimal energyRequiredKwh,
        BigDecimal arrivalSocPercent,
        boolean journeyPossible
) {
}