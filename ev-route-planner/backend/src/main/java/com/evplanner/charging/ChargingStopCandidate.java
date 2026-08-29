package com.evplanner.charging;

import com.evplanner.station.ChargingStation;

import java.math.BigDecimal;

public record ChargingStopCandidate(
        ChargingStation station,
        BigDecimal distanceFromOriginKm,
        BigDecimal distanceToDestinationKm,
        BigDecimal arrivalSocPercent,
        BigDecimal requiredChargePercent,
        BigDecimal chargingPowerKw
) {
}