package com.evplanner.station;

import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Geometry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChargingStationRepository
        extends JpaRepository<ChargingStation, Long> {

    @Query(value = """
        SELECT *
        FROM charging_station
        WHERE ST_DWithin(
            location::geography,
            :point::geography,
            :radiusMeters
        )
        """, nativeQuery = true)
    List<ChargingStation> findNearby(
            Point point,
            double radiusMeters
    );

    @Query(value = """
    SELECT *
    FROM charging_station
    WHERE ST_DWithin(
        location::geography,
        ST_GeomFromText(
            ST_AsText(:routeGeometry),
            4326
        )::geography,
        :radiusMeters
    )
    """, nativeQuery = true)
    List<ChargingStation> findNearRoute(
            Geometry routeGeometry,
            double radiusMeters
    );
}