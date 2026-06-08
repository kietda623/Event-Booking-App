import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { bookingsApi } from '../api/bookings'
import { FormError } from './FieldErrors'

export function CancelBookingButton({ booking, onCancelled }) {
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState(null)
  const canCancel = booking?.status === 'PENDING' || booking?.status === 'PAID'
  const mutation = useMutation({
    mutationFn: () => bookingsApi.cancel(booking.bookingId),
    onSuccess: (updated) => {
      setApiError(null)
      queryClient.invalidateQueries({ queryKey: ['bookings'] })
      queryClient.invalidateQueries({ queryKey: ['tickets'] })
      onCancelled?.(updated)
    },
    onError: setApiError,
  })

  if (!canCancel) return null

  const onCancel = () => {
    const warning =
      booking.status === 'PAID'
        ? 'Cancel this paid booking? A mock refund will be created as pending credit.'
        : 'Cancel this pending booking?'
    if (window.confirm(warning)) {
      mutation.mutate()
    }
  }

  return (
    <div className="inline-action">
      <button className="button ghost small" type="button" disabled={mutation.isPending} onClick={onCancel}>
        {mutation.isPending ? 'Cancelling...' : 'Cancel'}
      </button>
      <FormError error={apiError} />
    </div>
  )
}
