package com.evplanner.provider;

import com.evplanner.journey.GeoPoint;
import com.evplanner.station.ChargingStation;

import java.util.List;

public interface StationProvider {

    List<ChargingStation> findNearby(
            GeoPoint point,
            double radiusKm
    );
}