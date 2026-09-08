import { useCallback, useEffect, useState } from 'react'
import { ApiError, apiGet, apiPost } from '../../services/api'
import type { OnboardingStatus } from '../../types'

interface PhoneResponse {
  state: string
  demoCode: string
  note: string
}

const STEP_ORDER = [
  'ACCOUNT_CREATED',
  'PHONE_VERIFICATION_PENDING',
  'PHONE_VERIFIED',
  'IDENTITY_VERIFICATION_PENDING',
  'IDENTITY_VERIFIED',
  'PROFILE_ACTIVE',
]

function stepLabel(state: string): string {
  switch (state) {
    case 'ACCOUNT_CREATED':
      return 'Account created'
    case 'PHONE_VERIFICATION_PENDING':
      return 'Phone check pending'
    case 'PHONE_VERIFIED':
      return 'Phone verified'
    case 'IDENTITY_VERIFICATION_PENDING':
      return 'Identity check pending'
    case 'IDENTITY_VERIFIED':
      return 'Identity verified'
    case 'REQUIRES_REVIEW':
      return 'Under manual review'
    case 'PROFILE_ACTIVE':
      return 'Profile active'
    default:
      return state
  }
}

/** Farmer verification status + step actions. All checks run server-side. */
export default function VerificationPage() {
  const [status, setStatus] = useState<OnboardingStatus | null>(null)
  const [code, setCode] = useState('')
  const [demoCode, setDemoCode] = useState<string | null>(null)
  const [note, setNote] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const refresh = useCallback(async () => {
    try {
      setStatus(await apiGet<OnboardingStatus>('/api/verification/status'))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not load status')
    }
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  async function run<T>(fn: () => Promise<T>, after?: (result: T) => void) {
    try {
      setBusy(true)
      setError(null)
      const result = await fn()
      after?.(result)
      await refresh()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Action failed')
    } finally {
      setBusy(false)
    }
  }

  const state = status?.state ?? 'ACCOUNT_CREATED'
  const stepIndex = Math.max(
    0,
    STEP_ORDER.indexOf(state === 'REQUIRES_REVIEW' ? 'IDENTITY_VERIFICATION_PENDING' : state),
  )

  return (
    <div className="space-y-4">
      <section className="rounded-2xl bg-white p-6 shadow-sm">
        <h1 className="text-xl font-bold text-neutral-900">Verification</h1>
        <ol className="mt-4 space-y-2">
          {STEP_ORDER.map((step, i) => (
            <li key={step} className="flex items-center gap-3 text-sm">
              <span
                aria-hidden="true"
                className={`flex h-7 w-7 items-center justify-center rounded-full font-bold ${
                  i < stepIndex
                    ? 'bg-primary-700 text-white'
                    : i === stepIndex
                      ? 'border-2 border-primary-700 text-primary-700'
                      : 'bg-neutral-200 text-neutral-500'
                }`}
              >
                {i < stepIndex ? '✓' : i + 1}
              </span>
              <span className={i <= stepIndex ? 'font-semibold' : 'text-neutral-500'}>
                {stepLabel(step)}
              </span>
            </li>
          ))}
        </ol>
        {state === 'REQUIRES_REVIEW' && (
          <p className="mt-3 rounded-xl bg-yellow-50 p-3 text-sm text-yellow-800">
            ⏳ Your identity needs a manual review. You will be notified.
          </p>
        )}
      </section>

      <section className="space-y-3 rounded-2xl bg-white p-6 shadow-sm">
        <h2 className="font-bold text-neutral-900">Steps</h2>
        {(state === 'ACCOUNT_CREATED' || state === 'PHONE_VERIFICATION_PENDING') && (
          <button
            disabled={busy}
            onClick={() =>
              run(() => apiPost<PhoneResponse>('/api/verification/phone/request', {}), (r) => {
                setDemoCode(r.demoCode)
                setNote(r.note)
              })
            }
            className="w-full rounded-xl bg-primary-700 px-4 py-3 font-semibold text-white disabled:opacity-50"
          >
            {state === 'ACCOUNT_CREATED' ? 'Start phone check' : 'Resend code'}
          </button>
        )}
        {demoCode && (
          <p className="rounded-xl bg-neutral-100 p-3 text-sm text-neutral-700">
            📱 Demo code: <strong>{demoCode}</strong>
            {note && <span className="block text-xs text-neutral-500">{note}</span>}
          </p>
        )}
        {state === 'PHONE_VERIFICATION_PENDING' && (
          <form
            onSubmit={(e) => {
              e.preventDefault()
              void run(() => apiPost('/api/verification/phone/confirm', { code }), () =>
                setDemoCode(null),
              )
            }}
            className="flex gap-2"
          >
            <input
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="6-digit code"
              inputMode="numeric"
              className="min-w-0 flex-1 rounded-xl border border-neutral-300 px-4 py-3"
            />
            <button
              type="submit"
              disabled={busy}
              className="rounded-xl bg-primary-700 px-4 py-3 font-semibold text-white disabled:opacity-50"
            >
              Check
            </button>
          </form>
        )}
        {state === 'PHONE_VERIFIED' && (
          <button
            disabled={busy}
            onClick={() => run(() => apiPost('/api/verification/identity/request', {}))}
            className="w-full rounded-xl bg-primary-700 px-4 py-3 font-semibold text-white disabled:opacity-50"
          >
            Start identity check (demo)
          </button>
        )}
        {state === 'IDENTITY_VERIFICATION_PENDING' && (
          <button
            disabled={busy}
            onClick={() => run(() => apiPost('/api/verification/identity/confirm', {}))}
            className="w-full rounded-xl bg-primary-700 px-4 py-3 font-semibold text-white disabled:opacity-50"
          >
            Complete identity check
          </button>
        )}
        {status && (
          <dl className="space-y-1 pt-2 text-sm text-neutral-600">
            <div className="flex justify-between">
              <dt>FPO validated</dt>
              <dd>{status.fpoValidated ? '✓ Yes' : '○ Not yet'}</dd>
            </div>
            <div className="flex justify-between">
              <dt>Region consistent</dt>
              <dd>
                {status.regionConsistent == null
                  ? '—'
                  : status.regionConsistent
                    ? '✓ Yes'
                    : '✗ Mismatch'}
              </dd>
            </div>
          </dl>
        )}
        {error && (
          <p role="alert" className="text-sm text-red-700">
            {error}
          </p>
        )}
      </section>
    </div>
  )
}
