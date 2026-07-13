import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { Link, Navigate, Route, Routes, useLocation, useNavigate, useParams } from "react-router-dom";
import { CalendarPlus, LayoutDashboard, LogOut, Search, ShieldAlert } from "lucide-react";
import { toast } from "sonner";
import {
  ApiError,
  authApi,
  clearAuth,
  eventApi,
  formatDateTime,
  formatMoney,
  getInitialSession,
  isAdmin,
  saveAuth,
  userApi,
  type AuthSession,
  type EventRequest,
  type EventResponse
} from "@eventbooking/shared";
import {
  Alert,
  AlertDescription,
  Badge,
  Button,
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  Input,
  Label,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Textarea
} from "@eventbooking/shared/ui";

function getErrorMessage(error: unknown) {
  return error instanceof ApiError ? error.message : "Co loi xay ra";
}

function useAdminSession() {
  const [session, setSession] = useState<AuthSession>(() => getInitialSession());

  const refreshProfile = useCallback(async () => {
    if (!session.token) {
      return null;
    }
    const profile = await userApi.profile();
    setSession((current) => ({ ...current, profile }));
    return profile;
  }, [session.token]);

  useEffect(() => {
    if (session.token && !session.profile) {
      refreshProfile().catch(() => undefined);
    }
  }, [refreshProfile, session.profile, session.token]);

  useEffect(() => {
    const handler = () => setSession(getInitialSession());
    window.addEventListener("eventbooking:unauthorized", handler);
    return () => window.removeEventListener("eventbooking:unauthorized", handler);
  }, []);

  const login = async (username: string, password: string) => {
    const response = await authApi.login({ username, password });
    saveAuth(response);
    const profile = await userApi.profile();
    setSession({ token: response.token, username: response.username, profile });
  };

  const logout = () => {
    clearAuth();
    setSession(getInitialSession());
  };

  return { session, login, logout };
}

export default function App() {
  const auth = useAdminSession();
  return (
    <Routes>
      <Route path="/login" element={<LoginPage login={auth.login} />} />
      <Route path="/admin/*" element={<AdminGuard session={auth.session}><AdminLayout onLogout={auth.logout} session={auth.session} /></AdminGuard>} />
      <Route path="*" element={<Navigate to="/admin/events" replace />} />
    </Routes>
  );
}

function LoginPage({ login }: { login: (username: string, password: string) => Promise<void> }) {
  const navigate = useNavigate();
  const location = useLocation();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const from = (location.state as { from?: string } | null)?.from || "/admin/events";

  async function submit(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    try {
      await login(username, password);
      toast.success("Dang nhap admin thanh cong");
      navigate(from, { replace: true });
    } catch (error) {
      toast.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Admin Login</CardTitle>
          <CardDescription>Dang nhap bang account co role ADMIN.</CardDescription>
        </CardHeader>
        <CardContent>
          <form className="flex flex-col gap-4" onSubmit={submit}>
            <div className="flex flex-col gap-2"><Label>Username</Label><Input value={username} onChange={(event) => setUsername(event.target.value)} required /></div>
            <div className="flex flex-col gap-2"><Label>Password</Label><Input type="password" value={password} onChange={(event) => setPassword(event.target.value)} required /></div>
            <Button disabled={loading}>{loading ? "Dang xu ly..." : "Dang nhap"}</Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}

function AdminGuard({ session, children }: { session: AuthSession; children: React.ReactNode }) {
  const location = useLocation();
  if (!session.token) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }
  if (session.profile && !isAdmin(session.profile)) {
    return (
      <div className="flex min-h-screen items-center justify-center px-4">
        <Alert variant="destructive" className="max-w-lg">
          <ShieldAlert />
          <AlertDescription>403 Forbidden. Tai khoan hien tai khong co quyen ADMIN.</AlertDescription>
        </Alert>
      </div>
    );
  }
  return children;
}

function AdminLayout({ session, onLogout }: { session: AuthSession; onLogout: () => void }) {
  return (
    <div className="grid min-h-screen md:grid-cols-[260px_1fr]">
      <aside className="border-r bg-card p-5">
        <Link to="/admin/events" className="flex items-center gap-2 font-semibold">
          <LayoutDashboard />
          Event Admin
        </Link>
        <nav className="mt-8 flex flex-col gap-2">
          <Button asChild variant="secondary">
            <Link to="/admin/events">Events</Link>
          </Button>
          <Button asChild variant="ghost">
            <Link to="/admin/events/new">Create event</Link>
          </Button>
        </nav>
      </aside>
      <div>
        <header className="flex items-center justify-between border-b bg-background px-6 py-4">
          <div>
            <p className="text-sm text-muted-foreground">Signed in as</p>
            <p className="font-medium">{session.profile?.fullName || session.username}</p>
          </div>
          <Button variant="outline" onClick={onLogout}>
            <LogOut data-icon="inline-start" />
            Logout
          </Button>
        </header>
        <main className="p-6">
          <Routes>
            <Route index element={<Navigate to="events" replace />} />
            <Route path="events" element={<EventsAdminPage />} />
            <Route path="events/new" element={<EventFormPage mode="create" />} />
            <Route path="events/:id/edit" element={<EventFormPage mode="edit" />} />
          </Routes>
        </main>
      </div>
    </div>
  );
}

function EventsAdminPage() {
  const [events, setEvents] = useState<EventResponse[]>([]);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const load = useCallback(() => {
    eventApi
      .list({ search, page, size: 10, sortBy: "eventDate", sortDir: "asc" })
      .then((response) => {
        setEvents(response.content);
        setTotalPages(response.totalPages);
      })
      .catch((error) => toast.error(getErrorMessage(error)));
  }, [page, search]);

  useEffect(() => { load(); }, [load]);

  async function remove(event: EventResponse) {
    if (!window.confirm(`Delete "${event.title}"?`)) {
      return;
    }
    try {
      await eventApi.remove(event.id);
      toast.success("Da xoa su kien");
      load();
    } catch (error) {
      toast.error(getErrorMessage(error));
    }
  }

  const metrics = useMemo(() => ({
    events: events.length,
    available: events.reduce((total, event) => total + (event.availableTickets || 0), 0)
  }), [events]);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold">Event management</h1>
          <p className="text-sm text-muted-foreground">CRUD event tren backend Spring Boot hien tai.</p>
        </div>
        <Button asChild>
          <Link to="/admin/events/new"><CalendarPlus data-icon="inline-start" /> Create event</Link>
        </Button>
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        <Card><CardHeader><CardTitle>{metrics.events}</CardTitle><CardDescription>Events in page</CardDescription></CardHeader></Card>
        <Card><CardHeader><CardTitle>{metrics.available}</CardTitle><CardDescription>Available tickets in page</CardDescription></CardHeader></Card>
      </div>
      <Card>
        <CardHeader>
          <CardTitle>Events</CardTitle>
          <CardDescription>Search, paginate, edit and delete events.</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-2.5 text-muted-foreground" />
            <Input className="pl-10" placeholder="Search title or location" value={search} onChange={(event) => { setPage(0); setSearch(event.target.value); }} />
          </div>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Title</TableHead>
                <TableHead>Date</TableHead>
                <TableHead>Location</TableHead>
                <TableHead>Price</TableHead>
                <TableHead>Tickets</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {events.map((event) => (
                <TableRow key={event.id}>
                  <TableCell className="font-medium">{event.title}</TableCell>
                  <TableCell>{formatDateTime(event.eventDate)}</TableCell>
                  <TableCell>{event.location || "TBA"}</TableCell>
                  <TableCell>{formatMoney(event.price)}</TableCell>
                  <TableCell><Badge variant="secondary">{event.availableTickets ?? 0}</Badge></TableCell>
                  <TableCell>
                    <div className="flex justify-end gap-2">
                      <Button asChild variant="outline" size="sm"><Link to={`/admin/events/${event.id}/edit`}>Edit</Link></Button>
                      <Button variant="destructive" size="sm" onClick={() => remove(event)}>Delete</Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <div className="flex justify-end gap-2">
            <Button variant="outline" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>Prev</Button>
            <Button variant="outline" disabled={page + 1 >= totalPages} onClick={() => setPage((value) => value + 1)}>Next</Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

function EventFormPage({ mode }: { mode: "create" | "edit" }) {
  const navigate = useNavigate();
  const { id } = useParams();
  const [form, setForm] = useState<EventRequest>({
    title: "",
    description: "",
    eventDate: "",
    location: "",
    price: 0,
    totalTickets: 100
  });
  const [loading, setLoading] = useState(mode === "edit");

  useEffect(() => {
    if (mode === "edit" && id) {
      eventApi
        .get(Number(id))
        .then((event) => setForm({
          title: event.title,
          description: event.description || "",
          eventDate: event.eventDate.slice(0, 16),
          location: event.location || "",
          price: event.price || 0,
          totalTickets: event.availableTickets || 0
        }))
        .catch((error) => toast.error(getErrorMessage(error)))
        .finally(() => setLoading(false));
    }
  }, [id, mode]);

  function update<K extends keyof EventRequest>(key: K, value: EventRequest[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    try {
      const payload = { ...form, eventDate: new Date(form.eventDate).toISOString().slice(0, 19) };
      if (mode === "create") {
        await eventApi.create(payload);
      } else {
        await eventApi.update(Number(id), payload);
      }
      toast.success(mode === "create" ? "Da tao su kien" : "Da cap nhat su kien");
      navigate("/admin/events");
    } catch (error) {
      toast.error(getErrorMessage(error));
    }
  }

  if (loading) {
    return <Card><CardHeader><CardTitle>Loading...</CardTitle></CardHeader></Card>;
  }

  return (
    <Card className="max-w-3xl">
      <CardHeader>
        <CardTitle>{mode === "create" ? "Create event" : "Edit event"}</CardTitle>
        <CardDescription>Fields map truc tiep voi EventRequest backend.</CardDescription>
      </CardHeader>
      <CardContent>
        <form className="flex flex-col gap-4" onSubmit={submit}>
          <div className="flex flex-col gap-2"><Label>Title</Label><Input value={form.title} onChange={(event) => update("title", event.target.value)} required /></div>
          <div className="flex flex-col gap-2"><Label>Description</Label><Textarea value={form.description} onChange={(event) => update("description", event.target.value)} /></div>
          <div className="grid gap-4 md:grid-cols-2">
            <div className="flex flex-col gap-2"><Label>Date</Label><Input type="datetime-local" value={form.eventDate} onChange={(event) => update("eventDate", event.target.value)} required /></div>
            <div className="flex flex-col gap-2"><Label>Location</Label><Input value={form.location} onChange={(event) => update("location", event.target.value)} /></div>
          </div>
          <div className="grid gap-4 md:grid-cols-2">
            <div className="flex flex-col gap-2"><Label>Price</Label><Input type="number" value={form.price} onChange={(event) => update("price", Number(event.target.value))} /></div>
            <div className="flex flex-col gap-2"><Label>Total tickets</Label><Input type="number" value={form.totalTickets} onChange={(event) => update("totalTickets", Number(event.target.value))} /></div>
          </div>
          <div className="flex gap-2">
            <Button>{mode === "create" ? "Create" : "Save"}</Button>
            <Button asChild variant="outline"><Link to="/admin/events">Cancel</Link></Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
