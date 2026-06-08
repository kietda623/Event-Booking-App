import React, { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { Link, Navigate, Route, Routes, useLocation, useNavigate, useParams } from "react-router-dom";
import { CalendarDays, Heart, LogOut, Search, Ticket, UserRound } from "lucide-react";
import { toast } from "sonner";
import {
  ApiError,
  authApi,
  bookingApi,
  clearAuth,
  eventApi,
  favoriteApi,
  formatDateTime,
  formatMoney,
  formatStatus,
  getInitialSession,
  paymentApi,
  saveAuth,
  ticketApi,
  userApi,
  type AuthSession,
  type BookingResponse,
  type EventResponse,
  type FavoriteResponse,
  type TicketResponse
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
  Skeleton
} from "@eventbooking/shared/ui";

function getErrorMessage(error: unknown) {
  return error instanceof ApiError ? error.message : "Co loi xay ra";
}

function useAuthSession() {
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

  const register = async (username: string, password: string, email: string) => {
    const response = await authApi.register({ username, password, email });
    saveAuth(response);
    const profile = await userApi.profile();
    setSession({ token: response.token, username: response.username, profile });
  };

  const logout = () => {
    clearAuth();
    setSession(getInitialSession());
  };

  return { session, login, register, logout, refreshProfile };
}

type UserAuth = ReturnType<typeof useAuthSession>;

function AppShell({ auth }: { auth: UserAuth }) {
  const { session } = auth;
  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-10 border-b bg-background/95 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
          <Link to="/" className="flex items-center gap-2 font-semibold">
            <CalendarDays />
            Event Booking
          </Link>
          <nav className="hidden items-center gap-1 md:flex">
            <Button asChild variant="ghost" size="sm">
              <Link to="/">Events</Link>
            </Button>
            <Button asChild variant="ghost" size="sm">
              <Link to="/favorites">Favorites</Link>
            </Button>
            <Button asChild variant="ghost" size="sm">
              <Link to="/bookings">Bookings</Link>
            </Button>
            <Button asChild variant="ghost" size="sm">
              <Link to="/tickets">Tickets</Link>
            </Button>
          </nav>
          <div className="flex items-center gap-2">
            {session.token ? (
              <>
                <Button asChild variant="outline" size="sm">
                  <Link to="/profile">
                    <UserRound data-icon="inline-start" />
                    {session.profile?.fullName || session.username}
                  </Link>
                </Button>
                <Button variant="ghost" size="icon" onClick={auth.logout} aria-label="Logout">
                  <LogOut />
                </Button>
              </>
            ) : (
              <Button asChild size="sm">
                <Link to="/login">Login</Link>
              </Button>
            )}
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-8">
        <Routes>
          <Route path="/" element={<EventList />} />
          <Route path="/events/:id" element={<EventDetail authed={Boolean(session.token)} />} />
          <Route path="/login" element={<AuthPage mode="login" />} />
          <Route path="/register" element={<AuthPage mode="register" />} />
          <Route path="/bookings" element={<Protected token={session.token}><BookingsPage /></Protected>} />
          <Route path="/bookings/:id/payment" element={<Protected token={session.token}><PaymentPage /></Protected>} />
          <Route path="/tickets" element={<Protected token={session.token}><TicketsPage /></Protected>} />
          <Route path="/favorites" element={<Protected token={session.token}><FavoritesPage /></Protected>} />
          <Route path="/profile" element={<Protected token={session.token}><ProfilePage refreshProfile={auth.refreshProfile} /></Protected>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );

  function AuthPage({ mode }: { mode: "login" | "register" }) {
    return <AuthForm mode={mode} login={auth.login} register={auth.register} />;
  }
}

export default function App() {
  const auth = useAuthSession();
  return <AppShell auth={auth} />;
}

function Protected({ token, children }: { token: string | null; children: React.ReactNode }) {
  const location = useLocation();
  if (!token) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }
  return children;
}

function AuthForm({
  mode,
  login,
  register
}: {
  mode: "login" | "register";
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, password: string, email: string) => Promise<void>;
}) {
  const navigate = useNavigate();
  const location = useLocation();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const from = (location.state as { from?: string } | null)?.from || "/";

  async function submit(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    try {
      if (mode === "login") {
        await login(username, password);
      } else {
        await register(username, password, email);
      }
      toast.success(mode === "login" ? "Dang nhap thanh cong" : "Dang ky thanh cong");
      navigate(from, { replace: true });
    } catch (error) {
      toast.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto max-w-md">
      <Card>
        <CardHeader>
          <CardTitle>{mode === "login" ? "Dang nhap" : "Tao tai khoan"}</CardTitle>
          <CardDescription>Su dung username/password theo backend MVP hien tai.</CardDescription>
        </CardHeader>
        <CardContent>
          <form className="flex flex-col gap-4" onSubmit={submit}>
            <div className="flex flex-col gap-2">
              <Label htmlFor="username">Username</Label>
              <Input id="username" value={username} onChange={(event) => setUsername(event.target.value)} required />
            </div>
            {mode === "register" && (
              <div className="flex flex-col gap-2">
                <Label htmlFor="email">Email</Label>
                <Input id="email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} />
              </div>
            )}
            <div className="flex flex-col gap-2">
              <Label htmlFor="password">Password</Label>
              <Input id="password" type="password" value={password} onChange={(event) => setPassword(event.target.value)} required />
            </div>
            <Button disabled={loading}>{loading ? "Dang xu ly..." : mode === "login" ? "Dang nhap" : "Dang ky"}</Button>
          </form>
          <Button asChild variant="link" className="mt-4 px-0">
            <Link to={mode === "login" ? "/register" : "/login"}>
              {mode === "login" ? "Chua co tai khoan?" : "Da co tai khoan?"}
            </Link>
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}

function EventList() {
  const [search, setSearch] = useState("");
  const [upcoming, setUpcoming] = useState(true);
  const [page, setPage] = useState(0);
  const [events, setEvents] = useState<EventResponse[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    eventApi
      .list({ search, upcoming, page, size: 9, sortBy: "eventDate", sortDir: "asc" })
      .then((response) => {
        setEvents(response.content);
        setTotalPages(response.totalPages);
      })
      .catch((error) => toast.error(getErrorMessage(error)))
      .finally(() => setLoading(false));
  }, [page, search, upcoming]);

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-4 rounded-lg border bg-card p-6">
        <div>
          <p className="text-sm font-medium text-muted-foreground">Discover</p>
          <h1 className="text-3xl font-bold tracking-normal">Tim va dat ve su kien</h1>
        </div>
        <div className="flex flex-col gap-3 md:flex-row">
          <div className="relative flex-1">
            <Search className="pointer-events-none absolute left-3 top-2.5 text-muted-foreground" />
            <Input className="pl-10" placeholder="Tim theo ten hoac dia diem" value={search} onChange={(event) => { setPage(0); setSearch(event.target.value); }} />
          </div>
          <Button variant={upcoming ? "default" : "outline"} onClick={() => { setPage(0); setUpcoming((value) => !value); }}>
            Upcoming
          </Button>
        </div>
      </section>
      {loading ? (
        <div className="grid gap-4 md:grid-cols-3">{Array.from({ length: 6 }).map((_, index) => <Skeleton key={index} className="h-56" />)}</div>
      ) : events.length === 0 ? (
        <Alert><AlertDescription>Chua co su kien phu hop.</AlertDescription></Alert>
      ) : (
        <div className="grid gap-4 md:grid-cols-3">
          {events.map((event) => <EventCard key={event.id} event={event} />)}
        </div>
      )}
      <div className="flex justify-end gap-2">
        <Button variant="outline" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>Prev</Button>
        <Button variant="outline" disabled={page + 1 >= totalPages} onClick={() => setPage((value) => value + 1)}>Next</Button>
      </div>
    </div>
  );
}

function EventCard({ event }: { event: EventResponse }) {
  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between gap-3">
          <CardTitle className="line-clamp-2">{event.title}</CardTitle>
          <Badge variant="secondary">{formatMoney(event.price)}</Badge>
        </div>
        <CardDescription>{formatDateTime(event.eventDate)}</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <p className="line-clamp-3 text-sm text-muted-foreground">{event.description || "Chua co mo ta."}</p>
        <div className="flex items-center justify-between text-sm">
          <span>{event.location || "TBA"}</span>
          <span>{event.availableTickets ?? 0} ve</span>
        </div>
        <Button asChild>
          <Link to={`/events/${event.id}`}>Xem chi tiet</Link>
        </Button>
      </CardContent>
    </Card>
  );
}

function EventDetail({ authed }: { authed: boolean }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const [event, setEvent] = useState<EventResponse | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    eventApi.get(Number(id)).then(setEvent).catch((error) => toast.error(getErrorMessage(error))).finally(() => setLoading(false));
  }, [id]);

  async function book() {
    if (!authed) {
      navigate("/login", { state: { from: `/events/${id}` } });
      return;
    }
    try {
      const booking = await bookingApi.create({ eventId: Number(id), quantity });
      toast.success("Da tao booking");
      navigate(`/bookings/${booking.bookingId}/payment`);
    } catch (error) {
      toast.error(getErrorMessage(error));
    }
  }

  if (loading) {
    return <Skeleton className="h-96" />;
  }
  if (!event) {
    return <Alert variant="destructive"><AlertDescription>Khong tim thay su kien.</AlertDescription></Alert>;
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
      <Card>
        <CardHeader>
          <CardTitle className="text-3xl">{event.title}</CardTitle>
          <CardDescription>{formatDateTime(event.eventDate)} - {event.location || "TBA"}</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <p className="text-muted-foreground">{event.description || "Chua co mo ta chi tiet."}</p>
          <Badge className="w-fit" variant="secondary">{event.availableTickets ?? 0} ve con lai</Badge>
        </CardContent>
      </Card>
      <Card>
        <CardHeader>
          <CardTitle>Dat ve</CardTitle>
          <CardDescription>{formatMoney(event.price)} moi ve</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <Label htmlFor="quantity">So luong</Label>
            <Input id="quantity" type="number" min={1} value={quantity} onChange={(event) => setQuantity(Number(event.target.value))} />
          </div>
          <Button onClick={book}>Dat ve</Button>
        </CardContent>
      </Card>
    </div>
  );
}

function BookingsPage() {
  const [bookings, setBookings] = useState<BookingResponse[]>([]);
  const load = useCallback(() => bookingApi.mine().then(setBookings).catch((error) => toast.error(getErrorMessage(error))), []);
  useEffect(() => { load(); }, [load]);

  async function cancel(id: number) {
    try {
      await bookingApi.cancel(id);
      toast.success("Da huy booking");
      load();
    } catch (error) {
      toast.error(getErrorMessage(error));
    }
  }

  return <ListPage title="My Bookings" empty="Chua co booking.">
    {bookings.map((booking) => (
      <Card key={booking.bookingId}>
        <CardHeader>
          <CardTitle>{booking.eventTitle}</CardTitle>
          <CardDescription>{formatDateTime(booking.bookingTime)}</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div className="flex flex-wrap gap-2">
            <Badge variant="secondary">{booking.quantity} ve</Badge>
            <Badge variant="outline">{formatMoney(booking.totalPrice)}</Badge>
            <Badge>{formatStatus(booking.status)}</Badge>
          </div>
          <div className="flex gap-2">
            {booking.status === "PENDING" && <Button asChild><Link to={`/bookings/${booking.bookingId}/payment`}>Thanh toan</Link></Button>}
            {booking.status === "PENDING" && <Button variant="outline" onClick={() => cancel(booking.bookingId)}>Huy</Button>}
          </div>
        </CardContent>
      </Card>
    ))}
  </ListPage>;
}

function PaymentPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  async function pay(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    try {
      await paymentApi.pay({ bookingId: Number(id), method: "MOCK_CARD" });
      toast.success("Thanh toan thanh cong");
      navigate("/tickets");
    } catch (error) {
      toast.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  }
  return (
    <div className="mx-auto max-w-lg">
      <Card>
        <CardHeader>
          <CardTitle>Payment mock</CardTitle>
          <CardDescription>Backend MVP chua ket noi payment gateway that.</CardDescription>
        </CardHeader>
        <CardContent>
          <form className="flex flex-col gap-4" onSubmit={pay}>
            <Input placeholder="4242 4242 4242 4242" />
            <div className="grid grid-cols-2 gap-3"><Input placeholder="MM/YY" /><Input placeholder="CVV" /></div>
            <Button disabled={loading}>{loading ? "Dang thanh toan..." : "Thanh toan"}</Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}

function TicketsPage() {
  const [tickets, setTickets] = useState<TicketResponse[]>([]);
  useEffect(() => { ticketApi.mine().then(setTickets).catch((error) => toast.error(getErrorMessage(error))); }, []);
  return <ListPage title="My Tickets" empty="Chua co ve.">
    {tickets.map((ticket) => (
      <Card key={ticket.ticketId}>
        <CardHeader>
          <CardTitle>{ticket.eventTitle}</CardTitle>
          <CardDescription>{formatDateTime(ticket.eventDate)} - {ticket.location || "TBA"}</CardDescription>
        </CardHeader>
        <CardContent className="flex items-center justify-between gap-3">
          <Badge variant="outline">{ticket.ticketCode}</Badge>
          <Badge><Ticket data-icon="inline-start" /> {formatStatus(ticket.status)}</Badge>
        </CardContent>
      </Card>
    ))}
  </ListPage>;
}

function FavoritesPage() {
  const [favorites, setFavorites] = useState<FavoriteResponse[]>([]);
  const load = useCallback(() => favoriteApi.mine().then(setFavorites).catch((error) => toast.error(getErrorMessage(error))), []);
  useEffect(() => { load(); }, [load]);
  async function remove(id: number) {
    await favoriteApi.toggle(id);
    load();
  }
  return <ListPage title="Favorites" empty="Chua co su kien yeu thich.">
    {favorites.map((favorite) => (
      <Card key={favorite.eventId}>
        <CardHeader>
          <CardTitle>{favorite.eventTitle}</CardTitle>
          <CardDescription>{formatDateTime(favorite.eventDate)} - {favorite.location || "TBA"}</CardDescription>
        </CardHeader>
        <CardContent className="flex justify-between gap-3">
          <Badge variant="secondary">{formatMoney(favorite.price)}</Badge>
          <Button variant="outline" onClick={() => remove(favorite.eventId)}><Heart data-icon="inline-start" /> Remove</Button>
        </CardContent>
      </Card>
    ))}
  </ListPage>;
}

function ProfilePage({ refreshProfile }: { refreshProfile: () => Promise<unknown> }) {
  const [fullName, setFullName] = useState("");
  const [avatar, setAvatar] = useState("");
  const [eventReminder, setEventReminder] = useState(true);

  useEffect(() => {
    userApi.profile().then((profile) => {
      setFullName(profile.fullName || "");
      setAvatar(profile.avatar || "");
    });
  }, []);

  async function saveProfile(event: FormEvent) {
    event.preventDefault();
    try {
      await userApi.updateProfile({ fullName, avatar });
      await refreshProfile();
      toast.success("Da cap nhat profile");
    } catch (error) {
      toast.error(getErrorMessage(error));
    }
  }

  async function saveReminder() {
    try {
      await userApi.updateReminder({ eventReminder });
      toast.success("Da cap nhat reminder");
    } catch (error) {
      toast.error(getErrorMessage(error));
    }
  }

  return (
    <div className="grid gap-4 md:grid-cols-2">
      <Card>
        <CardHeader><CardTitle>Profile</CardTitle></CardHeader>
        <CardContent>
          <form className="flex flex-col gap-4" onSubmit={saveProfile}>
            <div className="flex flex-col gap-2"><Label>Full name</Label><Input value={fullName} onChange={(event) => setFullName(event.target.value)} /></div>
            <div className="flex flex-col gap-2"><Label>Avatar URL</Label><Input value={avatar} onChange={(event) => setAvatar(event.target.value)} /></div>
            <Button>Save profile</Button>
          </form>
        </CardContent>
      </Card>
      <Card>
        <CardHeader><CardTitle>Reminder</CardTitle><CardDescription>Luu setting reminder trong backend MVP.</CardDescription></CardHeader>
        <CardContent className="flex flex-col gap-4">
          <label className="flex items-center gap-3 text-sm">
            <input type="checkbox" checked={eventReminder} onChange={(event) => setEventReminder(event.target.checked)} />
            Nhac toi truoc su kien
          </label>
          <Button onClick={saveReminder}>Save reminder</Button>
        </CardContent>
      </Card>
    </div>
  );
}

function ListPage({ title, empty, children }: { title: string; empty: string; children: React.ReactNode }) {
  const items = useMemo(() => React.Children.toArray(children), [children]);
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-bold">{title}</h1>
      {items.length === 0 ? <Alert><AlertDescription>{empty}</AlertDescription></Alert> : items}
    </div>
  );
}
