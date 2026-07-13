import { FieldError } from '../../components/FieldErrors'

export function EventFormFields({ form, apiError, setField }) {
  return (
    <>
      <div className="form-grid">
        <label>
          Title
          <input value={form.title} onChange={(event) => setField('title', event.target.value)} required />
          <FieldError error={apiError} field="title" />
        </label>
        <label>
          Location
          <input value={form.location} onChange={(event) => setField('location', event.target.value)} />
        </label>
        <label>
          Start time
          <input
            type="datetime-local"
            value={form.startTime}
            onChange={(event) => setField('startTime', event.target.value)}
            required
          />
          <FieldError error={apiError} field="eventDate" />
        </label>
        <label>
          End time
          <input type="datetime-local" value={form.endTime} onChange={(event) => setField('endTime', event.target.value)} />
        </label>
        <label>
          Price
          <input type="number" min="0" step="0.01" value={form.price} onChange={(event) => setField('price', event.target.value)} />
        </label>
        <label>
          Total tickets
          <input
            type="number"
            min="0"
            value={form.totalTickets}
            onChange={(event) => setField('totalTickets', event.target.value)}
          />
        </label>
        <label>
          Image URL
          <input value={form.imageUrl} onChange={(event) => setField('imageUrl', event.target.value)} />
        </label>
        <label>
          Latitude
          <input type="number" step="any" value={form.latitude} onChange={(event) => setField('latitude', event.target.value)} />
        </label>
        <label>
          Longitude
          <input type="number" step="any" value={form.longitude} onChange={(event) => setField('longitude', event.target.value)} />
        </label>
      </div>
      <label>
        Description
        <textarea value={form.description} onChange={(event) => setField('description', event.target.value)} rows="5" />
      </label>
    </>
  )
}
