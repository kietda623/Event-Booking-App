export function FavoriteButton({ active = false, disabled = false, onClick, label = 'Toggle favorite' }) {
  return (
    <button
      className={`favorite-button ${active ? 'active' : ''}`}
      type="button"
      data-testid="favorite-toggle"
      aria-label={label}
      aria-pressed={active}
      title={active ? 'Remove favorite' : 'Add favorite'}
      disabled={disabled}
      onClick={onClick}
    >
      {active ? '♥' : '♡'}
    </button>
  )
}
