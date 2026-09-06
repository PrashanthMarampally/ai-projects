package com.evplanner.routing;

import com.evplanner.journey.GeoPoint;

import java.util.List;

public interface RoutingProvider {

    /**
     * Calculates the primary route between two points.
     *
     * Existing providers only need to implement this method. Providers that
     * support alternatives can override {@link #calculateRoutes(GeoPoint, GeoPoint)}.
     */
    RouteResult calculateRoute(
            GeoPoint origin,
            GeoPoint destination
    );

    /**
     * Returns the available route alternatives. The default implementation
     * preserves the existing single-route provider contract.
     */
    default List<RouteResult> calculateRoutes(
            GeoPoint origin,
            GeoPoint destination) {

        return List.of(calculateRoute(origin, destination));
    }
}
