export async function getVehicles() {
  const res = await fetch('/api/vehicles')
  if (!res.ok) throw new Error('Failed to load vehicles')
  return res.json()
}

export async function planJourney({ origin, destination, vehicleId, currentSoc, minArrivalSoc }) {
  const res = await fetch('/api/journeys/plan', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      origin: { latitude: origin.lat, longitude: origin.lng },
      destination: { latitude: destination.lat, longitude: destination.lng },
      vehicleId: Number(vehicleId),
      currentSocPercent: currentSoc,
      minimumArrivalSocPercent: minArrivalSoc,
    }),
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.detail || err.title || `Error ${res.status}`)
  }
  return res.json()
}
