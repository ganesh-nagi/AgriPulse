import { useCallback, useEffect, useState } from 'react'
import StateMessage from '../components/StateMessage'
import StatusBadge from '../components/StatusBadge'
import { apiGet, apiPost } from '../services/api'
import type { AdminOverview, CropOption, DemandEstimate, PressureAssessment, RegionalSupply, ResourceSnapshot, ScenarioResult } from '../types'
import { range, percent } from '../utils/format'
import { t } from '../i18n'

function iso(d: Date){return d.toISOString().slice(0,10)}
function win(){ const n=new Date(); return {from: iso(new Date(n.getTime()-30*864e5)), to: iso(new Date(n.getTime()+60*864e5))}}

export default function AdminPage(){
  const [overview,setOverview]=useState<AdminOverview|null>(null)
  const [crops,setCrops]=useState<CropOption[]>([])
  const [region,setRegion]=useState('Nashik')
  const [cropId,setCropId]=useState<number|null>(null)
  const [from,setFrom]=useState(win().from)
  const [to,setTo]=useState(win().to)
  const [supply,setSupply]=useState<RegionalSupply[]>([])
  const [demand,setDemand]=useState<DemandEstimate|null>(null)
  const [pressure,setPressure]=useState<PressureAssessment|null>(null)
  const [snapshot,setSnapshot]=useState<ResourceSnapshot|null>(null)
  const [loading,setLoading]=useState(true)
  const [drillLoading,setDrillLoading]=useState(false)
  const [error,setError]=useState<string|null>(null)
  // simulator (server-side; read-only, deterministic)
  const [sSupply,setSSupply]=useState('0')
  const [sDemand,setSDemand]=useState('0')
  const [sStorage,setSStorage]=useState('')
  const [sTransport,setSTransport]=useState('')
  const [sProcessing,setSProcessing]=useState('')
  const [simResult,setSimResult]=useState<ScenarioResult|null>(null)
  const [simRunning,setSimRunning]=useState(false)
  const [simError,setSimError]=useState<string|null>(null)

  const loadOverview = useCallback(async()=>{
    setLoading(true); setError(null)
    try{
      const [ov, cropList] = await Promise.all([
        apiGet<AdminOverview>('/api/admin/overview'),
        apiGet<CropOption[]>('/api/crops').catch(()=>[]),
      ])
      setOverview(ov); setCrops(cropList); if(cropList[0]&&!cropId) setCropId(cropList[0].id)
    }catch(e:unknown){ setError(e instanceof Error? e.message : t('state.failed'))}
    finally{ setLoading(false)}
  },[cropId])
  useEffect(()=>{void loadOverview()},[loadOverview])

  const loadRegion = useCallback(async()=>{
    if(!region || !cropId) return
    setDrillLoading(true)
    try{
      const [sup, dem, press, snap] = await Promise.all([
        apiGet<RegionalSupply[]>(`/api/public/regional/supply?region=${encodeURIComponent(region)}`).catch(()=>[]),
        apiGet<DemandEstimate>(`/api/demand/estimate?cropId=${cropId}&region=${encodeURIComponent(region)}&from=${from}&to=${to}`).catch(()=>null),
        apiGet<PressureAssessment>(`/api/pressure?cropId=${cropId}&region=${encodeURIComponent(region)}&from=${from}&to=${to}`).catch(()=>null),
        apiGet<ResourceSnapshot>(`/api/resources/snapshot?region=${encodeURIComponent(region)}&cropId=${cropId}&date=${to}`).catch(()=>null),
      ])
      setSupply(sup); setDemand(dem); setPressure(press); setSnapshot(snap)
    }catch{ /* keep */ }
    finally{ setDrillLoading(false)}
  },[region,cropId,from,to])
  useEffect(()=>{ if(crops.length) void loadRegion()},[loadRegion, crops.length])

  if(loading) return <StateMessage kind="loading" message="" />
  if(error && !overview) return <StateMessage kind="error" message={error} onRetry={()=>void loadOverview()} />

  async function runSimulation(){
    if(!cropId || !region) return
    setSimRunning(true); setSimError(null)
    try{
      const num = (v: string) => v==='' ? null : Number(v)
      const res = await apiPost<ScenarioResult>('/api/scenarios/simulate', {
        label: 'admin-whatif',
        input: {
          cropId, region, windowStart: from, windowEnd: to,
          supplyDeltaTonnes: Number(sSupply||0),
          demandDeltaTonnes: Number(sDemand||0),
          storageOverrideTonnes: num(sStorage),
          transportOverrideTonnes: num(sTransport),
          processingOverrideTonnes: num(sProcessing),
        },
      })
      setSimResult(res)
    }catch(e:unknown){ setSimError(e instanceof Error? e.message : 'Simulation failed') }
    finally{ setSimRunning(false) }
  }

  return (
    <div className="space-y-4">
      <section className="rounded-2xl bg-white p-5 shadow-sm">
        <h1 className="text-lg font-bold text-neutral-900">{t('admin.title')}</h1>
        <p className="text-xs text-neutral-500">Read-only regional intelligence · audit-logged admin actions</p>
        {overview && (
          <div className="mt-3 grid grid-cols-2 gap-3 md:grid-cols-4">
            <div className="rounded-xl bg-neutral-50 p-3"><p className="text-xs text-neutral-500">Users</p><p className="text-xl font-bold">{overview.totalUsers}</p><p className="text-xs text-neutral-500">{Object.entries(overview.usersByRole).map(([k,v])=>`${k}:${v}`).join(' · ')}</p></div>
            <div className="rounded-xl bg-neutral-50 p-3"><p className="text-xs text-neutral-500">Reports</p><p className="text-xs mt-1">{Object.entries(overview.reportsByStatus).map(([k,v])=><span key={k} className="mr-2">{k} {v}</span>)}</p><p className="text-xs text-neutral-500 mt-1">avg trust {overview.averageTrustScore.toFixed(1)}%</p></div>
            <div className="rounded-xl bg-neutral-50 p-3"><p className="text-xs text-neutral-500">Open demand</p><p className="text-xl font-bold">{overview.openRequirements}</p></div>
            <div className="rounded-xl bg-neutral-50 p-3"><p className="text-xs text-neutral-500">Resources</p><p className="text-sm font-bold">{overview.storageFacilities} stores · {overview.transportResources} transports</p></div>
          </div>
        )}
      </section>

      <section className="rounded-2xl bg-white p-5 shadow-sm">
        <div className="flex flex-wrap gap-2">
          <input value={region} onChange={e=>setRegion(e.target.value)} placeholder={t('common.region')} className="min-w-[120px] flex-1 rounded-xl border border-neutral-300 px-3 py-2 text-sm" />
          <select value={cropId??''} onChange={e=>setCropId(Number(e.target.value)||null)} className="rounded-xl border border-neutral-300 bg-white px-3 py-2 text-sm"><option value="">Crop</option>{crops.map(c=><option key={c.id} value={c.id}>{c.name}</option>)}</select>
          <input type="date" value={from} onChange={e=>setFrom(e.target.value)} className="rounded-xl border border-neutral-300 px-3 py-2 text-sm" />
          <input type="date" value={to} onChange={e=>setTo(e.target.value)} className="rounded-xl border border-neutral-300 px-3 py-2 text-sm" />
          <button onClick={()=>void loadRegion()} disabled={drillLoading} className="rounded-xl bg-primary-700 px-4 py-2 text-sm font-semibold text-white disabled:bg-neutral-300">{drillLoading?'…':'Load'}</button>
        </div>

        <div className="mt-4 grid gap-3 md:grid-cols-2">
          <div className="rounded-xl border border-neutral-200 p-4">
            <h3 className="text-sm font-bold text-neutral-900">Regional supply <span className="text-xs font-normal text-neutral-500">(ESTIMATED)</span></h3>
            {supply.length===0? <p className="mt-2 text-sm text-neutral-500">No signals.</p> : (
              <ul className="mt-2 space-y-1">{supply.map(s=>(
                <li key={s.cropName} className="flex justify-between text-sm"><span>{s.cropName} · {s.reportCount} reports</span><span className="font-semibold">{range(s.quantityMinTotal,s.quantityMaxTotal)}</span></li>
              ))}</ul>
            )}
            {supply.length>0 && <p className="mt-2 text-xs text-neutral-500">Avg confidence {Math.round(supply.reduce((a,b)=>a+b.averageConfidence,0)/supply.length)}%</p>}
          </div>
          <div className="rounded-xl border border-neutral-200 p-4">
            <h3 className="text-sm font-bold">Estimated demand</h3>
            {!demand? <p className="mt-2 text-sm text-neutral-500">No demand data.</p> : (
              <div className="mt-2 text-sm">
                <p className="font-bold">{range(demand.estimatedMinTonnes, demand.estimatedMaxTonnes)} <span className="text-xs text-neutral-500">{demand.dataSource} · {Math.round(demand.confidence)}%</span></p>
                <p className="text-xs text-neutral-500">Confirmed {demand.confirmedDemandTonnes.toFixed(1)} t · {demand.confirmedBuyerCount} buyers</p>
                <p className="text-xs text-neutral-500">Observed {range(demand.absorptionMinTonnes, demand.absorptionMaxTonnes)}</p>
              </div>
            )}
          </div>
          <div className="rounded-xl border border-neutral-200 p-4">
            <h3 className="text-sm font-bold flex items-center gap-2">Market pressure {pressure && <StatusBadge band={pressure.band} />}</h3>
            {!pressure? <p className="mt-2 text-sm text-neutral-500">No pressure.</p> : (
              <div className="mt-2">
                <p className="text-sm">{pressure.effectiveSupplyTonnes.toFixed(1)} t effective · {pressure.estimatedMinTonnes.toFixed(0)}–{pressure.estimatedMaxTonnes.toFixed(0)} t est.</p>
                <ul className="mt-2 space-y-1">{pressure.reasons.slice(0,3).map((r,i)=><li key={i} className="text-xs text-neutral-600"><span className="font-semibold">{r.component}:</span> {r.message}</li>)}</ul>
              </div>
            )}
          </div>
          <div className="rounded-xl border border-neutral-200 p-4">
            <h3 className="text-sm font-bold">Resource condition</h3>
            {!snapshot? <p className="mt-2 text-sm text-neutral-500">No snapshot.</p> : (
              <dl className="mt-2 grid grid-cols-2 gap-2 text-sm">
                <div className="rounded-lg bg-neutral-50 p-2"><dt className="text-xs text-neutral-500">Storage</dt><dd className="font-bold">{snapshot.storageAvailableTonnes.toFixed(1)} t</dd></div>
                <div className="rounded-lg bg-neutral-50 p-2"><dt className="text-xs text-neutral-500">Transport</dt><dd className="font-bold">{snapshot.transportAvailableTonnes.toFixed(1)} t</dd></div>
                <div className="rounded-lg bg-neutral-50 p-2"><dt className="text-xs text-neutral-500">Processing</dt><dd className="font-bold">{snapshot.processingAvailableTonnes.toFixed(1)} t</dd></div>
                <div className="rounded-lg bg-neutral-50 p-2"><dt className="text-xs text-neutral-500">Market absorp.</dt><dd className="font-bold">{snapshot.marketAbsorptionMinTonnes.toFixed(0)}–{snapshot.marketAbsorptionMaxTonnes.toFixed(0)} t</dd></div>
              </dl>
            )}
          </div>
        </div>
      </section>

      <section className="rounded-2xl bg-white p-5 shadow-sm">
        <h2 className="font-bold text-neutral-900">{t('admin.anomaly')}</h2>
        {!overview ? <p className="mt-2 text-sm text-neutral-500">No data.</p> : (
          <div className="mt-2 text-sm">
            <p>Average trust {percent(overview.averageTrustScore)} — {overview.averageTrustScore < 50 ? 'Low trust: review flags likely' : overview.averageTrustScore < 70 ? 'Moderate trust' : 'Healthy trust'}</p>
            <p className="mt-1 text-xs text-neutral-600">Anomaly signals from pressure engine: {pressure? pressure.reasons.filter(r=>r.component==='SUPPLY' && /above|shortage/i.test(r.message)).map(r=>r.message).join(' · ') || 'none flagged' : '—'}</p>
            <p className="mt-1 text-xs text-neutral-500">Status breakdown: {Object.entries(overview.reportsByStatus).map(([k,v])=>`${k} ${v}`).join(' · ') || 'none'}</p>
          </div>
        )}
      </section>

      <section className="rounded-2xl bg-white p-5 shadow-sm">
        <h2 className="font-bold text-neutral-900">{t('admin.simulator')}</h2>
        <p className="text-xs text-neutral-500">Server what-if — never modifies production. Deterministic.</p>
        <div className="mt-3 grid grid-cols-2 gap-3">
          <label className="text-sm">Supply Δ (t)<input value={sSupply} onChange={e=>setSSupply(e.target.value)} type="number" className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2" /></label>
          <label className="text-sm">Demand Δ (t)<input value={sDemand} onChange={e=>setSDemand(e.target.value)} type="number" className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2" /></label>
          <label className="text-sm">Storage avail (t)<input value={sStorage} onChange={e=>setSStorage(e.target.value)} placeholder="keep production" className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2" /></label>
          <label className="text-sm">Transport avail (t)<input value={sTransport} onChange={e=>setSTransport(e.target.value)} placeholder="keep production" className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2" /></label>
          <label className="text-sm col-span-2">Processing avail (t)<input value={sProcessing} onChange={e=>setSProcessing(e.target.value)} placeholder="keep production" className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2" /></label>
        </div>
        <div className="mt-3 flex gap-2">
          <button onClick={()=>{ setSSupply('0'); setSDemand('0'); setSStorage(''); setSTransport(''); setSProcessing(''); setSimResult(null) }} className="rounded-xl border border-neutral-300 px-4 py-2 text-sm font-semibold">{t('admin.reset')}</button>
          <button onClick={()=>void runSimulation()} disabled={simRunning || !cropId} className="rounded-xl bg-primary-700 px-4 py-2 text-sm font-semibold text-white disabled:bg-neutral-300">{simRunning? '…' : t('admin.run')}</button>
          <div className="flex-1" />
        </div>
        {simError && <p role="alert" className="mt-2 text-sm text-red-600">{simError}</p>}
        {simResult && (
          <div className="mt-4 rounded-xl bg-neutral-50 p-4">
            <p className="flex items-center gap-2 text-sm">Baseline <StatusBadge band={simResult.baseline.band} /> {simResult.baseline.effectiveSupplyTonnes.toFixed(1)} t vs {simResult.baseline.estimatedMinTonnes.toFixed(0)}–{simResult.baseline.estimatedMaxTonnes.toFixed(0)} t</p>
            <p className="mt-2 flex items-center gap-2 text-sm">Scenario <StatusBadge band={simResult.scenario.band} /> {simResult.scenario.effectiveSupplyTonnes.toFixed(1)} t vs {simResult.scenario.estimatedMinTonnes.toFixed(0)}–{simResult.scenario.estimatedMaxTonnes.toFixed(0)} t</p>
            <ul className="mt-2 space-y-1">{simResult.scenario.reasons.slice(0,4).map((r,i)=><li key={i} className="text-xs text-neutral-600"><span className="font-semibold">{r.component}:</span> {r.message}</li>)}</ul>
          </div>
        )}
      </section>
    </div>
  )
}
