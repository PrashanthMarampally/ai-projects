package com.evplanner.routing;

import com.evplanner.journey.GeoPoint;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteGeometryService {

    private final GeometryFactory geometryFactory =
            new GeometryFactory();

    public LineString toLineString(
            List<GeoPoint> points) {

        if (points == null || points.size() < 2) {
            throw new IllegalArgumentException(
                    "At least two route points are required"
            );
        }

        Coordinate[] coordinates =
                points.stream()
                        .map(point -> new Coordinate(
                                point.longitude().doubleValue(),
                                point.latitude().doubleValue()
                        ))
                        .toArray(Coordinate[]::new);

        LineString lineString =
                geometryFactory.createLineString(coordinates);

        lineString.setSRID(4326);

        return lineString;
    }
}