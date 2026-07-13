import { useEffect, useState } from 'react'

export function useBrowserGeolocation() {
  const [coords, setCoords] = useState(null)
  const [locationStatus, setLocationStatus] = useState('idle')

  useEffect(() => {
    if (!navigator.geolocation) {
      setLocationStatus('unsupported')
      return
    }
    setLocationStatus('loading')
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setCoords({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
        })
        setLocationStatus('granted')
      },
      () => setLocationStatus('denied'),
      { enableHighAccuracy: false, timeout: 8000, maximumAge: 300000 },
    )
  }, [])

  return { coords, locationStatus }
}
