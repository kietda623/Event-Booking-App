import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { ticketsApi } from '../../api/tickets'
import { FormError } from '../../components/FieldErrors'
import { formatDateTime } from '../../utils/format'

export function AdminCheckInPage() {
  const [ticketCode, setTicketCode] = useState('')
  const [apiError, setApiError] = useState(null)
  const mutation = useMutation({
    mutationFn: ticketsApi.checkIn,
    onSuccess: () => {
      setApiError(null)
      setTicketCode('')
    },
    onError: setApiError,
  })

  const onSubmit = (event) => {
    event.preventDefault()
    setApiError(null)
    mutation.mutate({ ticketCode: ticketCode.trim() })
  }

  const result = mutation.data

  return (
    <section className="narrow-page">
      <div className="page-header">
        <div>
          <p className="eyebrow">Admin</p>
          <h1>Ticket check-in</h1>
        </div>
      </div>
      <form className="form-card" onSubmit={onSubmit}>
        <FormError error={apiError} />
        {result && (
          <div className="toast">
            <strong>{result.attendeeName}</strong>
            <span>
              {result.eventTitle} checked in at {formatDateTime(result.checkedInAt)}
              {result.seatNumber ? `, seat ${result.seatNumber}` : ''}
            </span>
          </div>
        )}
        <label>
          Ticket code
          <input
            value={ticketCode}
            onChange={(event) => setTicketCode(event.target.value)}
            placeholder="Paste ticket UUID"
            autoComplete="off"
          />
        </label>
        <button className="button primary" type="submit" disabled={mutation.isPending || !ticketCode.trim()}>
          {mutation.isPending ? 'Checking in...' : 'Check in'}
        </button>
      </form>
    </section>
  )
}
