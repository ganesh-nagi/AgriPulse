import { useCallback, useEffect, useState } from 'react'
import StateMessage from '../../components/StateMessage'
import StatusBadge from '../../components/StatusBadge'
import NetworkMap from '../../components/NetworkMap'
import { useFarmContext } from '../../hooks/useFarmContext'
import { t } from '../../i18n'
import {
  defaultWindow,
  getDemand,
  getMarkets,
  getPressure,
  getSnapshot,
} from '../../services/farmer'
import type {
  DemandEstimate,
  MarketNode,
  PressureAssessment,
  ResourceSnapshot,
} from '../../types'
import { demandStrength, marketCondition, resourceCondition } from '../../utils/display'
import { range } from '../../utils/format'

/**
 * Nearby markets: public node signals only. Per-market rows carry the
 * regional pressure, demand and resource condition — no buyer details,
 * no farmer identities.
 */
export default function MarketsPage() {
  const { region, cropId, cropName, loading: ctxLoading, error: ctxError, reload } =
    useFarmContext()
  const [markets, setMarkets] = useState<MarketNode[]>([])
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
      const [m, p, d, s] = await Promise.all([
        getMarkets(region),
        getPressure(cropId, region, from, to),
        getDemand(cropId, region, from, to),
        getSnapshot(region, cropId, to).catch(() => null),
      ])
      setMarkets(m)
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

  if (ctxLoading || (loading && markets.length === 0 && !failed))
    return <StateMessage kind="loading" message="" />
  if (ctxError)
    return <StateMessage kind="error" message={t('state.failed')} onRetry={() => void reload()} />
  if (!region || !cropId)
    return <StateMessage kind="empty" message={t('home.noFarmDetail')} />
  if (failed || !pressure || !demand)
    return <StateMessage kind="error" message={t('state.failed')} onRetry={() => void load()} />

  const strength = demandStrength(demand)
  const resources =
    snapshot != null ? resourceCondition(snapshot, pressure.effectiveSupplyTonnes) : null

  return (
    <div className="space-y-4">
      <section className="rounded-2xl bg-white p-6 shadow-sm">
        <p className="text-sm text-neutral-500">
          {region} · {cropName}
        </p>
        <h1 className="mt-1 text-xl font-bold text-neutral-900">{t('markets.title')}</h1>
      </section>

      <NetworkMap
        cropName={cropName}
        fallbackLat={markets.find((m) => m.latitude != null)?.latitude ?? null}
        fallbackLon={markets.find((m) => m.longitude != null)?.longitude ?? null}
      />

      {markets.length === 0 ? (
        <StateMessage kind="empty" message={t('markets.empty')} />
      ) : (
        <ul className="space-y-3">
          {markets.map((m) => (
            <li key={m.id} className="rounded-2xl bg-white p-5 shadow-sm">
              <div className="flex items-center justify-between gap-2">
                <h2 className="text-base font-bold text-neutral-900">{m.name}</h2>
                <StatusBadge band={pressure.band} />
              </div>
              <p className="mt-1 text-sm text-neutral-500">
                {range(m.absorptionMinTonnes, m.absorptionMaxTonnes)} (ESTIMATED)
              </p>
              <dl className="mt-3 space-y-2 text-sm">
                <div className="flex items-center justify-between">
                  <dt className="text-neutral-500">▦ {t('home.pressure')}</dt>
                  <dd className="font-semibold">{marketCondition(pressure.band)}</dd>
                </div>
                <div className="flex items-center justify-between">
                  <dt className="text-neutral-500">🤝 {t('home.demand')}</dt>
                  <dd className="font-semibold">{strength.label}</dd>
                </div>
                <div className="flex items-center justify-between">
                  <dt className="text-neutral-500">🚚 {t('markets.resources')}</dt>
                  <dd className="font-semibold">{resources ? resources.label : '—'}</dd>
                </div>
              </dl>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
