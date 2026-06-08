import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Bar, BarChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { adminApi } from '../../api/admin'
import { EmptyState, ErrorState, LoadingState } from '../../components/StateViews'
import { formatCurrency } from '../../utils/format'

export function AdminAnalyticsPage() {
  const query = useQuery({
    queryKey: ['admin-analytics'],
    queryFn: adminApi.analytics,
  })
  const statusRows = useMemo(() => {
    const source = query.data?.bookingsByStatus || {}
    return Object.entries(source).map(([status, count]) => ({ status, count }))
  }, [query.data])

  if (query.isLoading) return <LoadingState label="Loading analytics..." />
  if (query.isError) return <ErrorState error={query.error} title="Could not load analytics" />
  if (!query.data) return <EmptyState title="No analytics available" />

  const analytics = query.data

  return (
    <section className="page-stack">
      <div className="page-header">
        <div>
          <p className="eyebrow">Admin</p>
          <h1>Analytics</h1>
        </div>
      </div>
      <div className="metric-grid">
        <Metric label="Events" value={analytics.totalEvents} />
        <Metric label="Users" value={analytics.totalUsers} />
        <Metric label="Bookings" value={analytics.totalBookings} />
        <Metric label="Revenue" value={formatCurrency(analytics.totalRevenue)} />
      </div>
      <div className="analytics-grid">
        <section className="analytics-panel">
          <h2>Bookings by status</h2>
          <div className="chart-box">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={statusRows}>
                <XAxis dataKey="status" />
                <YAxis allowDecimals={false} />
                <Tooltip />
                <Bar dataKey="count" fill="#1f6f78" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </section>
        <section className="analytics-panel">
          <h2>Top events</h2>
          <div className="table-wrap simple">
            <table>
              <thead>
                <tr>
                  <th>Event</th>
                  <th>Booked</th>
                </tr>
              </thead>
              <tbody>
                {(analytics.topEvents || []).map((event) => (
                  <tr key={event.id}>
                    <td>{event.title}</td>
                    <td>{event.bookedCount}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </section>
  )
}

function Metric({ label, value }) {
  return (
    <div className="metric-card">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}
