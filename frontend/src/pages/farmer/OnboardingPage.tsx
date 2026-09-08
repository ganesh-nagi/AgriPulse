import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { ApiError, apiPost } from '../../services/api'

const schema = z.object({
  fullName: z.string().min(2, 'Enter your name'),
  phone: z.string().optional(),
  region: z.string().min(2, 'Enter your region'),
})

/** Step 1 of farmer onboarding: create the farmer profile. */
export default function OnboardingPage() {
  const navigate = useNavigate()
  const [fullName, setFullName] = useState('')
  const [phone, setPhone] = useState('')
  const [region, setRegion] = useState('')
  const [error, setError] = useState<string | null>(null)

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault()
    const parsed = schema.safeParse({ fullName, phone, region })
    if (!parsed.success) {
      setError(parsed.error.issues[0]?.message ?? 'Check your input')
      return
    }
    try {
      setError(null)
      await apiPost('/api/verification/profile', parsed.data)
      navigate('/verify', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not save profile')
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-neutral-100 p-6">
      <form
        onSubmit={onSubmit}
        className="w-full max-w-sm space-y-4 rounded-2xl bg-white p-6 shadow-sm"
      >
        <h1 className="text-xl font-bold text-neutral-900">Your farmer profile</h1>
        <p className="text-sm text-neutral-600">Step 1 of verification. Demo only — no real ID needed.</p>
        <label className="block">
          <span className="text-sm text-neutral-600">Full name</span>
          <input
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            className="mt-1 w-full rounded-xl border border-neutral-300 px-4 py-3"
            autoComplete="name"
          />
        </label>
        <label className="block">
          <span className="text-sm text-neutral-600">Phone (optional)</span>
          <input
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            className="mt-1 w-full rounded-xl border border-neutral-300 px-4 py-3"
            autoComplete="tel"
          />
        </label>
        <label className="block">
          <span className="text-sm text-neutral-600">Region / village cluster</span>
          <input
            value={region}
            onChange={(e) => setRegion(e.target.value)}
            className="mt-1 w-full rounded-xl border border-neutral-300 px-4 py-3"
          />
        </label>
        {error && (
          <p role="alert" className="text-sm text-red-700">
            {error}
          </p>
        )}
        <button
          type="submit"
          className="w-full rounded-xl bg-primary-700 px-4 py-3 font-semibold text-white"
        >
          Continue
        </button>
      </form>
    </div>
  )
}
