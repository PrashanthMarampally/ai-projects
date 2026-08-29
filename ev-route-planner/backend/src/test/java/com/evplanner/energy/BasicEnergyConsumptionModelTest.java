package com.evplanner.energy;

import com.evplanner.vehicle.Vehicle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BasicEnergyConsumptionModelTest {

    @Test
    void shouldCalculateArrivalSoc() {

        Vehicle vehicle = new Vehicle(
                "Test EV",
                BigDecimal.valueOf(60),
                BigDecimal.valueOf(57),
                BigDecimal.valueOf(400),
                BigDecimal.valueOf(14.25),
                "CCS2"
        );

        EnergyConsumptionModel model =
                new BasicEnergyConsumptionModel();

        EnergyCalculationResult result =
                model.calculate(
                        vehicle,
                        BigDecimal.valueOf(200),
                        BigDecimal.valueOf(80),
                        BigDecimal.valueOf(15)
                );

        assertEquals(
                0,
                result.energyRequiredKwh().compareTo(BigDecimal.valueOf(28.5))
        );

        assertEquals(
                0,
                result.arrivalSocPercent().compareTo(BigDecimal.valueOf(30))
        );

        assertTrue(result.journeyPossible());
    }
}