import { useState, useEffect } from 'react'
import JourneyForm from './components/JourneyForm'
import JourneyMap from './components/JourneyMap'
import JourneyResults from './components/JourneyResults'
import { getVehicles, planJourney } from './api/journeyApi'

export default function App() {
  const [origin, setOrigin] = useState(null)
  const [destination, setDestination] = useState(null)
  const [vehicles, setVehicles] = useState([])
  const [vehicleId, setVehicleId] = useState('')
  const [currentSoc, setCurrentSoc] = useState(80)
  const [minArrivalSoc, setMinArrivalSoc] = useState(15)
  const [plan, setPlan] = useState(null)
  const [selectedIdx, setSelectedIdx] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [mapClickMode, setMapClickMode] = useState(null) // 'origin' | 'destination' | null

  useEffect(() => {
    getVehicles()
      .then(v => {
        setVehicles(v)
        if (v.length > 0) setVehicleId(v[0].id)
      })
      .catch(() => setError('Could not load vehicles. Is the backend running?'))
  }, [])

  function handleMapClick({ lat, lng }) {
    if (mapClickMode === 'origin') {
      setOrigin({ lat, lng })
      setPlan(null)
      setMapClickMode('destination')
    } else if (mapClickMode === 'destination') {
      setDestination({ lat, lng })
      setPlan(null)
      setMapClickMode(null)
    }
  }

  function handleOriginDrag(pos) {
    setOrigin(pos)
    setPlan(null)
  }

  function handleDestinationDrag(pos) {
    setDestination(pos)
    setPlan(null)
  }

  function handlePreset(o, d) {
    setOrigin(o)
    setDestination(d)
    setPlan(null)
    setError(null)
  }

  async function handlePlan() {
    if (!origin || !destination || !vehicleId) return
    setLoading(true)
    setError(null)
    setPlan(null)
    try {
      const result = await planJourney({ origin, destination, vehicleId, currentSoc, minArrivalSoc })
      setPlan(result)
      setSelectedIdx(0)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  const selectedOption = plan?.options?.[selectedIdx] ?? null

  return (
    <div className="app">
      <header className="app-header">
        <span className="app-logo">&#9889;</span>
        <h1>EV Route Planner</h1>
        {mapClickMode && (
          <span className="click-hint">
            Click on map to set {mapClickMode}
          </span>
        )}
      </header>
      <div className="app-body">
        <aside className="sidebar">
          <JourneyForm
            origin={origin}
            destination={destination}
            vehicles={vehicles}
            vehicleId={vehicleId}
            currentSoc={currentSoc}
            minArrivalSoc={minArrivalSoc}
            mapClickMode={mapClickMode}
            loading={loading}
            onVehicleChange={setVehicleId}
            onCurrentSocChange={setCurrentSoc}
            onMinArrivalSocChange={setMinArrivalSoc}
            onSetMapClickMode={setMapClickMode}
            onPlan={handlePlan}
            onPreset={handlePreset}
          />
          {error && <div className="error-banner">{error}</div>}
          {plan && (
            <JourneyResults
              plan={plan}
              selectedIdx={selectedIdx}
              onSelect={setSelectedIdx}
            />
          )}
        </aside>
        <main className="map-panel">
          <JourneyMap
            origin={origin}
            destination={destination}
            selectedOption={selectedOption}
            mapClickMode={mapClickMode}
            onMapClick={handleMapClick}
            onOriginDrag={handleOriginDrag}
            onDestinationDrag={handleDestinationDrag}
          />
        </main>
      </div>
    </div>
  )
}
