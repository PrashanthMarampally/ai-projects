package com.evplanner.station;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ChargingStationFinderTest {

    @Test
    void shouldRejectNullPoint() {

        ChargingStationRepository repository =
                org.mockito.Mockito.mock(
                        ChargingStationRepository.class);

        ChargingStationFinder finder =
                new ChargingStationFinder(repository);

        assertThrows(
                IllegalArgumentException.class,
                () -> finder.findNearby(null, 10)
        );
    }

    @Test
    void shouldRejectInvalidRadius() {

        ChargingStationRepository repository =
                org.mockito.Mockito.mock(
                        ChargingStationRepository.class);

        ChargingStationFinder finder =
                new ChargingStationFinder(repository);

        var geometryFactory =
                new org.locationtech.jts.geom.GeometryFactory();

        var point = geometryFactory.createPoint(
                new org.locationtech.jts.geom.Coordinate(
                        78.4867,
                        17.3850
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> finder.findNearby(point, 0)
        );
    }
}