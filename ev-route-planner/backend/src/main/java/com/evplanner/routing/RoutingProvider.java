package com.evplanner.routing;

import com.evplanner.journey.GeoPoint;

public interface RoutingProvider {

    RouteResult calculateRoute(
            GeoPoint origin,
            GeoPoint destination
    );
}