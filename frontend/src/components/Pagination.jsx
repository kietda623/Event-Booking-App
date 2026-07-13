export function Pagination({ page = 0, totalPages = 0, onPageChange }) {
  return (
    <div className="pagination">
      <button className="button ghost" type="button" disabled={page <= 0} onClick={() => onPageChange(page - 1)}>
        Previous
      </button>
      <span>
        Page {page + 1} of {Math.max(totalPages, 1)}
      </span>
      <button
        className="button ghost"
        type="button"
        disabled={page + 1 >= totalPages}
        onClick={() => onPageChange(page + 1)}
      >
        Next
      </button>
    </div>
  )
}
