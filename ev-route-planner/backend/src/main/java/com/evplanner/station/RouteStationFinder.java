package com.evplanner.station;

import com.evplanner.routing.RouteGeometryService;
import com.evplanner.routing.RouteResult;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteStationFinder {

    private final ChargingStationRepository repository;
    private final RouteGeometryService routeGeometryService;

    public RouteStationFinder(
            ChargingStationRepository repository,
            RouteGeometryService routeGeometryService) {

        this.repository = repository;
        this.routeGeometryService = routeGeometryService;
    }

    public List<ChargingStation> findNearRoute(
            RouteResult route,
            double corridorRadiusKm) {

        Geometry routeGeometry =
                routeGeometryService.toLineString(
                        route.geometry()
                );

        return repository.findNearRoute(
                routeGeometry,
                corridorRadiusKm * 1_000
        );
    }
}