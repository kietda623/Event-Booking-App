export function LoadingState({ label = 'Loading data...' }) {
  return <div className="state-box">{label}</div>
}

export function EmptyState({ title = 'No data found', text = 'Try a different filter or come back later.' }) {
  return (
    <div className="state-box">
      <strong>{title}</strong>
      <span>{text}</span>
    </div>
  )
}

export function ErrorState({ error, title = 'Something went wrong' }) {
  return (
    <div className="state-box error">
      <strong>{title}</strong>
      <span>{error?.message || 'Unable to complete the request.'}</span>
      {error?.code && <code>{error.code}</code>}
    </div>
  )
}
