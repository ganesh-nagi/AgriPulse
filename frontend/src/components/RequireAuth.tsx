import { Navigate, Outlet, useLocation } from 'react-router-dom'
import type { UserRole } from '../types'

function readAuth(): { token: string | null; role: UserRole | null } {
  return {
    token: localStorage.getItem('agripulse.token'),
    role: localStorage.getItem('agripulse.role') as UserRole | null,
  }
}

/** Redirects to login when no token is stored. */
export function RequireAuth() {
  const location = useLocation()
  const { token } = readAuth()
  if (!token) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <Outlet />
}

interface RoleProps {
  roles: UserRole[]
}

/**
 * Client-side role gate for UX only. Every API enforces roles server-side,
 * so tampering with localStorage cannot grant access.
 */
export function RequireRole({ roles }: RoleProps) {
  const { role } = readAuth()
  if (!role || !roles.includes(role)) return <Navigate to="/" replace />
  return <Outlet />
}
