import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { ApiError, apiGet } from '../../services/api'
import type { OnboardingStatus } from '../../types'

/** Account overview: identity, verification state and logout. */
export default function ProfilePage() {
  const { email, logout } = useAuth()
  const navigate = useNavigate()
  const [status, setStatus] = useState<OnboardingStatus | null>(null)

  useEffect(() => {
    apiGet<OnboardingStatus>('/api/verification/status')
      .then(setStatus)
      .catch((err) => {
        if (err instanceof ApiError && err.status === 400) setStatus(null)
      })
  }, [])

  async function onLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="space-y-4">
      <section className="rounded-2xl bg-white p-6 shadow-sm">
        <h1 className="text-xl font-bold text-neutral-900">Profile</h1>
        <p className="mt-1 break-all text-sm text-neutral-600">{email}</p>
        <dl className="mt-4 space-y-2 text-sm">
          <div className="flex justify-between">
            <dt className="text-neutral-600">Verification</dt>
            <dd className="font-semibold">{status ? status.state.replace(/_/g, ' ') : '—'}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-neutral-600">FPO validated</dt>
            <dd className="font-semibold">{status?.fpoValidated ? '✓ Yes' : '○ Not yet'}</dd>
          </div>
        </dl>
        <Link
          to="/verify"
          className="mt-4 block rounded-xl border border-primary-700 px-4 py-3 text-center font-semibold text-primary-700"
        >
          Verification steps
        </Link>
        <button
          onClick={() => void onLogout()}
          className="mt-2 w-full rounded-xl bg-neutral-200 px-4 py-3 font-semibold text-neutral-800"
        >
          Log out
        </button>
      </section>
    </div>
  )
}
