package com.evplanner.station;

import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChargingStationFinder {

    private final ChargingStationRepository repository;

    public ChargingStationFinder(
            ChargingStationRepository repository) {
        this.repository = repository;
    }

    public List<ChargingStation> findNearby(
            Point point,
            double radiusKm) {

        if (point == null) {
            throw new IllegalArgumentException(
                    "Point must not be null");
        }

        if (radiusKm <= 0) {
            throw new IllegalArgumentException(
                    "Radius must be greater than zero");
        }

        return repository.findNearby(
                point,
                radiusKm * 1_000
        );
    }
}