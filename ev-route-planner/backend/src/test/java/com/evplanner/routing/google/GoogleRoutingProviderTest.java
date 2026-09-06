package com.evplanner.routing.google;

import com.evplanner.journey.GeoPoint;
import com.evplanner.routing.RouteResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleRoutingProviderTest {

    private final GoogleRoutesClient client = mock(GoogleRoutesClient.class);
    private final GoogleRoutingProvider provider = new GoogleRoutingProvider(client);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldMapGoogleRoutesToDomainRoutes() throws Exception {
        String json = """
                {
                  "routes": [
                    {
                      "distanceMeters": 575400,
                      "duration": "30720s",
                      "polyline": {
                        "geoJsonLinestring": {
                          "type": "LineString",
                          "coordinates": [
                            [78.4867, 17.3850],
                            [77.9000, 15.5000],
                            [77.5946, 12.9716]
                          ]
                        }
                      }
                    },
                    {
                      "distanceMeters": 590000,
                      "duration": "32000s",
                      "polyline": {
                        "geoJsonLinestring": {
                          "type": "LineString",
                          "coordinates": [
                            [78.4867, 17.3850],
                            [77.5946, 12.9716]
                          ]
                        }
                      }
                    }
                  ]
                }
                """;

        JsonNode response = objectMapper.readTree(json);
        when(client.computeRoutes(any(), any())).thenReturn(response);

        List<RouteResult> routes = provider.calculateRoutes(
                new GeoPoint(BigDecimal.valueOf(17.3850), BigDecimal.valueOf(78.4867)),
                new GeoPoint(BigDecimal.valueOf(12.9716), BigDecimal.valueOf(77.5946)));

        assertEquals(2, routes.size());
        assertEquals(0, routes.get(0).distanceKm().compareTo(BigDecimal.valueOf(575.400)));
        assertEquals(512, routes.get(0).durationMinutes());
        assertEquals(3, routes.get(0).geometry().size());
        assertEquals(0, routes.get(0).geometry().get(0).latitude()
                .compareTo(BigDecimal.valueOf(17.3850)));
        assertEquals(0, routes.get(0).geometry().get(0).longitude()
                .compareTo(BigDecimal.valueOf(78.4867)));
    }

    @Test
    void shouldReturnFirstRouteFromCalculateRoute() throws Exception {
        String json = """
                {
                  "routes": [
                    {
                      "distanceMeters": 100000,
                      "duration": "3600s",
                      "polyline": {
                        "geoJsonLinestring": {
                          "type": "LineString",
                          "coordinates": [[78.0, 17.0], [77.0, 16.0]]
                        }
                      }
                    }
                  ]
                }
                """;

        when(client.computeRoutes(any(), any())).thenReturn(objectMapper.readTree(json));

        RouteResult route = provider.calculateRoute(
                new GeoPoint(BigDecimal.valueOf(17), BigDecimal.valueOf(78)),
                new GeoPoint(BigDecimal.valueOf(16), BigDecimal.valueOf(77)));

        assertEquals(100, route.distanceKm().doubleValue(), 0.0001);
        assertEquals(60, route.durationMinutes());
        assertEquals(2, route.geometry().size());
    }
}
