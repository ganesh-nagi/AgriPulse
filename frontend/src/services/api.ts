/**
 * Minimal JSON API client. Base URL comes from VITE_API_URL (see .env.example).
 * JWT is read from localStorage; all business logic stays on the server.
 */

const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

function authHeaders(): HeadersInit {
  const token = localStorage.getItem('agripulse.token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

const SESSION_KEYS = ['agripulse.token', 'agripulse.refresh', 'agripulse.email', 'agripulse.role']

/** Expired/revoked access token: drop the dead session so route guards redirect to login. */
function clearExpiredSession(): void {
  SESSION_KEYS.forEach((k) => localStorage.removeItem(k))
  const path = window.location.pathname
  if (path !== '/login' && path !== '/register') {
    window.location.assign('/login')
  }
}

async function handle<T>(res: Response): Promise<T> {
  if (!res.ok) {
    if (res.status === 401) clearExpiredSession()
    let message = `Request failed (${res.status})`
    try {
      const body = await res.json()
      if (typeof body?.message === 'string') message = body.message
    } catch {
      // keep default message
    }
    throw new ApiError(res.status, message)
  }
  if (res.status === 204) return undefined as T
  return (await res.json()) as T
}

export async function apiGet<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, { headers: { ...authHeaders() } })
  return handle<T>(res)
}

export async function apiPost<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(body),
  })
  return handle<T>(res)
}

export async function apiPatch<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(body),
  })
  return handle<T>(res)
}

export async function apiPut<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(body),
  })
  return handle<T>(res)
}

export async function apiDelete<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    method: 'DELETE',
    headers: { ...authHeaders() },
  })
  return handle<T>(res)
}

export async function apiPostForm<T>(
  path: string,
  body: FormData,
): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${localStorage.getItem('agripulse.token') ?? ''}` },
    body,
  })
  return handle<T>(res)
}

export async function apiPostCancel<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
  })
  return handle<T>(res)
}
