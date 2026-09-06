package com.evplanner.journey;

import java.math.BigDecimal;

/** API-ready description of one charging stop. */
public record ChargingStopPlan(
        Long stationId,
        String stationName,
        BigDecimal distanceFromOriginKm,
        BigDecimal distanceToDestinationKm,
        BigDecimal arrivalSocPercent,
        BigDecimal targetSocPercent,
        BigDecimal energyToAddKwh,
        BigDecimal chargingPowerKw,
        Integer estimatedChargingMinutes
) {}
