export function StatusBadge({ status }) {
  return <span className={`status-badge ${String(status || '').toLowerCase()}`}>{status || 'UNKNOWN'}</span>
}
