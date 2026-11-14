import { useEffect, useMemo } from 'react'
import {
  MapContainer,
  TileLayer,
  Marker,
  Popup,
  Polyline,
  useMapEvents,
  useMap,
} from 'react-leaflet'
import L from 'leaflet'

function makeIcon(bg, label) {
  return L.divIcon({
    html: `<div style="
      background:${bg};
      width:32px;height:32px;border-radius:50%;
      border:3px solid white;
      box-shadow:0 2px 8px rgba(0,0,0,0.35);
      display:flex;align-items:center;justify-content:center;
      font-size:13px;font-weight:700;color:white;
    ">${label}</div>`,
    className: '',
    iconSize: [32, 32],
    iconAnchor: [16, 16],
    popupAnchor: [0, -20],
  })
}

const originIcon = makeIcon('#16a34a', 'A')
const destIcon = makeIcon('#dc2626', 'B')
const stopIcon = makeIcon('#d97706', 'C')

function MapClickHandler({ mode, onMapClick }) {
  useMapEvents({
    click(e) {
      if (mode) onMapClick({ lat: e.latlng.lat, lng: e.latlng.lng })
    },
  })
  return null
}

function FitBounds({ coords }) {
  const map = useMap()
  useEffect(() => {
    if (coords && coords.length >= 2) {
      map.fitBounds(L.latLngBounds(coords), { padding: [50, 50] })
    }
  }, [coords, map])
  return null
}

export default function JourneyMap({
  origin,
  destination,
  selectedOption,
  mapClickMode,
  onMapClick,
  onOriginDrag,
  onDestinationDrag,
}) {
  const routeCoords = useMemo(() => {
    if (!selectedOption?.routeGeometry) return null
    return selectedOption.routeGeometry.map((p) => [p.latitude, p.longitude])
  }, [selectedOption])

  // Fit to route when a plan is received; fit to markers when only origin+destination are set
  const fitCoords = useMemo(() => {
    if (routeCoords && routeCoords.length >= 2) return routeCoords
    const pts = []
    if (origin) pts.push([origin.lat, origin.lng])
    if (destination) pts.push([destination.lat, destination.lng])
    return pts.length >= 2 ? pts : null
  }, [routeCoords, origin, destination])

  return (
    <MapContainer
      center={[20.5937, 78.9629]}
      zoom={5}
      style={{ width: '100%', height: '100%' }}
      className={mapClickMode ? 'crosshair-cursor' : ''}
    >
      <TileLayer
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
      />

      <MapClickHandler mode={mapClickMode} onMapClick={onMapClick} />
      {fitCoords && <FitBounds coords={fitCoords} />}

      {routeCoords && (
        <Polyline positions={routeCoords} color="#2563eb" weight={4} opacity={0.8} />
      )}

      {origin && (
        <Marker
          position={[origin.lat, origin.lng]}
          icon={originIcon}
          draggable
          eventHandlers={{
            dragend(e) {
              const { lat, lng } = e.target.getLatLng()
              onOriginDrag({ lat, lng })
            },
          }}
        >
          <Popup>
            <strong>Origin</strong>
            <br />
            {origin.lat.toFixed(5)}, {origin.lng.toFixed(5)}
          </Popup>
        </Marker>
      )}

      {destination && (
        <Marker
          position={[destination.lat, destination.lng]}
          icon={destIcon}
          draggable
          eventHandlers={{
            dragend(e) {
              const { lat, lng } = e.target.getLatLng()
              onDestinationDrag({ lat, lng })
            },
          }}
        >
          <Popup>
            <strong>Destination</strong>
            <br />
            {destination.lat.toFixed(5)}, {destination.lng.toFixed(5)}
          </Popup>
        </Marker>
      )}

      {selectedOption?.chargingStops?.map((stop, i) =>
        stop.latitude != null && stop.longitude != null ? (
          <Marker
            key={stop.stationId ?? i}
            position={[stop.latitude, stop.longitude]}
            icon={stopIcon}
          >
            <Popup>
              <strong>{stop.stationName ?? `Stop ${i + 1}`}</strong>
              <br />
              Arrive: {stop.arrivalSocPercent}%
              <br />
              Charge to: {stop.targetSocPercent}%
              <br />
              Time: ~{stop.estimatedChargingMinutes} min
              <br />
              Power: {stop.chargingPowerKw} kW
            </Popup>
          </Marker>
        ) : null,
      )}
    </MapContainer>
  )
}
