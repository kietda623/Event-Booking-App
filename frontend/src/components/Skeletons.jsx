export function EventGridSkeleton({ count = 6 }) {
  return (
    <div className="event-grid" aria-label="Loading events">
      {Array.from({ length: count }).map((_, index) => (
        <article className="event-card skeleton-card" key={index}>
          <div className="skeleton-block image" />
          <div className="event-card-body">
            <div className="skeleton-line short" />
            <div className="skeleton-line title" />
            <div className="skeleton-line" />
            <div className="event-card-footer">
              <div className="skeleton-line short" />
              <div className="skeleton-button" />
            </div>
          </div>
        </article>
      ))}
    </div>
  )
}

export function BookingListSkeleton({ rows = 5 }) {
  return (
    <div className="table-wrap" aria-label="Loading bookings">
      <table>
        <tbody>
          {Array.from({ length: rows }).map((_, index) => (
            <tr key={index}>
              <td>
                <div className="skeleton-line title" />
              </td>
              <td>
                <div className="skeleton-line" />
              </td>
              <td>
                <div className="skeleton-line short" />
              </td>
              <td>
                <div className="skeleton-line short" />
              </td>
              <td>
                <div className="skeleton-button" />
              </td>
              <td>
                <div className="skeleton-button" />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
