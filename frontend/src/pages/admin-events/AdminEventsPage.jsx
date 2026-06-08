import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { eventsApi } from '../../api/events'
import { EmptyState, ErrorState, LoadingState } from '../../components/StateViews'
import { Pagination } from '../../components/Pagination'
import { formatCurrency, formatDateTime } from '../../utils/format'

export function AdminEventsPage() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [apiError, setApiError] = useState(null)
  const query = useQuery({
    queryKey: ['admin-events', page],
    queryFn: () => eventsApi.list({ page, size: 10 }),
  })
  const deleteMutation = useMutation({
    mutationFn: eventsApi.remove,
    onSuccess: () => {
      setApiError(null)
      queryClient.invalidateQueries({ queryKey: ['admin-events'] })
      queryClient.invalidateQueries({ queryKey: ['events'] })
    },
    onError: setApiError,
  })

  const onDelete = (event) => {
    if (window.confirm(`Delete "${event.title}"?`)) {
      deleteMutation.mutate(event.id)
    }
  }

  if (query.isLoading) return <LoadingState label="Loading admin events..." />
  if (query.isError) return <ErrorState error={query.error} title="Could not load admin events" />

  const events = query.data?.content || []

  return (
    <section className="page-stack">
      <div className="page-header">
        <div>
          <p className="eyebrow">Admin</p>
          <h1>Event management</h1>
        </div>
        <Link className="button primary" to="/admin/events/new">
          New event
        </Link>
      </div>
      {apiError && <ErrorState error={apiError} title="Could not delete event" />}
      {events.length === 0 ? (
        <EmptyState title="No events yet" text="Create the first event to start selling tickets." />
      ) : (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Date</th>
                  <th>Location</th>
                  <th>Price</th>
                  <th>Tickets</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {events.map((event) => (
                  <tr key={event.id}>
                    <td>{event.title}</td>
                    <td>{formatDateTime(event.eventDate)}</td>
                    <td>{event.location}</td>
                    <td>{formatCurrency(event.price)}</td>
                    <td>{event.availableTickets}</td>
                    <td className="row-actions">
                      <Link className="button small" to={`/admin/events/${event.id}/edit`}>
                        Edit
                      </Link>
                      <button className="button ghost small" type="button" onClick={() => onDelete(event)}>
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={query.data?.page || 0} totalPages={query.data?.totalPages || 0} onPageChange={setPage} />
        </>
      )}
    </section>
  )
}
