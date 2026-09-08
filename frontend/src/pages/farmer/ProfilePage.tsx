import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { LANGUAGES, getLanguage, setLanguage, t } from '../../i18n'
import { getMyReports, getVerification } from '../../services/farmer'
import type { OnboardingStatus } from '../../types'

/** Account overview: verification, reports, privacy, language, logout. */
export default function ProfilePage() {
  const { email, logout } = useAuth()
  const navigate = useNavigate()
  const [status, setStatus] = useState<OnboardingStatus | null>(null)
  const [reportCount, setReportCount] = useState<number | null>(null)
  const [lang, setLang] = useState(getLanguage())

  useEffect(() => {
    getVerification()
      .then(setStatus)
      .catch(() => setStatus(null))
    getMyReports()
      .then((list) => setReportCount(list.length))
      .catch(() => setReportCount(null))
  }, [])

  async function onLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  function onLanguage(code: string) {
    setLanguage(code)
    setLang(getLanguage())
  }

  return (
    <div className="space-y-4">
      <section className="rounded-2xl bg-white p-6 shadow-sm">
        <h1 className="text-xl font-bold text-neutral-900">{t('profile.title')}</h1>
        <p className="mt-1 break-all text-sm text-neutral-600">{email}</p>

        <div className="mt-4 flex items-center gap-3">
          <span
            aria-hidden="true"
            className="flex h-11 w-11 items-center justify-center rounded-xl bg-neutral-100 text-xl"
          >
            ✓
          </span>
          <div className="flex-1">
            <p className="text-sm text-neutral-500">{t('profile.verification')}</p>
            <p className="font-bold text-neutral-900">
              {status ? status.state.replace(/_/g, ' ') : '—'}
            </p>
          </div>
          <Link to="/verify" className="text-sm font-semibold text-primary-700">
            {t('profile.verify')}
          </Link>
        </div>

        <Link to="/report" className="mt-2 flex items-center gap-3">
          <span
            aria-hidden="true"
            className="flex h-11 w-11 items-center justify-center rounded-xl bg-neutral-100 text-xl"
          >
            +
          </span>
          <div className="flex-1">
            <p className="text-sm text-neutral-500">{t('profile.reports')}</p>
            <p className="font-bold text-neutral-900">{reportCount ?? '—'}</p>
          </div>
        </Link>
      </section>

      <section className="rounded-2xl bg-white p-6 shadow-sm">
        <div className="flex items-center gap-3">
          <span
            aria-hidden="true"
            className="flex h-11 w-11 items-center justify-center rounded-xl bg-neutral-100 text-xl"
          >
            🔒
          </span>
          <div>
            <h2 className="font-bold text-neutral-900">{t('profile.privacy')}</h2>
            <p className="mt-1 text-sm text-neutral-600">{t('profile.privacyDetail')}</p>
          </div>
        </div>
      </section>

      <section className="rounded-2xl bg-white p-6 shadow-sm">
        <label className="block">
          <span className="text-sm text-neutral-500">🌐 {t('profile.language')}</span>
          <select
            value={lang}
            onChange={(e) => onLanguage(e.target.value)}
            className="mt-1 w-full rounded-xl border border-neutral-300 bg-white px-4 py-3"
          >
            {LANGUAGES.map((l) => (
              <option key={l.code} value={l.code}>
                {l.label}
              </option>
            ))}
          </select>
        </label>
        <button
          onClick={() => void onLogout()}
          className="mt-4 w-full rounded-xl bg-neutral-200 px-4 py-3 font-semibold text-neutral-800"
        >
          {t('profile.logout')}
        </button>
      </section>
    </div>
  )
}
