package com.evplanner.routing.google;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "evplanner.routing.provider", havingValue = "google")
public class GoogleRoutesClient {

    private static final String COMPUTE_ROUTES_PATH = "/directions/v2:computeRoutes";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String fieldMask;

    public GoogleRoutesClient(
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder,
            GoogleRoutingProperties properties) {
        this.objectMapper = objectMapper;
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException("GOOGLE_MAPS_API_KEY must be configured when Google routing is enabled");
        }
        this.apiKey = properties.apiKey();
        this.fieldMask = properties.fieldMask();
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .build();
    }

    public JsonNode computeRoutes(
            com.evplanner.journey.GeoPoint origin,
            com.evplanner.journey.GeoPoint destination) {

        Map<String, Object> request = Map.of(
                "origin", waypoint(origin),
                "destination", waypoint(destination),
                "travelMode", "DRIVE",
                "routingPreference", "TRAFFIC_AWARE",
                "computeAlternativeRoutes", true,
                "polylineQuality", "HIGH_QUALITY",
                "polylineEncoding", "GEO_JSON_LINESTRING",
                "routeModifiers", Map.of(
                        "avoidTolls", false,
                        "avoidHighways", false,
                        "avoidFerries", false),
                "languageCode", "en-IN",
                "units", "METRIC");

        String response = restClient.post()
                .uri(COMPUTE_ROUTES_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", fieldMask)
                .body(request)
                .retrieve()
                .body(String.class);

        try {
            return objectMapper.readTree(response);
        } catch (Exception e) {
            throw new GoogleRoutingException("Unable to parse Google Routes API response", e);
        }
    }

    private Map<String, Object> waypoint(com.evplanner.journey.GeoPoint point) {
        return Map.of(
                "location", Map.of(
                        "latLng", Map.of(
                                "latitude", point.latitude(),
                                "longitude", point.longitude())));
    }
}
