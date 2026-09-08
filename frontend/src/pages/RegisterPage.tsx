import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { useAuth } from '../hooks/useAuth'
import { ApiError } from '../services/api'
import type { UserRole } from '../types'

const ROLES: { value: UserRole; label: string }[] = [
  { value: 'FARMER', label: 'Farmer' },
  { value: 'BUYER', label: 'Buyer' },
  { value: 'FPO', label: 'FPO' },
  { value: 'STORAGE_OPERATOR', label: 'Storage operator' },
  { value: 'TRANSPORTER', label: 'Transporter' },
]

const schema = z.object({
  email: z.string().email('Enter a valid email'),
  password: z
    .string()
    .min(8, 'At least 8 characters')
    .regex(/[A-Za-z]/, 'Needs a letter')
    .regex(/[0-9]/, 'Needs a digit'),
  role: z.enum(['FARMER', 'BUYER', 'FPO', 'STORAGE_OPERATOR', 'TRANSPORTER']),
})

/** Registration with role selection. ADMIN is never offered (server rejects it). */
export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<UserRole>('FARMER')
  const [error, setError] = useState<string | null>(null)

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault()
    const parsed = schema.safeParse({ email, password, role })
    if (!parsed.success) {
      setError(parsed.error.issues[0]?.message ?? 'Check your input')
      return
    }
    try {
      setError(null)
      const auth = await register(parsed.data.email, parsed.data.password, parsed.data.role)
      navigate(auth.role === 'FARMER' ? '/onboarding' : '/', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Registration failed')
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-neutral-100 p-6">
      <form
        onSubmit={onSubmit}
        className="w-full max-w-sm space-y-4 rounded-2xl bg-white p-6 shadow-sm"
      >
        <h1 className="text-xl font-bold text-primary-700">Join AgriPulse</h1>
        <label className="block">
          <span className="text-sm text-neutral-600">Email</span>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="mt-1 w-full rounded-xl border border-neutral-300 px-4 py-3"
            autoComplete="email"
          />
        </label>
        <label className="block">
          <span className="text-sm text-neutral-600">Password (8+ chars, letter + digit)</span>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="mt-1 w-full rounded-xl border border-neutral-300 px-4 py-3"
            autoComplete="new-password"
          />
        </label>
        <fieldset>
          <legend className="text-sm text-neutral-600">I am a…</legend>
          <div className="mt-1 grid grid-cols-1 gap-2">
            {ROLES.map((r) => (
              <label
                key={r.value}
                className={`flex min-h-[52px] cursor-pointer items-center gap-3 rounded-xl border px-4 ${
                  role === r.value ? 'border-primary-700 bg-primary-50' : 'border-neutral-300'
                }`}
              >
                <input
                  type="radio"
                  name="role"
                  checked={role === r.value}
                  onChange={() => setRole(r.value)}
                  className="h-5 w-5 accent-green-700"
                />
                <span className="font-medium">{r.label}</span>
              </label>
            ))}
          </div>
        </fieldset>
        {error && (
          <p role="alert" className="text-sm text-red-700">
            {error}
          </p>
        )}
        <button
          type="submit"
          className="w-full rounded-xl bg-primary-700 px-4 py-3 font-semibold text-white"
        >
          Create account
        </button>
        <p className="text-center text-sm text-neutral-600">
          Have an account?{' '}
          <Link to="/login" className="font-semibold text-primary-700">
            Log in
          </Link>
        </p>
      </form>
    </div>
  )
}
