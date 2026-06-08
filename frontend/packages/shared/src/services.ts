import { apiRequest, toQueryString } from "./apiClient";
import type {
  AuthResponse,
  BookingRequest,
  BookingResponse,
  EventQuery,
  EventRequest,
  EventResponse,
  FavoriteResponse,
  LoginRequest,
  PageResponse,
  PaymentRequest,
  PaymentResponse,
  ProfileRequest,
  RegisterRequest,
  ReminderRequest,
  ReminderResponse,
  TicketResponse,
  UserResponse
} from "./types";

export const authApi = {
  register: (request: RegisterRequest) =>
    apiRequest<AuthResponse>("/api/auth/register", {
      method: "POST",
      auth: false,
      body: JSON.stringify(request)
    }),
  login: (request: LoginRequest) =>
    apiRequest<AuthResponse>("/api/auth/login", {
      method: "POST",
      auth: false,
      body: JSON.stringify(request)
    })
};

export const eventApi = {
  list: (query: EventQuery = {}) =>
    apiRequest<PageResponse<EventResponse>>(`/api/events${toQueryString(query)}`, { auth: false }),
  get: (id: number) => apiRequest<EventResponse>(`/api/events/${id}`, { auth: false }),
  create: (request: EventRequest) =>
    apiRequest<EventResponse>("/api/events", { method: "POST", body: JSON.stringify(request) }),
  update: (id: number, request: EventRequest) =>
    apiRequest<EventResponse>(`/api/events/${id}`, { method: "PUT", body: JSON.stringify(request) }),
  remove: (id: number) => apiRequest<void>(`/api/events/${id}`, { method: "DELETE" })
};

export const bookingApi = {
  create: (request: BookingRequest) =>
    apiRequest<BookingResponse>("/api/bookings", { method: "POST", body: JSON.stringify(request) }),
  mine: () => apiRequest<BookingResponse[]>("/api/bookings/my"),
  cancel: (id: number) => apiRequest<BookingResponse>(`/api/bookings/${id}/cancel`, { method: "POST" })
};

export const paymentApi = {
  pay: (request: PaymentRequest) =>
    apiRequest<PaymentResponse>("/api/payments", { method: "POST", body: JSON.stringify(request) })
};

export const ticketApi = {
  mine: () => apiRequest<TicketResponse[]>("/api/tickets")
};

export const userApi = {
  profile: () => apiRequest<UserResponse>("/api/users/profile"),
  updateProfile: (request: ProfileRequest) =>
    apiRequest<UserResponse>("/api/users/profile", { method: "PUT", body: JSON.stringify(request) }),
  updateReminder: (request: ReminderRequest) =>
    apiRequest<ReminderResponse>("/api/users/reminders", { method: "PUT", body: JSON.stringify(request) })
};

export const favoriteApi = {
  mine: () => apiRequest<FavoriteResponse[]>("/api/favorites"),
  toggle: (eventId: number) => apiRequest<FavoriteResponse>(`/api/favorites/${eventId}`, { method: "POST" })
};
