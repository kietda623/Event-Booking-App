import { FormError } from '../../components/FieldErrors'

function TierFields({ tierForm, setTierField }) {
  return (
    <>
      <div className="form-grid">
        <label>
          Tier name
          <input value={tierForm.name} onChange={(event) => setTierField('name', event.target.value)} />
        </label>
        <label>
          Tier price
          <input type="number" min="0" step="0.01" value={tierForm.price} onChange={(event) => setTierField('price', event.target.value)} />
        </label>
        <label>
          Total quantity
          <input type="number" min="0" value={tierForm.totalQuantity} onChange={(event) => setTierField('totalQuantity', event.target.value)} />
        </label>
      </div>
      <label>
        Tier description
        <textarea value={tierForm.description} onChange={(event) => setTierField('description', event.target.value)} rows="3" />
      </label>
    </>
  )
}

function TierHeader() {
  return (
    <div className="section-header">
      <div>
        <p className="eyebrow">Inventory</p>
        <h2>Ticket tiers</h2>
      </div>
    </div>
  )
}

export function TierEditorSection({
  isEditing,
  tiers,
  tierForm,
  editingTierId,
  tierApiError,
  tierMutation,
  deleteTierMutation,
  setTierField,
  editTier,
  submitTier,
  cancelTierEdit,
}) {
  return (
    <section className="tier-editor">
      <TierHeader />
      {isEditing && (
        <>
          <FormError error={tierApiError} />
          <div className="tier-list">
            {tiers.map((tier) => (
              <div className="tier-row" key={tier.id}>
                <div>
                  <strong>{tier.name}</strong>
                  <span>
                    {tier.soldQuantity ?? 0}/{tier.totalQuantity ?? 0} sold
                  </span>
                </div>
                <span>{tier.availableQuantity ?? 0} left</span>
                <div className="row-actions">
                  <button className="button small" type="button" onClick={() => editTier(tier)}>
                    Edit
                  </button>
                  <button
                    className="button small ghost"
                    type="button"
                    onClick={() => deleteTierMutation.mutate(tier.id)}
                    disabled={deleteTierMutation.isPending}
                  >
                    Delete
                  </button>
                </div>
              </div>
            ))}
          </div>
        </>
      )}
      <TierFields tierForm={tierForm} setTierField={setTierField} />
      {isEditing && (
        <div className="row-actions">
          <button className="button" type="button" onClick={submitTier} disabled={tierMutation.isPending}>
            {editingTierId ? 'Update tier' : 'Add tier'}
          </button>
          {editingTierId && (
            <button className="button ghost" type="button" onClick={cancelTierEdit}>
              Cancel
            </button>
          )}
        </div>
      )}
    </section>
  )
}
