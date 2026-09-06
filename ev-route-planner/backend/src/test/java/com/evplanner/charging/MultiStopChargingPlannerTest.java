package com.evplanner.charging;

import com.evplanner.energy.BasicEnergyConsumptionModel;
import com.evplanner.journey.GeoPoint;
import com.evplanner.routing.RouteResult;
import com.evplanner.routing.RouteStationPositionCalculator;
import com.evplanner.station.ChargingConnector;
import com.evplanner.station.ChargingStation;
import com.evplanner.vehicle.Vehicle;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultiStopChargingPlannerTest {

    private final MultiStopChargingPlanner planner =
            new MultiStopChargingPlanner(
                    new BasicEnergyConsumptionModel(),
                    new ChargingStationEligibilityService(),
                    new RouteStationPositionCalculator());

    @Test
    void shouldUseTwoStopsWhenNoSingleStationCanReachDestination() {
        Vehicle vehicle = vehicle();
        RouteResult route = route(500);

        ChargingStation first = station("S1", 100, 60);
        ChargingStation second = station("S2", 300, 60);
        first.setId(1L);
        second.setId(2L);

        MultiStopChargingPlan plan = planner.plan(
                vehicle,
                BigDecimal.valueOf(60),
                BigDecimal.valueOf(15),
                route,
                List.of(first, second));

        assertNotNull(plan);
        assertEquals(2, plan.chargingRecommendations().size());
        assertEquals("S1", planStationExternalId(plan, first, second, 0));
        assertEquals("S2", planStationExternalId(plan, first, second, 1));
    }

    @Test
    void shouldPreferOneStopWhenItRequiresLessChargingTime() {
        Vehicle vehicle = vehicle();
        RouteResult route = route(300);

        ChargingStation early = station("EARLY", 90, 30);
        ChargingStation late = station("LATE", 200, 60);
        early.setId(3L);
        late.setId(4L);

        MultiStopChargingPlan plan = planner.plan(
                vehicle,
                BigDecimal.valueOf(85),
                BigDecimal.valueOf(15),
                route,
                List.of(early, late));

        assertNotNull(plan);
        assertEquals(1, plan.chargingRecommendations().size());
        assertEquals(late.getId(), plan.chargingRecommendations().get(0).stationId());
    }

    private String planStationExternalId(
            MultiStopChargingPlan plan,
            ChargingStation first,
            ChargingStation second,
            int index) {

        Long id = plan.chargingRecommendations().get(index).stationId();

        if (id != null && id.equals(first.getId())) {
            return first.getExternalId();
        }

        if (id != null && id.equals(second.getId())) {
            return second.getExternalId();
        }

        return null;
    }

    private Vehicle vehicle() {
        return new Vehicle(
                "Test EV",
                BigDecimal.valueOf(60),
                BigDecimal.valueOf(60),
                BigDecimal.valueOf(400),
                BigDecimal.valueOf(20),
                "CCS2");
    }

    private RouteResult route(double distanceKm) {
        BigDecimal endpointLatitude =
                BigDecimal.valueOf(distanceKm / 111.0);

        BigDecimal midpointLatitude =
                BigDecimal.valueOf(distanceKm / 2.0 / 111.0);

        return new RouteResult(
                BigDecimal.valueOf(distanceKm),
                300,
                List.of(
                        new GeoPoint(BigDecimal.ZERO, BigDecimal.ZERO),
                        new GeoPoint(midpointLatitude, BigDecimal.ZERO),
                        new GeoPoint(endpointLatitude, BigDecimal.ZERO)));
    }

    private ChargingStation station(
            String externalId,
            double routeDistanceKm,
            double powerKw) {

        double latitude = routeDistanceKm / 111.0;

        GeometryFactory geometryFactory = new GeometryFactory();

        ChargingStation station = new ChargingStation(
                externalId,
                externalId,
                BigDecimal.valueOf(latitude),
                BigDecimal.ZERO,
                geometryFactory.createPoint(
                        new Coordinate(0, latitude)));

        ChargingConnector connector = new ChargingConnector(
                station,
                "CCS2",
                BigDecimal.valueOf(powerKw),
                2,
                2);

        station.addConnector(connector);

        return station;
    }
}
