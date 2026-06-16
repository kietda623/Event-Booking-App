import { toInputDateTime } from '../../utils/format'

export const emptyEventForm = {
  title: '',
  description: '',
  startTime: '',
  endTime: '',
  location: '',
  price: '',
  totalTickets: '',
  imageUrl: '',
  latitude: '',
  longitude: '',
}

export const emptyTierForm = {
  name: '',
  price: '',
  totalQuantity: '',
  description: '',
}

export function eventToForm(event) {
  return {
    title: event.title || '',
    description: event.description || '',
    startTime: toInputDateTime(event.eventDate),
    endTime: '',
    location: event.location || '',
    price: event.price ?? '',
    totalTickets: event.availableTickets ?? '',
    imageUrl: event.imageUrl || '',
    latitude: event.latitude ?? '',
    longitude: event.longitude ?? '',
  }
}

export function tierToForm(tier) {
  return {
    name: tier.name || '',
    price: tier.price ?? '',
    totalQuantity: tier.totalQuantity ?? '',
    description: tier.description || '',
  }
}

export function buildEventPayload(form) {
  return {
    title: form.title,
    description: form.description,
    eventDate: form.startTime,
    location: form.location,
    price: form.price === '' ? null : Number(form.price),
    totalTickets: form.totalTickets === '' ? null : Number(form.totalTickets),
    imageUrl: form.imageUrl || null,
    latitude: form.latitude === '' ? null : Number(form.latitude),
    longitude: form.longitude === '' ? null : Number(form.longitude),
  }
}

export function buildTierPayload(tierForm) {
  return {
    name: tierForm.name,
    price: tierForm.price === '' ? 0 : Number(tierForm.price),
    totalQuantity: tierForm.totalQuantity === '' ? 0 : Number(tierForm.totalQuantity),
    description: tierForm.description || null,
  }
}

export function buildInitialTierPayload(isEditing, tierForm) {
  return !isEditing && tierForm.name.trim() ? buildTierPayload(tierForm) : null
}
