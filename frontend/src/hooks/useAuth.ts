import { useCallback, useState } from 'react'
import { apiPost } from '../services/api'
import type { AuthResponse, UserRole } from '../types'

const TOKEN_KEY = 'agripulse.token'
const REFRESH_KEY = 'agripulse.refresh'
const EMAIL_KEY = 'agripulse.email'
const ROLE_KEY = 'agripulse.role'

/** Auth state backed by localStorage. Roles come from the server, never the UI. */
export function useAuth() {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY))
  const [email, setEmail] = useState<string | null>(() => localStorage.getItem(EMAIL_KEY))
  const [role, setRole] = useState<UserRole | null>(
    () => (localStorage.getItem(ROLE_KEY) as UserRole | null),
  )

  const save = useCallback((auth: AuthResponse) => {
    localStorage.setItem(TOKEN_KEY, auth.token)
    localStorage.setItem(REFRESH_KEY, auth.refreshToken)
    localStorage.setItem(EMAIL_KEY, auth.email)
    localStorage.setItem(ROLE_KEY, auth.role)
    setToken(auth.token)
    setEmail(auth.email)
    setRole(auth.role)
  }, [])

  const clear = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_KEY)
    localStorage.removeItem(EMAIL_KEY)
    localStorage.removeItem(ROLE_KEY)
    setToken(null)
    setEmail(null)
    setRole(null)
  }, [])

  const login = useCallback(
    async (emailInput: string, password: string) => {
      const auth = await apiPost<AuthResponse>('/api/auth/login', {
        email: emailInput,
        password,
      })
      save(auth)
      return auth
    },
    [save],
  )

  const register = useCallback(
    async (emailInput: string, password: string, roleInput: UserRole) => {
      const auth = await apiPost<AuthResponse>('/api/auth/register', {
        email: emailInput,
        password,
        role: roleInput,
      })
      save(auth)
      return auth
    },
    [save],
  )

  const logout = useCallback(async () => {
    try {
      await apiPost('/api/auth/logout', {})
    } catch {
      // Server logout is best-effort; local session always clears.
    }
    clear()
  }, [clear])

  return { token, email, role, login, register, logout }
}
