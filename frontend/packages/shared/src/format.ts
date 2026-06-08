export function formatDateTime(value?: string | null) {
  if (!value) {
    return "TBA";
  }
  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(new Date(value));
}

export function formatMoney(value?: number | null) {
  if (value === undefined || value === null) {
    return "Mien phi";
  }
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0
  }).format(value);
}

export function formatStatus(status: string) {
  const labels: Record<string, string> = {
    PENDING: "Cho thanh toan",
    PAID: "Da thanh toan",
    CANCELLED: "Da huy"
  };
  return labels[status] || status;
}
