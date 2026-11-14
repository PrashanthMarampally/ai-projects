function fmtDuration(minutes) {
  if (!minutes && minutes !== 0) return '--'
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return h > 0 ? `${h}h ${m}m` : `${m}m`
}

export default function JourneyResults({ plan, selectedIdx, onSelect }) {
  const { options, recommendedOption } = plan

  return (
    <div className="results-panel">
      <h3>Journey Options</h3>
      {options.map((opt, i) => {
        const isRec = opt === recommendedOption
        const isSelected = i === selectedIdx
        const stops = opt.chargingStops?.length ?? 0
        const chargingTime = opt.chargingStops?.reduce(
          (sum, s) => sum + (s.estimatedChargingMinutes ?? 0),
          0,
        ) ?? 0
        const totalTime = (opt.estimatedDurationMinutes ?? 0) + chargingTime

        return (
          <div
            key={i}
            className={`option-card${isSelected ? ' selected' : ''}`}
            onClick={() => onSelect(i)}
          >
            <div className="option-header">
              <span className="option-label">Option {i + 1}</span>
              {isRec && <span className="rec-badge">Recommended</span>}
              <span className="option-score">Score: {opt.overallScore}</span>
            </div>

            <div className="option-stats">
              <span>{opt.distanceKm} km</span>
              <span>{fmtDuration(opt.estimatedDurationMinutes)} drive</span>
              <span>{stops} stop{stops !== 1 ? 's' : ''}</span>
              <span>{fmtDuration(totalTime)} total</span>
            </div>

            <div className="option-soc">
              Arrival SOC: <strong>{opt.minimumSocPercent}%</strong>
            </div>

            {stops > 0 && (
              <div className="stop-list">
                {opt.chargingStops.map((s, j) => (
                  <div key={j} className="stop-item">
                    <span>{s.stationName ?? `Stop ${j + 1}`}</span>
                    <span>{s.distanceFromOriginKm} km</span>
                    <span>
                      +{s.estimatedChargingMinutes} min @ {s.chargingPowerKw} kW
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        )
      })}
    </div>
  )
}
