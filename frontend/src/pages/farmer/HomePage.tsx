import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import InfoRow from '../../components/InfoRow'
import StateMessage from '../../components/StateMessage'
import StatusBadge from '../../components/StatusBadge'
import { useFarmContext } from '../../hooks/useFarmContext'
import { t } from '../../i18n'
import { defaultWindow, getDemand, getPressure, getSnapshot } from '../../services/farmer'
import type { DemandEstimate, PressureAssessment, ResourceSnapshot } from '../../types'
import { demandStrength, importantReasons, marketCondition, supplyCondition } from '../../utils/display'
import { range } from '../../utils/format'

/**
 * Farmer home: what a farmer needs today. Regional aggregates only —
 * no identities, no exact private quantities, no buyer details.
 */
export default function HomePage() {
  const { region, crops, cropId, cropName, setCropId, loading: ctxLoading, error: ctxError, reload } =
    useFarmContext()
  const [pressure, setPressure] = useState<PressureAssessment | null>(null)
  const [demand, setDemand] = useState<DemandEstimate | null>(null)
  const [snapshot, setSnapshot] = useState<ResourceSnapshot | null>(null)
  const [loading, setLoading] = useState(false)
  const [failed, setFailed] = useState(false)

  const load = useCallback(async () => {
    if (!region || !cropId) return
    setLoading(true)
    setFailed(false)
    try {
      const { from, to } = defaultWindow()
      const [p, d, s] = await Promise.all([
        getPressure(cropId, region, from, to),
        getDemand(cropId, region, from, to),
        getSnapshot(region, cropId, to).catch(() => null),
      ])
      setPressure(p)
      setDemand(d)
      setSnapshot(s)
    } catch {
      setFailed(true)
    } finally {
      setLoading(false)
    }
  }, [region, cropId])

  useEffect(() => {
    void load()
  }, [load])

  if (ctxLoading) return <StateMessage kind="loading" message="" />
  if (ctxError)
    return <StateMessage kind="error" message={t('state.failed')} onRetry={() => void reload()} />
  if (!region || !cropId) {
    return (
      <section className="rounded-2xl bg-white p-6 text-center shadow-sm">
        <p className="text-2xl" aria-hidden="true">
          ⌂
        </p>
        <h1 className="mt-2 text-xl font-bold text-neutral-900">{t('home.noFarm')}</h1>
        <p className="mt-1 text-sm text-neutral-600">{t('home.noFarmDetail')}</p>
        <Link
          to="/onboarding"
          className="mt-4 block rounded-xl bg-primary-700 px-4 py-3 font-semibold text-white"
        >
          {t('home.addFarm')}
        </Link>
      </section>
    )
  }
  if (loading && !pressure) return <StateMessage kind="loading" message="" />
  if (failed || !pressure || !demand)
    return <StateMessage kind="error" message={t('state.failed')} onRetry={() => void load()} />

  const supply = supplyCondition(pressure)
  const strength = demandStrength(demand)
  const alerts = importantReasons(pressure).slice(0, 2)

  return (
    <div className="space-y-4">
      <section className="rounded-2xl bg-white p-6 shadow-sm">
        <div className="flex items-center justify-between gap-2">
          <div>
            <p className="text-sm text-neutral-500">{region}</p>
            <h1 className="text-xl font-bold uppercase text-neutral-900">{cropName}</h1>
          </div>
          {crops.length > 1 && (
            <select
              aria-label="Crop"
              value={cropId}
              onChange={(e) => setCropId(Number(e.target.value))}
              className="rounded-xl border border-neutral-300 bg-white px-3 py-2 text-sm"
            >
              {crops.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          )}
        </div>

        <div className="mt-2 divide-y divide-neutral-100">
          <InfoRow
            icon="🌾"
            label={`${t('home.supply')} (${demand.dataSource})`}
            value={`${supply.label} · ${range(demand.estimatedMinTonnes, demand.estimatedMaxTonnes)}`}
            hint={supply.hint}
          />
          <InfoRow
            icon="🤝"
            label={t('home.demand')}
            value={strength.label}
            hint={strength.hint}
          />
          <div className="flex items-center gap-3 py-3">
            <span
              aria-hidden="true"
              className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-neutral-100 text-xl"
            >
              ▦
            </span>
            <div className="min-w-0 flex-1">
              <p className="text-sm text-neutral-500">{t('home.pressure')}</p>
              <p className="text-base font-bold text-neutral-900">
                {marketCondition(pressure.band)}
              </p>
            </div>
            <StatusBadge band={pressure.band} />
          </div>
        </div>
      </section>

      {alerts.length > 0 && (
        <section className="rounded-2xl bg-white p-6 shadow-sm">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-bold text-neutral-900">{t('home.alerts')}</h2>
            <Link to="/alerts" className="text-sm font-semibold text-primary-700">
              {t('home.viewAll')}
            </Link>
          </div>
          <ul className="mt-2 space-y-2">
            {alerts.map((a, i) => (
              <li key={i} className="flex items-start gap-2 text-sm text-neutral-700">
                <span aria-hidden="true">{a.icon}</span>
                <span>{a.text}</span>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  )
}
