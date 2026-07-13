import { clearAuth, getToken } from "./auth";
import type { ApiErrorBody } from "./types";

export class ApiError extends Error {
  status: number;
  errors?: ApiErrorBody["errors"];

  constructor(status: number, body: ApiErrorBody) {
    super(body.message || "Request failed");
    this.name = "ApiError";
    this.status = status;
    this.errors = body.errors;
  }
}

type RequestOptions = RequestInit & {
  auth?: boolean;
};

async function parseBody(response: Response) {
  if (response.status === 204) {
    return undefined;
  }
  const text = await response.text();
  if (!text) {
    return undefined;
  }
  return JSON.parse(text);
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers(options.headers);
  const token = getToken();

  if (!headers.has("Content-Type") && options.body) {
    headers.set("Content-Type", "application/json");
  }
  if (token && options.auth !== false) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(path, { ...options, headers });
  const body = await parseBody(response);

  if (!response.ok) {
    if (response.status === 401) {
      clearAuth();
      window.dispatchEvent(new CustomEvent("eventbooking:unauthorized"));
    }
    throw new ApiError(response.status, body || { message: response.statusText });
  }

  return body as T;
}

export function toQueryString(params: Record<string, string | number | boolean | undefined>) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== "") {
      query.set(key, String(value));
    }
  });
  const value = query.toString();
  return value ? `?${value}` : "";
}
