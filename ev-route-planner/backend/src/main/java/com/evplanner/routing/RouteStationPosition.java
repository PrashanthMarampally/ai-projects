package com.evplanner.routing;

import com.evplanner.station.ChargingStation;

import java.math.BigDecimal;

public record RouteStationPosition(
        ChargingStation station,
        BigDecimal distanceFromOriginKm,
        BigDecimal distanceToDestinationKm,
        BigDecimal detourKm
) {
}