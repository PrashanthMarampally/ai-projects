package com.evplanner.routing;

import com.evplanner.journey.GeoPoint;
import com.evplanner.station.ChargingStation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RouteStationPositionCalculatorTest {

    private final RouteStationPositionCalculator calculator =
            new RouteStationPositionCalculator();

    @Test
    void shouldProjectStationOntoRouteSegment() {
        RouteResult route = new RouteResult(
                BigDecimal.valueOf(20),
                30,
                List.of(
                        point(17.0000, 78.0000),
                        point(17.0000, 78.1800),
                        point(17.0000, 78.3000)
                )
        );

        ChargingStation station = station(17.0100, 78.1000);

        RouteStationPosition position = calculator.calculate(route, station);

        // The station projects onto the first segment, not merely to one of
        // its endpoints.  The exact route distance is scaled to the provider
        // supplied 20 km total.
        assertEquals(0, position.distanceFromOriginKm()
                .compareTo(BigDecimal.valueOf(6.67)));
        assertEquals(0, position.distanceToDestinationKm()
                .compareTo(BigDecimal.valueOf(13.33)));
        assertEquals(0, position.detourKm()
                .compareTo(BigDecimal.valueOf(1.11)));
    }

    @Test
    void shouldClampStationBeforeRouteStartToFirstSegment() {
        RouteResult route = new RouteResult(
                BigDecimal.TEN,
                20,
                List.of(
                        point(17.0000, 78.0000),
                        point(17.0000, 78.1000)
                )
        );

        ChargingStation station = station(17.0000, 77.9800);

        RouteStationPosition position = calculator.calculate(route, station);

        assertEquals(0, position.distanceFromOriginKm().compareTo(BigDecimal.ZERO));
        assertEquals(
                0,
                position.distanceToDestinationKm()
                        .compareTo(BigDecimal.TEN));
    }

    @Test
    void shouldRejectInvalidRouteGeometry() {
        RouteResult route = new RouteResult(
                BigDecimal.TEN,
                20,
                List.of(point(17.0000, 78.0000))
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculate(route, station(17.0, 78.0)));
    }

    private GeoPoint point(double latitude, double longitude) {
        return new GeoPoint(
                BigDecimal.valueOf(latitude),
                BigDecimal.valueOf(longitude));
    }

    private ChargingStation station(double latitude, double longitude) {
        return new ChargingStation(
                "test",
                "Test Station",
                BigDecimal.valueOf(latitude),
                BigDecimal.valueOf(longitude),
                null);
    }
}
