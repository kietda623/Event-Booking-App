import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { ticketsApi } from '../../api/tickets'
import { EmptyState, ErrorState, LoadingState } from '../../components/StateViews'
import { Pagination } from '../../components/Pagination'
import { StatusBadge } from '../../components/StatusBadge'
import { formatDateTime } from '../../utils/format'

export function TicketsPage() {
  const [page, setPage] = useState(0)
  const query = useQuery({
    queryKey: ['tickets', page],
    queryFn: () => ticketsApi.mine({ page, size: 10 }),
  })

  if (query.isLoading) return <LoadingState label="Loading tickets..." />
  if (query.isError) return <ErrorState error={query.error} title="Could not load tickets" />

  const tickets = query.data?.content || []

  return (
    <section className="page-stack">
      <div className="page-header">
        <div>
          <p className="eyebrow">Entry passes</p>
          <h1>Tickets</h1>
        </div>
      </div>
      {tickets.length === 0 ? (
        <EmptyState title="No tickets yet" text="Paid bookings will generate tickets here." />
      ) : (
        <>
          <div className="ticket-grid">
            {tickets.map((ticket) => (
              <article className="ticket-card" key={ticket.ticketId} data-testid="ticket-card">
                <div>
                  <p className="eyebrow">{ticket.ticketType || 'GENERAL'}</p>
                  <h3>{ticket.eventTitle}</h3>
                </div>
                <code data-testid="ticket-code">{ticket.ticketCode}</code>
                <dl className="info-list compact">
                  <div>
                    <dt>Date</dt>
                    <dd>{formatDateTime(ticket.eventDate)}</dd>
                  </div>
                  <div>
                    <dt>Quantity</dt>
                    <dd>{ticket.quantity}</dd>
                  </div>
                  <div>
                    <dt>Status</dt>
                    <dd>
                      <StatusBadge status={ticket.status} />
                    </dd>
                  </div>
                </dl>
              </article>
            ))}
          </div>
          <Pagination page={query.data?.page || 0} totalPages={query.data?.totalPages || 0} onPageChange={setPage} />
        </>
      )}
    </section>
  )
}
