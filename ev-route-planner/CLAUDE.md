# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

India-first EV journey intelligence engine. Given an origin, destination, vehicle, and current battery state, it produces optimized journey plans with multi-stop charging recommendations. The project uses a validation-first approach — the intelligence engine is benchmarked against existing solutions before investing in external providers or a mobile app.

## Commands

All commands run from `backend/`.

```bash
# Start the database (required before running the app)
cd infrastructure/docker && docker compose up -d

# Build
./mvnw clean package

# Run
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=MultiStopChargingPlannerTest

# Run a single test method
./mvnw test -Dtest=MultiStopChargingPlannerTest#someMethod
```

## Infrastructure

- **Database**: PostgreSQL 17 with PostGIS extension (`postgis/postgis:17-3.5`)
  - Host: `localhost:5432`, DB/user/password: all `evplanner`
- **Flyway**: migrations in `src/main/resources/db/migration/`
- **Server**: runs on port 8080

## Routing Providers

The routing layer is provider-abstracted. Two implementations exist:

- **`fake`** (default): no network calls, used in tests and local dev
- **`google`**: calls Google Routes API

To use Google:
```bash
export EVPLANNER_ROUTING_PROVIDER=google
export GOOGLE_MAPS_API_KEY='your-key'
```

## Architecture

The main entry point is `POST /api/journeys/plan` (`JourneyController`), which delegates entirely to `JourneyIntelligenceEngine`.

**Request flow** (`DefaultJourneyIntelligenceEngine.plan`):
1. Look up the `Vehicle` from the DB
2. Call `RoutingProvider` to get one or more `RouteResult`s (polyline + distance + duration)
3. For each route:
   - `RouteStationFinder.findNearRoute` — queries PostGIS for charging stations within a configurable corridor (`evplanner.routing.station-corridor-radius-km`, default 50 km)
   - `EnergyConsumptionModel.calculate` — determines if charging is needed
   - `ChargingStrategy.evaluate` — produces a `ChargingDecision`
   - If charging is required: `MultiStopChargingPlanner.plan` — bounded recursive search (max 4 stops) over candidate stations, minimizing total charging time then detour
4. Each route becomes a `JourneyOption` with a score; options are sorted and wrapped in a `JourneyPlan`

**Package responsibilities:**

| Package | Role |
|---|---|
| `api` | REST controller and exception handler |
| `intelligence` | Orchestration: `JourneyIntelligenceEngine` / `DefaultJourneyIntelligenceEngine` |
| `charging` | Multi-stop planner, eligibility, charging strategy, candidate evaluation |
| `routing` | `RoutingProvider` interface, fake and Google implementations, route geometry/corridor utilities |
| `energy` | `EnergyConsumptionModel` — SOC arithmetic given vehicle, distance, and reserve |
| `station` | JPA entities and repositories for `ChargingStation`/`ChargingConnector`, `RouteStationFinder` |
| `vehicle` | `Vehicle` JPA entity and repository |
| `journey` | Immutable domain records: `JourneyRequest`, `JourneyPlan`, `JourneyOption`, `ChargingStopPlan` |
| `provider` | Thin provider interfaces (`RoutingProvider`, `StationProvider`) |

**Scoring** (temporary): score = `max(0, 1000 - totalMinutes)` where `totalMinutes` = drive time + estimated charging time. Marked for replacement with a full scoring model.

**Key config property**: `evplanner.routing.station-corridor-radius-km` (default 50) controls how far off-route a charging station can be to be considered a candidate.
