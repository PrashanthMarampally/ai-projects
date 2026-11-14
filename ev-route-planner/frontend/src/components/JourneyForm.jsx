const PRESETS = [
  {
    label: 'Hyd \u2192 Blr',
    origin: { lat: 17.385, lng: 78.4867 },
    dest: { lat: 12.9716, lng: 77.5946 },
  },
  {
    label: 'Mumbai \u2192 Pune',
    origin: { lat: 19.076, lng: 72.8777 },
    dest: { lat: 18.5204, lng: 73.8567 },
  },
  {
    label: 'Delhi \u2192 Agra',
    origin: { lat: 28.6139, lng: 77.209 },
    dest: { lat: 27.1767, lng: 78.0081 },
  },
]

export default function JourneyForm({
  origin,
  destination,
  vehicles,
  vehicleId,
  currentSoc,
  minArrivalSoc,
  mapClickMode,
  loading,
  onVehicleChange,
  onCurrentSocChange,
  onMinArrivalSocChange,
  onSetMapClickMode,
  onPlan,
  onPreset,
}) {
  const canPlan = origin && destination && vehicleId && !loading

  return (
    <div className="form-panel">
      <div className="form-section">
        <h3>Route</h3>
        <div className="preset-row">
          {PRESETS.map((p) => (
            <button
              key={p.label}
              className="preset-btn"
              onClick={() => onPreset(p.origin, p.dest)}
            >
              {p.label}
            </button>
          ))}
        </div>

        <div className="point-row">
          <button
            className={`point-btn origin-btn${mapClickMode === 'origin' ? ' active' : ''}`}
            onClick={() =>
              onSetMapClickMode(mapClickMode === 'origin' ? null : 'origin')
            }
          >
            A Origin
          </button>
          <span className="coord-display">
            {origin
              ? `${origin.lat.toFixed(4)}, ${origin.lng.toFixed(4)}`
              : 'Click map'}
          </span>
        </div>

        <div className="point-row">
          <button
            className={`point-btn dest-btn${mapClickMode === 'destination' ? ' active' : ''}`}
            onClick={() =>
              onSetMapClickMode(
                mapClickMode === 'destination' ? null : 'destination',
              )
            }
          >
            B Destination
          </button>
          <span className="coord-display">
            {destination
              ? `${destination.lat.toFixed(4)}, ${destination.lng.toFixed(4)}`
              : 'Click map'}
          </span>
        </div>
      </div>

      <div className="form-section">
        <h3>Vehicle</h3>
        {vehicles.length === 0 ? (
          <p className="hint">No vehicles in database. Seed data first.</p>
        ) : (
          <select
            className="vehicle-select"
            value={vehicleId}
            onChange={(e) => onVehicleChange(e.target.value)}
          >
            {vehicles.map((v) => (
              <option key={v.id} value={v.id}>
                {v.name} ({v.rangeKm} km)
              </option>
            ))}
          </select>
        )}
      </div>

      <div className="form-section">
        <h3>Battery</h3>
        <div className="slider-label">
          <span>Current SOC</span>
          <span className="soc-value">{currentSoc}%</span>
        </div>
        <input
          type="range"
          min="1"
          max="100"
          value={currentSoc}
          onChange={(e) => onCurrentSocChange(Number(e.target.value))}
          className="soc-slider"
        />
        <div className="slider-label">
          <span>Min arrival SOC</span>
          <span className="soc-value">{minArrivalSoc}%</span>
        </div>
        <input
          type="range"
          min="0"
          max="50"
          value={minArrivalSoc}
          onChange={(e) => onMinArrivalSocChange(Number(e.target.value))}
          className="soc-slider"
        />
      </div>

      <button className="plan-btn" onClick={onPlan} disabled={!canPlan}>
        {loading ? 'Planning...' : 'Plan Journey'}
      </button>
    </div>
  )
}
