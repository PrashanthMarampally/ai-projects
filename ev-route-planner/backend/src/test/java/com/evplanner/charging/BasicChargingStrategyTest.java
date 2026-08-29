package com.evplanner.charging;

import com.evplanner.energy.EnergyCalculationResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BasicChargingStrategyTest {

    private final ChargingStrategy strategy =
            new BasicChargingStrategy();

    @Test
    void shouldNotRequireChargingWhenDestinationIsReachable() {

        EnergyCalculationResult energy =
                new EnergyCalculationResult(
                        BigDecimal.valueOf(30),
                        BigDecimal.valueOf(35),
                        true
                );

        ChargingDecision decision =
                strategy.evaluate(
                        energy,
                        BigDecimal.valueOf(65),
                        BigDecimal.valueOf(15)
                );

        assertFalse(decision.chargingRequired());

        assertEquals(
                0,
                decision.requiredChargePercent()
                        .compareTo(BigDecimal.ZERO)
        );
    }

    @Test
    void shouldRequireChargingWhenDestinationIsNotReachable() {

        EnergyCalculationResult energy =
                new EnergyCalculationResult(
                        BigDecimal.valueOf(50),
                        BigDecimal.valueOf(5),
                        false
                );

        ChargingDecision decision =
                strategy.evaluate(
                        energy,
                        BigDecimal.valueOf(55),
                        BigDecimal.valueOf(15)
                );

        assertTrue(decision.chargingRequired());

        assertEquals(
                0,
                decision.requiredChargePercent()
                        .compareTo(BigDecimal.TEN)
        );
    }
}