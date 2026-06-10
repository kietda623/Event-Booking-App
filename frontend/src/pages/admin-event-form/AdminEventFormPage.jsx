import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { eventsApi } from '../../api/events'
import { FieldError, FormError } from '../../components/FieldErrors'
import { EmptyState, ErrorState, LoadingState } from '../../components/StateViews'
import { toInputDateTime } from '../../utils/format'

const emptyForm = {
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

const emptyTierForm = {
  name: '',
  price: '',
  totalQuantity: '',
  description: '',
}

export function AdminEventFormPage() {
  const { id } = useParams()
  const isEditing = Boolean(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [form, setForm] = useState(emptyForm)
  const [tierForm, setTierForm] = useState(emptyTierForm)
  const [editingTierId, setEditingTierId] = useState(null)
  const [apiError, setApiError] = useState(null)
  const [tierApiError, setTierApiError] = useState(null)

  const eventQuery = useQuery({
    queryKey: ['admin-event', id],
    queryFn: () => eventsApi.get(id),
    enabled: isEditing,
  })

  useEffect(() => {
    if (eventQuery.data) {
      setForm({
        title: eventQuery.data.title || '',
        description: eventQuery.data.description || '',
        startTime: toInputDateTime(eventQuery.data.eventDate),
        endTime: '',
        location: eventQuery.data.location || '',
        price: eventQuery.data.price ?? '',
        totalTickets: eventQuery.data.availableTickets ?? '',
        imageUrl: eventQuery.data.imageUrl || '',
        latitude: eventQuery.data.latitude ?? '',
        longitude: eventQuery.data.longitude ?? '',
      })
    }
  }, [eventQuery.data])

  const mutation = useMutation({
    mutationFn: async ({ eventPayload, initialTierPayload }) => {
      if (isEditing) {
        return eventsApi.update(id, eventPayload)
      }
      const created = await eventsApi.create(eventPayload)
      if (initialTierPayload?.name) {
        await eventsApi.createTier(created.id, initialTierPayload)
      }
      return created
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-events'] })
      queryClient.invalidateQueries({ queryKey: ['events'] })
      navigate('/admin/events')
    },
    onError: setApiError,
  })
  const tierMutation = useMutation({
    mutationFn: (payload) =>
      editingTierId ? eventsApi.updateTier(id, editingTierId, payload) : eventsApi.createTier(id, payload),
    onSuccess: () => {
      setTierForm(emptyTierForm)
      setEditingTierId(null)
      setTierApiError(null)
      queryClient.invalidateQueries({ queryKey: ['admin-event', id] })
      queryClient.invalidateQueries({ queryKey: ['event', id] })
      queryClient.invalidateQueries({ queryKey: ['events'] })
    },
    onError: setTierApiError,
  })
  const deleteTierMutation = useMutation({
    mutationFn: (tierId) => eventsApi.removeTier(id, tierId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-event', id] })
      queryClient.invalidateQueries({ queryKey: ['event', id] })
      queryClient.invalidateQueries({ queryKey: ['events'] })
    },
    onError: setTierApiError,
  })

  if (isEditing && eventQuery.isLoading) return <LoadingState label="Loading event form..." />
  if (isEditing && eventQuery.isError && eventQuery.error?.code === 'EVENT_NOT_FOUND') {
    return <EmptyState title="Event not found" text="The event may have been deleted." />
  }
  if (isEditing && eventQuery.isError) return <ErrorState error={eventQuery.error} title="Could not load event" />

  const setField = (field, value) => setForm((current) => ({ ...current, [field]: value }))
  const setTierField = (field, value) => setTierForm((current) => ({ ...current, [field]: value }))

  const submitTier = () => {
    setTierApiError(null)
    tierMutation.mutate({
      name: tierForm.name,
      price: tierForm.price === '' ? 0 : Number(tierForm.price),
      totalQuantity: tierForm.totalQuantity === '' ? 0 : Number(tierForm.totalQuantity),
      description: tierForm.description || null,
    })
  }

  const editTier = (tier) => {
    setEditingTierId(tier.id)
    setTierForm({
      name: tier.name || '',
      price: tier.price ?? '',
      totalQuantity: tier.totalQuantity ?? '',
      description: tier.description || '',
    })
  }

  const onSubmit = (event) => {
    event.preventDefault()
    setApiError(null)
    const eventPayload = {
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
    const initialTierPayload =
      !isEditing && tierForm.name.trim()
        ? {
            name: tierForm.name,
            price: tierForm.price === '' ? 0 : Number(tierForm.price),
            totalQuantity: tierForm.totalQuantity === '' ? 0 : Number(tierForm.totalQuantity),
            description: tierForm.description || null,
          }
        : null
    mutation.mutate({ eventPayload, initialTierPayload })
  }

  return (
    <section className="page-stack">
      <div className="page-header">
        <div>
          <p className="eyebrow">Admin</p>
          <h1>{isEditing ? 'Edit event' : 'New event'}</h1>
        </div>
        <Link className="button ghost" to="/admin/events">
          Back
        </Link>
      </div>
      <form className="form-card wide-form" onSubmit={onSubmit}>
        <FormError error={apiError} />
        <div className="form-grid">
          <label>
            Title
            <input value={form.title} onChange={(event) => setField('title', event.target.value)} required />
            <FieldError error={apiError} field="title" />
          </label>
          <label>
            Location
            <input value={form.location} onChange={(event) => setField('location', event.target.value)} />
          </label>
          <label>
            Start time
            <input
              type="datetime-local"
              value={form.startTime}
              onChange={(event) => setField('startTime', event.target.value)}
              required
            />
            <FieldError error={apiError} field="eventDate" />
          </label>
          <label>
            End time
            <input type="datetime-local" value={form.endTime} onChange={(event) => setField('endTime', event.target.value)} />
          </label>
          <label>
            Price
            <input type="number" min="0" step="0.01" value={form.price} onChange={(event) => setField('price', event.target.value)} />
          </label>
          <label>
            Total tickets
            <input
              type="number"
              min="0"
              value={form.totalTickets}
              onChange={(event) => setField('totalTickets', event.target.value)}
            />
          </label>
          <label>
            Image URL
            <input value={form.imageUrl} onChange={(event) => setField('imageUrl', event.target.value)} />
          </label>
          <label>
            Latitude
            <input type="number" step="any" value={form.latitude} onChange={(event) => setField('latitude', event.target.value)} />
          </label>
          <label>
            Longitude
            <input type="number" step="any" value={form.longitude} onChange={(event) => setField('longitude', event.target.value)} />
          </label>
        </div>
        <label>
          Description
          <textarea value={form.description} onChange={(event) => setField('description', event.target.value)} rows="5" />
        </label>
        {isEditing && (
          <section className="tier-editor">
            <div className="section-header">
              <div>
                <p className="eyebrow">Inventory</p>
                <h2>Ticket tiers</h2>
              </div>
            </div>
            <FormError error={tierApiError} />
            <div className="tier-list">
              {(eventQuery.data?.tiers || []).map((tier) => (
                <div className="tier-row" key={tier.id}>
                  <div>
                    <strong>{tier.name}</strong>
                    <span>
                      {tier.soldQuantity ?? 0}/{tier.totalQuantity ?? 0} sold
                    </span>
                  </div>
                  <span>{tier.availableQuantity ?? 0} left</span>
                  <div className="row-actions">
                    <button className="button small" type="button" onClick={() => editTier(tier)}>
                      Edit
                    </button>
                    <button
                      className="button small ghost"
                      type="button"
                      onClick={() => deleteTierMutation.mutate(tier.id)}
                      disabled={deleteTierMutation.isPending}
                    >
                      Delete
                    </button>
                  </div>
                </div>
              ))}
            </div>
            <div className="form-grid">
              <label>
                Tier name
                <input value={tierForm.name} onChange={(event) => setTierField('name', event.target.value)} />
              </label>
              <label>
                Tier price
                <input type="number" min="0" step="0.01" value={tierForm.price} onChange={(event) => setTierField('price', event.target.value)} />
              </label>
              <label>
                Total quantity
                <input type="number" min="0" value={tierForm.totalQuantity} onChange={(event) => setTierField('totalQuantity', event.target.value)} />
              </label>
            </div>
            <label>
              Tier description
              <textarea value={tierForm.description} onChange={(event) => setTierField('description', event.target.value)} rows="3" />
            </label>
            <div className="row-actions">
              <button className="button" type="button" onClick={submitTier} disabled={tierMutation.isPending}>
                {editingTierId ? 'Update tier' : 'Add tier'}
              </button>
              {editingTierId && (
                <button
                  className="button ghost"
                  type="button"
                  onClick={() => {
                    setEditingTierId(null)
                    setTierForm(emptyTierForm)
                  }}
                >
                  Cancel
                </button>
              )}
            </div>
          </section>
        )}
        {!isEditing && (
          <section className="tier-editor">
            <div className="section-header">
              <div>
                <p className="eyebrow">Inventory</p>
                <h2>Ticket tiers</h2>
              </div>
            </div>
            <div className="form-grid">
              <label>
                Tier name
                <input value={tierForm.name} onChange={(event) => setTierField('name', event.target.value)} />
              </label>
              <label>
                Tier price
                <input type="number" min="0" step="0.01" value={tierForm.price} onChange={(event) => setTierField('price', event.target.value)} />
              </label>
              <label>
                Total quantity
                <input type="number" min="0" value={tierForm.totalQuantity} onChange={(event) => setTierField('totalQuantity', event.target.value)} />
              </label>
            </div>
            <label>
              Tier description
              <textarea value={tierForm.description} onChange={(event) => setTierField('description', event.target.value)} rows="3" />
            </label>
          </section>
        )}
        <button className="button primary" type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'Saving...' : 'Save event'}
        </button>
      </form>
    </section>
  )
}
