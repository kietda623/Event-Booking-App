import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { eventsApi } from '../../api/events'
import { FormError } from '../../components/FieldErrors'
import { EmptyState, ErrorState, LoadingState } from '../../components/StateViews'
import { EventFormFields } from './EventFormFields'
import { TierEditorSection } from './TierEditorSection'
import {
  buildEventPayload,
  buildInitialTierPayload,
  buildTierPayload,
  emptyEventForm,
  emptyTierForm,
  eventToForm,
  tierToForm,
} from './adminEventFormModel'

export function AdminEventFormPage() {
  const { id } = useParams()
  const isEditing = Boolean(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [form, setForm] = useState(emptyEventForm)
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
      setForm(eventToForm(eventQuery.data))
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
    tierMutation.mutate(buildTierPayload(tierForm))
  }

  const editTier = (tier) => {
    setEditingTierId(tier.id)
    setTierForm(tierToForm(tier))
  }

  const cancelTierEdit = () => {
    setEditingTierId(null)
    setTierForm(emptyTierForm)
  }

  const onSubmit = (event) => {
    event.preventDefault()
    setApiError(null)
    mutation.mutate({
      eventPayload: buildEventPayload(form),
      initialTierPayload: buildInitialTierPayload(isEditing, tierForm),
    })
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
        <EventFormFields form={form} apiError={apiError} setField={setField} />
        <TierEditorSection
          isEditing={isEditing}
          tiers={eventQuery.data?.tiers || []}
          tierForm={tierForm}
          editingTierId={editingTierId}
          tierApiError={tierApiError}
          tierMutation={tierMutation}
          deleteTierMutation={deleteTierMutation}
          setTierField={setTierField}
          editTier={editTier}
          submitTier={submitTier}
          cancelTierEdit={cancelTierEdit}
        />
        <button className="button primary" type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'Saving...' : 'Save event'}
        </button>
      </form>
    </section>
  )
}
