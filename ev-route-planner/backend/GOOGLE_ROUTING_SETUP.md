# Google Routes API setup

The project now supports Google Routes API as a routing provider while retaining the fake provider for tests/local development.

## Configuration

Set:

```bash
export EVPLANNER_ROUTING_PROVIDER=google
export GOOGLE_MAPS_API_KEY='your-key'
```

Or configure the equivalent environment variables in your IDE/run configuration.

The source does not contain the API key.

## What the provider requests

- DRIVE travel mode
- traffic-aware routing
- alternative routes
- high-quality route geometry
- GeoJSON LineString geometry
- metric units

The Google response is mapped to the application's `RouteResult`, so the rest of the EV intelligence layer remains provider-independent.

## Default behavior

If `EVPLANNER_ROUTING_PROVIDER` is not set, the fake provider remains active. This keeps the normal unit-test suite independent of Google's network/API key.
