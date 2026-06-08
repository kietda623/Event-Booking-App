export type AuthResponse = {
  token: string;
  username: string;
};

export type LoginRequest = {
  username: string;
  password: string;
};

export type RegisterRequest = LoginRequest & {
  email?: string;
};

export type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
};

export type EventResponse = {
  id: number;
  title: string;
  description?: string | null;
  eventDate: string;
  location?: string | null;
  price?: number | null;
  availableTickets?: number | null;
};

export type EventRequest = {
  title: string;
  description?: string;
  eventDate: string;
  location?: string;
  price?: number;
  totalTickets?: number;
};

export type BookingRequest = {
  eventId: number;
  quantity?: number;
};

export type BookingResponse = {
  bookingId: number;
  eventId: number;
  eventTitle: string;
  quantity: number;
  totalPrice: number;
  bookingTime: string;
  status: string;
};

export type PaymentRequest = {
  bookingId: number;
  cardNumber?: string;
  expiry?: string;
  cvv?: string;
  method?: string;
};

export type PaymentResponse = {
  paymentId: number;
  bookingId: number;
  amount: number;
  status: string;
};

export type TicketResponse = {
  ticketId: number;
  ticketCode: string;
  eventId: number;
  eventTitle: string;
  eventDate: string;
  location?: string | null;
  quantity: number;
  status: string;
};

export type UserResponse = {
  id: number;
  username: string;
  fullName?: string | null;
  email?: string | null;
  avatar?: string | null;
  role: "USER" | "ADMIN" | string;
};

export type ProfileRequest = {
  fullName?: string;
  avatar?: string;
};

export type ReminderRequest = {
  eventReminder: boolean;
};

export type ReminderResponse = {
  eventReminder: boolean;
};

export type FavoriteResponse = {
  eventId: number;
  eventTitle: string;
  eventDate: string;
  location?: string | null;
  price?: number | null;
  favorited: boolean;
};

export type ApiErrorBody = {
  success?: false;
  message?: string;
  errors?: Array<{ field: string; message: string }>;
};

export type EventQuery = {
  search?: string;
  upcoming?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: "asc" | "desc";
};
