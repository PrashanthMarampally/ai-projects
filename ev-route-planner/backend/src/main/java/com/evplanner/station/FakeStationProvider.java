package com.evplanner.station;

import com.evplanner.journey.GeoPoint;
import com.evplanner.provider.StationProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FakeStationProvider implements StationProvider {

    private final ChargingStationRepository repository;

    public FakeStationProvider(
            ChargingStationRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ChargingStation> findNearby(
            GeoPoint point,
            double radiusKm) {

        /*
         * Temporary MVP implementation.
         *
         * Real PostGIS spatial querying comes next.
         */
        return repository.findAll();
    }
}