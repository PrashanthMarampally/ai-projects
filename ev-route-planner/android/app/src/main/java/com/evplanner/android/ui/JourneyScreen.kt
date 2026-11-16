package com.evplanner.android.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evplanner.android.api.JourneyOption
import com.evplanner.android.viewmodel.JourneyViewModel
import com.evplanner.android.viewmodel.UiState
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

private data class Preset(val label: String, val oLat: Double, val oLng: Double, val dLat: Double, val dLng: Double)

private val PRESETS = listOf(
    Preset("Hyd→Blr",   17.385, 78.4867, 12.9716, 77.5946),
    Preset("Mum→Pune",  19.076, 72.8777, 18.5204, 73.8567),
    Preset("Del→Agra",  28.6139, 77.209, 27.1767, 78.0081),
    Preset("Del→Jaipur",28.6139, 77.209, 26.9124, 75.7873),
    Preset("Blr→Chen",  12.9716, 77.5946, 13.0827, 80.2707)
)

@Composable
fun JourneyScreen(modifier: Modifier = Modifier, vm: JourneyViewModel = viewModel()) {
    val origin by vm.origin.collectAsState()
    val destination by vm.destination.collectAsState()
    val vehicles by vm.vehicles.collectAsState()
    val selectedVehicleId by vm.selectedVehicleId.collectAsState()
    val currentSoc by vm.currentSoc.collectAsState()
    val minArrivalSoc by vm.minArrivalSoc.collectAsState()
    val pickMode by vm.mapPickMode.collectAsState()
    val uiState by vm.uiState.collectAsState()
    val selectedOption by vm.selectedOption.collectAsState()
    val context = LocalContext.current

    Column(modifier.fillMaxSize()) {
        // Map (top half)
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val mapView = rememberOsmMapView(context, vm, origin, destination, selectedOption)
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )
            if (pickMode != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xCC1565C0))
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tap map to set ${if (pickMode == "origin") "origin" else "destination"}",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Controls (bottom half)
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            // Presets
            item {
                Spacer(Modifier.height(8.dp))
                Text("Presets", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(PRESETS) { p ->
                        Button(
                            onClick = { vm.applyPreset(p.oLat, p.oLng, p.dLat, p.dLng) },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) { Text(p.label, fontSize = 11.sp) }
                    }
                }
            }

            // Origin / Destination
            item {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PickButton(
                        label = "A Origin",
                        active = pickMode == "origin",
                        coord = origin?.let { "%.4f, %.4f".format(it.latitude, it.longitude) },
                        modifier = Modifier.weight(1f)
                    ) { vm.setMapPickMode(if (pickMode == "origin") null else "origin") }
                    PickButton(
                        label = "B Dest",
                        active = pickMode == "destination",
                        coord = destination?.let { "%.4f, %.4f".format(it.latitude, it.longitude) },
                        modifier = Modifier.weight(1f)
                    ) { vm.setMapPickMode(if (pickMode == "destination") null else "destination") }
                }
            }

            // Vehicle
            item {
                Spacer(Modifier.height(8.dp))
                Text("Vehicle", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(vehicles) { v ->
                        val selected = v.id == selectedVehicleId
                        Button(
                            onClick = { vm.setVehicle(v.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selected) Color.White
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) { Text("${v.name} (${v.rangeKm.toInt()}km)", fontSize = 11.sp) }
                    }
                }
            }

            // SOC sliders
            item {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Current SOC", fontSize = 12.sp)
                    Text("$currentSoc%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = currentSoc.toFloat(),
                    onValueChange = { vm.setCurrentSoc(it.toInt()) },
                    valueRange = 1f..100f,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Min arrival SOC", fontSize = 12.sp)
                    Text("$minArrivalSoc%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = minArrivalSoc.toFloat(),
                    onValueChange = { vm.setMinArrivalSoc(it.toInt()) },
                    valueRange = 0f..50f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Plan button
            item {
                val canPlan = origin != null && destination != null && selectedVehicleId != null
                        && uiState !is UiState.Loading
                Button(
                    onClick = { vm.planJourney() },
                    enabled = canPlan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState is UiState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.width(20.dp).height(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (uiState is UiState.Loading) "Planning..." else "Plan Journey")
                }
            }

            // Results
            when (val state = uiState) {
                is UiState.Error -> item {
                    Spacer(Modifier.height(8.dp))
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                is UiState.Success -> itemsIndexed(state.options) { index, opt ->
                    Spacer(Modifier.height(8.dp))
                    JourneyOptionCard(
                        index = index + 1,
                        option = opt,
                        selected = opt == selectedOption,
                        onClick = { vm.selectOption(if (opt == selectedOption) null else opt) }
                    )
                }
                else -> {}
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PickButton(
    label: String, active: Boolean, coord: String?, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Column(modifier.clickable { onClick() }) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (active) Color(0xFF1565C0) else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(6.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.fillMaxWidth().height(32.dp)
        ) { Text(label, fontSize = 11.sp) }
        Text(
            text = coord ?: "Tap to set",
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun JourneyOptionCard(index: Int, option: JourneyOption, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFE3F2FD) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(if (selected) 4.dp else 1.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Option $index", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "Score: ${option.overallScore.toInt()}",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1565C0),
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${option.distanceKm.toInt()} km · ${option.estimatedDurationMinutes} min · " +
                        "${option.chargingStops.size} stop${if (option.chargingStops.size != 1) "s" else ""}",
                fontSize = 12.sp,
                color = Color.DarkGray
            )

            // Score breakdown pills
            option.scoreBreakdown?.let { sb ->
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ScorePill("Time", sb.timeEfficiency)
                    ScorePill("Buffer", sb.arrivalBuffer)
                    ScorePill("Charging", sb.chargingExperience)
                    ScorePill("Safety", sb.safety)
                }
            }

            // Charging stops
            if (selected && option.chargingStops.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                option.chargingStops.forEach { stop ->
                    Text(
                        "⚡ ${stop.stationName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "   ${stop.chargingPowerKw.toInt()} kW · " +
                                "${stop.arrivalSocPercent.toInt()}% → ${stop.targetSocPercent.toInt()}% · " +
                                "${stop.estimatedChargingMinutes} min",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}

@Composable
private fun ScorePill(label: String, value: Double) {
    Box(
        Modifier
            .background(Color(0xFF1565C0).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text("$label ${value.toInt()}", fontSize = 10.sp, color = Color(0xFF1565C0))
    }
}

@Composable
fun rememberOsmMapView(
    context: Context,
    vm: JourneyViewModel,
    origin: com.evplanner.android.api.GeoPoint?,
    destination: com.evplanner.android.api.GeoPoint?,
    selectedOption: JourneyOption?
): MapView {
    Configuration.getInstance().userAgentValue = "EVRoutePlanner/1.0"

    val mapView = remember(context) {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(5.0)
            controller.setCenter(GeoPoint(20.5937, 78.9629))
        }
    }

    // Update overlays whenever state changes
    androidx.compose.runtime.LaunchedEffect(origin, destination, selectedOption) {
        mapView.overlays.clear()

        // Tap overlay (must be first so it doesn't block other overlays)
        val tapOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                vm.onMapTap(p.latitude, p.longitude)
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean = false
        })
        mapView.overlays.add(tapOverlay)

        // Route polyline
        selectedOption?.let { opt ->
            if (opt.routeGeometry.size >= 2) {
                val line = Polyline().apply {
                    outlinePaint.color = android.graphics.Color.parseColor("#1565C0")
                    outlinePaint.strokeWidth = 8f
                    setPoints(opt.routeGeometry.map { GeoPoint(it.latitude, it.longitude) })
                }
                mapView.overlays.add(line)
            }
            opt.chargingStops.forEach { stop ->
                Marker(mapView).apply {
                    position = GeoPoint(stop.latitude, stop.longitude)
                    title = stop.stationName
                    snippet = "${stop.chargingPowerKw.toInt()} kW · ${stop.estimatedChargingMinutes} min"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    mapView.overlays.add(this)
                }
            }
        }

        // Origin marker
        origin?.let {
            Marker(mapView).apply {
                position = GeoPoint(it.latitude, it.longitude)
                title = "Origin"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                mapView.overlays.add(this)
            }
        }

        // Destination marker
        destination?.let {
            Marker(mapView).apply {
                position = GeoPoint(it.latitude, it.longitude)
                title = "Destination"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                mapView.overlays.add(this)
            }
        }

        // Auto-center
        if (origin != null && destination != null) {
            val centerLat = (origin.latitude + destination.latitude) / 2
            val centerLng = (origin.longitude + destination.longitude) / 2
            mapView.controller.animateTo(GeoPoint(centerLat, centerLng))
            mapView.controller.setZoom(7.0)
        }

        mapView.invalidate()
    }

    return mapView
}
