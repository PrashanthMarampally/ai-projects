package com.evplanner.charging;

import com.evplanner.station.ChargingStation;

import java.math.BigDecimal;

public record ChargingStationCandidate(
        ChargingStation station,
        BigDecimal distanceFromRouteOriginKm,
        BigDecimal distanceToDestinationKm,
        BigDecimal estimatedArrivalSocPercent,
        boolean reachable
) {
}