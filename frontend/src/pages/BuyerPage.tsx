import { useCallback, useEffect, useState } from 'react'
import StateMessage from '../components/StateMessage'
import StatusBadge from '../components/StatusBadge'
import { apiGet, apiPost } from '../services/api'
import type { BuyerRequirement, CropOption, DemandEstimate, PressureAssessment, RegionalSupply } from '../types'
import { range, tonnes } from '../utils/format'
import { t } from '../i18n'

function iso(d: Date) { return d.toISOString().slice(0, 10) }
function defaultWindow() {
  const n = new Date()
  return { from: iso(new Date(n.getTime() - 30*864e5)), to: iso(new Date(n.getTime() + 60*864e5)) }
}

export default function BuyerPage() {
  const [requirements, setRequirements] = useState<BuyerRequirement[]>([])
  const [supply, setSupply] = useState<RegionalSupply[]>([])
  const [pressure, setPressure] = useState<PressureAssessment | null>(null)
  const [demand, setDemand] = useState<DemandEstimate | null>(null)
  const [crops, setCrops] = useState<CropOption[]>([])
  const [region, setRegion] = useState('')
  const [cropId, setCropId] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [formQty, setFormQty] = useState('')
  const [formDate, setFormDate] = useState(iso(new Date(Date.now()+14*864e5)))
  const [formQuality, setFormQuality] = useState('Standard')
  const [submitting, setSubmitting] = useState(false)

  const load = useCallback(async () => {
    setLoading(true); setError(null)
    try {
      const [reqs, cropList] = await Promise.all([
        apiGet<BuyerRequirement[]>('/api/buyer/requirements/mine'),
        apiGet<CropOption[]>('/api/crops').catch(()=>[] as CropOption[])
      ])
      setRequirements(reqs)
      setCrops(cropList)
      const r = reqs[0]?.region || region
      const c = cropList[0]?.id ?? null
      if (r && !region) setRegion(r)
      if (c && !cropId) setCropId(c)
      const effRegion = r || region
      const effCrop = c ?? cropId
      if (effRegion && effCrop) {
        const { from, to } = defaultWindow()
        const [sup, press, dem] = await Promise.all([
          apiGet<RegionalSupply[]>(`/api/public/regional/supply?region=${encodeURIComponent(effRegion)}`).catch(()=>[]),
          apiGet<PressureAssessment>(`/api/pressure?cropId=${effCrop}&region=${encodeURIComponent(effRegion)}&from=${from}&to=${to}`).catch(()=>null),
          apiGet<DemandEstimate>(`/api/demand/estimate?cropId=${effCrop}&region=${encodeURIComponent(effRegion)}&from=${from}&to=${to}`).catch(()=>null),
        ])
        setSupply(sup); setPressure(press); setDemand(dem)
      }
    } catch (e: unknown) { setError(e instanceof Error ? e.message : t('state.failed')) }
    finally { setLoading(false) }
  }, []) // region/cropId intentionally not deps to avoid loop; manual refresh handles

  useEffect(()=>{ void load() }, [load])

  const refreshRegion = useCallback(async () => {
    if (!region || !cropId) return
    try {
      const { from, to } = defaultWindow()
      const [sup, press, dem] = await Promise.all([
        apiGet<RegionalSupply[]>(`/api/public/regional/supply?region=${encodeURIComponent(region)}`),
        apiGet<PressureAssessment>(`/api/pressure?cropId=${cropId}&region=${encodeURIComponent(region)}&from=${from}&to=${to}`).catch(()=>null),
        apiGet<DemandEstimate>(`/api/demand/estimate?cropId=${cropId}&region=${encodeURIComponent(region)}&from=${from}&to=${to}`).catch(()=>null),
      ])
      setSupply(sup); setPressure(press); setDemand(dem)
    } catch { /* keep previous */ }
  }, [region, cropId])

  useEffect(()=>{ if(region && cropId) void refreshRegion() }, [region, cropId, refreshRegion])

  async function onCreate(e: React.FormEvent) {
    e.preventDefault()
    if (!cropId || !region) { setError('Select crop and region'); return }
    setSubmitting(true)
    try {
      await apiPost('/api/buyer/requirements', { cropId, quantityTonnes: Number(formQty), quality: formQuality, requiredDate: formDate, region })
      setShowForm(false); setFormQty('')
      await load()
    } catch (err: unknown) { setError(err instanceof Error ? err.message : 'Could not create') }
    finally { setSubmitting(false) }
  }

  if (loading) return <StateMessage kind="loading" message="" />
  if (error && requirements.length===0) return <StateMessage kind="error" message={error} onRetry={()=>void load()} />

  const supplyForCrop = cropId ? supply.filter(s=> crops.find(c=>c.id===cropId)?.name===s.cropName) : supply
  const displaySupply = supplyForCrop.length? supplyForCrop : supply

  return (
    <div className="space-y-4">
      <section className="rounded-2xl bg-white p-5 shadow-sm">
        <div className="flex items-center justify-between">
          <h1 className="text-lg font-bold text-neutral-900">{t('buyer.title')}</h1>
          <button onClick={()=>setShowForm(v=>!v)} className="rounded-xl bg-primary-700 px-4 py-2 text-sm font-semibold text-white">{t('buyer.create')}</button>
        </div>
        <div className="mt-3 flex flex-wrap gap-2">
          <input value={region} onChange={e=>setRegion(e.target.value)} placeholder={t('common.region')} className="min-w-[140px] flex-1 rounded-xl border border-neutral-300 px-3 py-2 text-sm" />
          <select value={cropId ?? ''} onChange={e=>setCropId(Number(e.target.value)||null)} className="rounded-xl border border-neutral-300 bg-white px-3 py-2 text-sm">
            <option value="">Crop</option>
            {crops.map(c=><option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>
        {showForm && (
          <form onSubmit={onCreate} className="mt-4 rounded-xl border border-neutral-200 p-4 space-y-3">
            <div className="grid grid-cols-2 gap-3">
              <label className="text-sm">Qty (t)<input value={formQty} onChange={e=>setFormQty(e.target.value)} type="number" min="0.1" step="0.1" required className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2" /></label>
              <label className="text-sm">Needed by<input value={formDate} onChange={e=>setFormDate(e.target.value)} type="date" required className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2" /></label>
            </div>
            <label className="text-sm block">Quality
              <select value={formQuality} onChange={e=>setFormQuality(e.target.value)} className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2"><option>Standard</option><option>Good</option><option>Excellent</option></select>
            </label>
            <button disabled={submitting} className="w-full rounded-xl bg-green-700 px-4 py-2.5 font-semibold text-white disabled:bg-neutral-300">{submitting?'Saving…':'Post requirement'}</button>
          </form>
        )}
      </section>

      <section className="rounded-2xl bg-white p-5 shadow-sm">
        <h2 className="font-bold text-neutral-900">{t('buyer.requirements')} <span className="font-normal text-neutral-500">· {requirements.length}</span></h2>
        {requirements.length===0 ? <p className="mt-2 text-sm text-neutral-500">{t('buyer.emptyReq')}</p> : (
          <ul className="mt-3 divide-y divide-neutral-100">
            {requirements.map(r=>(
              <li key={r.id} className="flex items-center justify-between py-3">
                <div>
                  <p className="font-semibold text-neutral-900">{r.cropName} · {tonnes(r.quantityTonnes)}</p>
                  <p className="text-xs text-neutral-500">{r.region} · {r.requiredDate} · {r.dataSource}</p>
                </div>
                <span className="rounded-full bg-neutral-100 px-2.5 py-1 text-xs font-medium text-neutral-700">{r.status}</span>
              </li>
            ))}
          </ul>
        )}
      </section>

      <div className="grid gap-4 md:grid-cols-2">
        <section className="rounded-2xl bg-white p-5 shadow-sm">
          <h2 className="font-bold text-neutral-900">{t('buyer.supply')}</h2>
          <p className="text-xs text-neutral-500">Aggregated regional totals — no farmer identities</p>
          {displaySupply.length===0 ? <p className="mt-3 text-sm text-neutral-500">{t('buyer.noSupply')}</p> : (
            <ul className="mt-3 space-y-2">
              {displaySupply.map(s=>(
                <li key={s.cropName} className="rounded-xl border border-neutral-200 p-3">
                  <p className="font-semibold">{s.cropName}</p>
                  <p className="text-sm">{range(s.quantityMinTotal, s.quantityMaxTotal)} <span className="text-xs text-neutral-500">({s.reportCount} reports · {Math.round(s.averageConfidence)}% avg confidence · {s.dataSource})</span></p>
                </li>
              ))}
            </ul>
          )}
        </section>
        <section className="rounded-2xl bg-white p-5 shadow-sm">
          <h2 className="font-bold text-neutral-900">{t('buyer.pressure')}</h2>
          {!pressure ? <p className="mt-2 text-sm text-neutral-500">Select region and crop to see pressure.</p> : (
            <div className="mt-2">
              <div className="flex items-center gap-2"><StatusBadge band={pressure.band} /><span className="text-sm font-semibold">{pressure.band}</span></div>
              <p className="mt-2 text-sm text-neutral-600">{pressure.effectiveSupplyTonnes.toFixed(1)} t effective supply · {pressure.estimatedMinTonnes.toFixed(0)}–{pressure.estimatedMaxTonnes.toFixed(0)} t demand</p>
              <ul className="mt-2 space-y-1">
                {pressure.reasons.slice(0,3).map((r,i)=><li key={i} className="text-xs text-neutral-600"><span className="font-semibold">{r.component}:</span> {r.message}</li>)}
              </ul>
              {demand && <p className="mt-2 text-xs text-neutral-500">Demand confidence {Math.round(demand.confidence)}% · {demand.provenance.map(p=>p.signal+':'+p.sources.join('/')).join(' · ')}</p>}
            </div>
          )}
        </section>
      </div>
      {error && <p role="alert" className="text-sm text-red-600">{error}</p>}
    </div>
  )
}
