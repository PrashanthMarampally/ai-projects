package com.evplanner.charging;

import java.math.BigDecimal;

public record ChargingDecision(
        boolean chargingRequired,
        BigDecimal arrivalSocPercent,
        BigDecimal requiredChargePercent
) {
}