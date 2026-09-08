import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import StateMessage from '../../components/StateMessage'
import { useFarmContext } from '../../hooks/useFarmContext'
import { t } from '../../i18n'
import {
  defaultWindow,
  getMyReports,
  getPressure,
  getVerification,
} from '../../services/farmer'
import type { PressureAssessment } from '../../types'
import { importantReasons } from '../../utils/display'

interface Alert {
  icon: string
  title: string
  detail: string
  to: string
  action: string
}

/** Important, actionable messages only. Nothing informational lives here. */
export default function AlertsPage() {
  const { region, cropId, loading: ctxLoading, error: ctxError, reload } = useFarmContext()
  const [alerts, setAlerts] = useState<Alert[]>([])
  const [loading, setLoading] = useState(false)
  const [failed, setFailed] = useState(false)

  const load = useCallback(async () => {
    if (!region || !cropId) return
    setLoading(true)
    setFailed(false)
    try {
      const { from, to } = defaultWindow()
      const [pressure, verification, reports] = await Promise.all([
        getPressure(cropId, region, from, to).catch(() => null),
        getVerification().catch(() => null),
        getMyReports().catch(() => []),
      ])
      const next: Alert[] = []
      if (pressure) {
        for (const r of importantReasons(pressure as PressureAssessment).slice(0, 3)) {
          next.push({
            icon: r.icon,
            title: r.text,
            detail: '',
            to: '/markets',
            action: t('home.viewAll'),
          })
        }
      }
      if (verification && verification.state !== 'PROFILE_ACTIVE') {
        next.push({
          icon: '✓',
          title: 'Complete verification',
          detail: 'Verified reports carry more weight in your region.',
          to: '/verify',
          action: t('profile.verify'),
        })
      }
      const reviewCount = reports.filter((r) => r.trustLevel === 'REQUIRES_REVIEW').length
      if (reviewCount > 0) {
        next.push({
          icon: '◐',
          title: `${reviewCount} report${reviewCount > 1 ? 's need' : ' needs'} review`,
          detail: 'Check the details and update if needed.',
          to: '/report',
          action: t('nav.report'),
        })
      }
      setAlerts(next)
    } catch {
      setFailed(true)
    } finally {
      setLoading(false)
    }
  }, [region, cropId])

  useEffect(() => {
    void load()
  }, [load])

  if (ctxLoading || loading) return <StateMessage kind="loading" message="" />
  if (ctxError)
    return <StateMessage kind="error" message={t('state.failed')} onRetry={() => void reload()} />
  if (failed) return <StateMessage kind="error" message={t('state.failed')} onRetry={() => void load()} />

  return (
    <div className="space-y-4">
      <section className="rounded-2xl bg-white p-6 shadow-sm">
        <h1 className="text-xl font-bold text-neutral-900">{t('alerts.title')}</h1>
        {alerts.length === 0 ? (
          <p className="mt-2 text-sm text-neutral-500">{t('alerts.empty')}</p>
        ) : (
          <ul className="mt-3 space-y-3">
            {alerts.map((a, i) => (
              <li key={i} className="rounded-xl border border-neutral-200 p-4">
                <p className="font-semibold text-neutral-900">
                  <span aria-hidden="true">{a.icon} </span>
                  {a.title}
                </p>
                {a.detail && <p className="mt-1 text-sm text-neutral-600">{a.detail}</p>}
                <Link
                  to={a.to}
                  className="mt-2 inline-block rounded-lg bg-primary-700 px-4 py-2 text-sm font-semibold text-white"
                >
                  {a.action}
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
