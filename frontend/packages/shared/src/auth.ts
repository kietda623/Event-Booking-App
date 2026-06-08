import type { AuthResponse, UserResponse } from "./types";

const TOKEN_KEY = "eventbooking.token";
const USERNAME_KEY = "eventbooking.username";

export type AuthSession = {
  token: string | null;
  username: string | null;
  profile: UserResponse | null;
};

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function saveAuth(response: AuthResponse) {
  localStorage.setItem(TOKEN_KEY, response.token);
  localStorage.setItem(USERNAME_KEY, response.username);
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USERNAME_KEY);
}

export function getStoredUsername() {
  return localStorage.getItem(USERNAME_KEY);
}

export function getInitialSession(): AuthSession {
  return {
    token: getToken(),
    username: getStoredUsername(),
    profile: null
  };
}

export function isAdmin(profile: UserResponse | null) {
  return profile?.role === "ADMIN";
}
