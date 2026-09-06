package com.evplanner.routing.google;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "evplanner.routing.google")
public record GoogleRoutingProperties(
        String apiKey,
        String baseUrl,
        String fieldMask) {

    public GoogleRoutingProperties {
        baseUrl = baseUrl == null || baseUrl.isBlank()
                ? "https://routes.googleapis.com"
                : baseUrl;
        fieldMask = fieldMask == null || fieldMask.isBlank()
                ? "routes.distanceMeters,routes.duration,routes.polyline.geoJsonLinestring"
                : fieldMask;
    }
}
