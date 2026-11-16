package com.evplanner.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evplanner.android.api.EvPlannerApi
import com.evplanner.android.api.GeoPoint
import com.evplanner.android.api.JourneyOption
import com.evplanner.android.api.JourneyRequest
import com.evplanner.android.api.Vehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UiState {
    data object Idle : UiState()
    data object Loading : UiState()
    data class Success(val options: List<JourneyOption>) : UiState()
    data class Error(val message: String) : UiState()
}

data class MapPin(val lat: Double, val lng: Double, val isOrigin: Boolean)

class JourneyViewModel : ViewModel() {

    private val api = EvPlannerApi.create()

    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()

    private val _selectedVehicleId = MutableStateFlow<Long?>(null)
    val selectedVehicleId: StateFlow<Long?> = _selectedVehicleId.asStateFlow()

    private val _origin = MutableStateFlow<GeoPoint?>(null)
    val origin: StateFlow<GeoPoint?> = _origin.asStateFlow()

    private val _destination = MutableStateFlow<GeoPoint?>(null)
    val destination: StateFlow<GeoPoint?> = _destination.asStateFlow()

    private val _currentSoc = MutableStateFlow(80)
    val currentSoc: StateFlow<Int> = _currentSoc.asStateFlow()

    private val _minArrivalSoc = MutableStateFlow(10)
    val minArrivalSoc: StateFlow<Int> = _minArrivalSoc.asStateFlow()

    // null = not picking; "origin" or "destination"
    private val _mapPickMode = MutableStateFlow<String?>(null)
    val mapPickMode: StateFlow<String?> = _mapPickMode.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _selectedOption = MutableStateFlow<JourneyOption?>(null)
    val selectedOption: StateFlow<JourneyOption?> = _selectedOption.asStateFlow()

    init {
        loadVehicles()
    }

    private fun loadVehicles() {
        viewModelScope.launch {
            runCatching { api.getVehicles() }
                .onSuccess { list ->
                    _vehicles.value = list
                    if (_selectedVehicleId.value == null) {
                        _selectedVehicleId.value = list.firstOrNull()?.id
                    }
                }
        }
    }

    fun setVehicle(id: Long) { _selectedVehicleId.value = id }
    fun setCurrentSoc(v: Int) { _currentSoc.value = v }
    fun setMinArrivalSoc(v: Int) { _minArrivalSoc.value = v }
    fun setMapPickMode(mode: String?) { _mapPickMode.value = mode }
    fun selectOption(opt: JourneyOption?) { _selectedOption.value = opt }

    fun onMapTap(lat: Double, lng: Double) {
        when (_mapPickMode.value) {
            "origin" -> {
                _origin.value = GeoPoint(lat, lng)
                _mapPickMode.value = null
            }
            "destination" -> {
                _destination.value = GeoPoint(lat, lng)
                _mapPickMode.value = null
            }
        }
    }

    fun applyPreset(originLat: Double, originLng: Double, destLat: Double, destLng: Double) {
        _origin.value = GeoPoint(originLat, originLng)
        _destination.value = GeoPoint(destLat, destLng)
        _mapPickMode.value = null
        _selectedOption.value = null
        _uiState.value = UiState.Idle
    }

    fun planJourney() {
        val o = _origin.value ?: return
        val d = _destination.value ?: return
        val vid = _selectedVehicleId.value ?: return

        _uiState.value = UiState.Loading
        _selectedOption.value = null
        viewModelScope.launch {
            runCatching {
                api.planJourney(
                    JourneyRequest(
                        origin = o,
                        destination = d,
                        vehicleId = vid,
                        currentSocPercent = _currentSoc.value.toDouble(),
                        minimumArrivalSocPercent = _minArrivalSoc.value.toDouble()
                    )
                )
            }.onSuccess { plan ->
                _uiState.value = if (plan.options.isEmpty()) {
                    UiState.Error("No journey options found")
                } else {
                    UiState.Success(plan.options)
                }
            }.onFailure { e ->
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
