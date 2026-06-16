export const eventFilters = [
  { label: 'All', value: '' },
  { label: 'Popular', value: 'popular' },
  { label: 'Upcoming', value: 'upcoming' },
  { label: 'Nearby', value: 'nearby' },
]

export function getEventType(typeParam) {
  return eventFilters.some((filter) => filter.value === typeParam) ? typeParam : ''
}

export function buildEventTypeParams(searchParams, type) {
  const nextParams = new URLSearchParams(searchParams)
  if (type) {
    nextParams.set('type', type)
  } else {
    nextParams.delete('type')
  }
  return nextParams
}
