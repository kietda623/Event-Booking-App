export function FavoriteButton({ active = false, disabled = false, onClick, label = 'Toggle favorite' }) {
  return (
    <button
      className={`favorite-button ${active ? 'active' : ''}`}
      type="button"
      aria-label={label}
      title={active ? 'Remove favorite' : 'Add favorite'}
      disabled={disabled}
      onClick={onClick}
    >
      {active ? '♥' : '♡'}
    </button>
  )
}
