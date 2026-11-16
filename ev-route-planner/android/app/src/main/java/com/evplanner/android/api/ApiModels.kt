package com.evplanner.android.api

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

data class JourneyRequest(
    val origin: GeoPoint,
    val destination: GeoPoint,
    val vehicleId: Long,
    val currentSocPercent: Double,
    val minimumArrivalSocPercent: Double
)

data class ScoreBreakdown(
    val timeEfficiency: Double,
    val arrivalBuffer: Double,
    val chargingExperience: Double,
    val safety: Double,
    val overall: Double
)

data class ChargingStop(
    val stationId: Long,
    val stationName: String,
    val distanceFromOriginKm: Double,
    val distanceToDestinationKm: Double,
    val arrivalSocPercent: Double,
    val targetSocPercent: Double,
    val energyToAddKwh: Double,
    val chargingPowerKw: Double,
    val estimatedChargingMinutes: Int,
    val latitude: Double,
    val longitude: Double
)

data class JourneyOption(
    val distanceKm: Double,
    val estimatedDurationMinutes: Int,
    val chargingStops: List<ChargingStop>,
    val minimumSocPercent: Double,
    val overallScore: Double,
    val routeGeometry: List<GeoPoint>,
    val scoreBreakdown: ScoreBreakdown?
)

data class JourneyPlan(
    val options: List<JourneyOption>
)

data class Vehicle(
    val id: Long,
    val name: String,
    val rangeKm: Double,
    val batteryCapacityKwh: Double
)
