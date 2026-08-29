package com.evplanner.charging;

import com.evplanner.energy.EnergyCalculationResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class BasicChargingStrategy implements ChargingStrategy {

    @Override
    public ChargingDecision evaluate(
            EnergyCalculationResult energyCalculation,
            BigDecimal currentSocPercent,
            BigDecimal minimumArrivalSocPercent) {

        if (energyCalculation.journeyPossible()) {
            return new ChargingDecision(
                    false,
                    energyCalculation.arrivalSocPercent(),
                    BigDecimal.ZERO
            );
        }

        BigDecimal requiredChargePercent =
                minimumArrivalSocPercent
                        .subtract(energyCalculation.arrivalSocPercent())
                        .setScale(2, RoundingMode.HALF_UP);

        return new ChargingDecision(
                true,
                energyCalculation.arrivalSocPercent(),
                requiredChargePercent
        );
    }
}