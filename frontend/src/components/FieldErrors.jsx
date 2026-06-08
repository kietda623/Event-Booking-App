export function fieldMessage(error, field) {
  return error?.errors?.find((item) => item.field === field)?.message
}

export function FieldError({ error, field }) {
  const message = fieldMessage(error, field)
  if (!message) return null
  return <span className="field-error">{message}</span>
}

export function FormError({ error }) {
  if (!error?.message) return null
  return (
    <div className="form-error">
      <strong>{error.code || 'ERROR'}</strong>
      <span>{error.message}</span>
    </div>
  )
}
