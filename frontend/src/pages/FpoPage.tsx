import { useCallback, useEffect, useState } from 'react'
import StateMessage from '../components/StateMessage'
import StatusBadge from '../components/StatusBadge'
import { apiGet, apiPost } from '../services/api'
import type { CropOption, DemandEstimate, FpoMember, PressureAssessment, RegionalSupply, ResourceSnapshot } from '../types'
import { range } from '../utils/format'
import { t } from '../i18n'

function iso(d: Date){ return d.toISOString().slice(0,10)}
function windowDef(){ const n=new Date(); return {from: iso(new Date(n.getTime()-30*864e5)), to: iso(new Date(n.getTime()+60*864e5))}}

export default function FpoPage(){
  const [region,setRegion]=useState('')
  const [crops,setCrops]=useState<CropOption[]>([])
  const [cropId,setCropId]=useState<number|null>(null)
  const [members,setMembers]=useState<FpoMember[]>([])
  const [supply,setSupply]=useState<RegionalSupply[]>([])
  const [demand,setDemand]=useState<DemandEstimate|null>(null)
  const [pressure,setPressure]=useState<PressureAssessment|null>(null)
  const [snapshot,setSnapshot]=useState<ResourceSnapshot|null>(null)
  const [loading,setLoading]=useState(false)
  const [error,setError]=useState<string|null>(null)
  const [validating,setValidating]=useState<number|null>(null)

  const loadCrops = useCallback(async()=>{
    try{ const list=await apiGet<CropOption[]>('/api/crops'); setCrops(list); if(list[0]&&!cropId) setCropId(list[0].id)}catch{/* */ }
  },[cropId])
  useEffect(()=>{void loadCrops()},[loadCrops])

  const loadRegion = useCallback(async()=>{
    if(!region || !cropId) return
    setLoading(true); setError(null)
    try{
      const {from,to}=windowDef()
      const [m, sup, dem, press, snap] = await Promise.all([
        apiGet<FpoMember[]>(`/api/fpo/members?region=${encodeURIComponent(region)}`),
        apiGet<RegionalSupply[]>(`/api/public/regional/supply?region=${encodeURIComponent(region)}`).catch(()=>[]),
        apiGet<DemandEstimate>(`/api/demand/estimate?cropId=${cropId}&region=${encodeURIComponent(region)}&from=${from}&to=${to}`).catch(()=>null),
        apiGet<PressureAssessment>(`/api/pressure?cropId=${cropId}&region=${encodeURIComponent(region)}&from=${from}&to=${to}`).catch(()=>null),
        apiGet<ResourceSnapshot>(`/api/resources/snapshot?region=${encodeURIComponent(region)}&cropId=${cropId}&date=${to}`).catch(()=>null),
      ])
      setMembers(m); setSupply(sup); setDemand(dem); setPressure(press); setSnapshot(snap)
    }catch(e:unknown){ setError(e instanceof Error? e.message : t('state.failed')) }
    finally{ setLoading(false)}
  },[region,cropId])

  async function validate(id:number){
    setValidating(id)
    try{ await apiPost('/api/fpo/validations', { farmerUserId: id }); await loadRegion() } catch(e:unknown){ setError(e instanceof Error? e.message:'Validate failed')}
    finally{ setValidating(null)}
  }

  return (
    <div className="space-y-4">
      <section className="rounded-2xl bg-white p-5 shadow-sm">
        <h1 className="text-lg font-bold text-neutral-900">{t('fpo.title')}</h1>
        <p className="text-xs text-neutral-500">Aggregated member view — no phone, no coordinates, no private quantities</p>
        <div className="mt-3 flex gap-2">
          <input value={region} onChange={e=>setRegion(e.target.value)} placeholder={`${t('common.region')} e.g. Nashik`} className="flex-1 rounded-xl border border-neutral-300 px-3 py-2 text-sm" />
          <select value={cropId??''} onChange={e=>setCropId(Number(e.target.value)||null)} className="rounded-xl border border-neutral-300 bg-white px-3 py-2 text-sm">
            <option value="">Crop</option>
            {crops.map(c=><option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
          <button onClick={()=>void loadRegion()} className="rounded-xl bg-primary-700 px-4 py-2 text-sm font-semibold text-white">Load</button>
        </div>
      </section>

      {loading ? <StateMessage kind="loading" message="" /> : (
        <>
          <div className="grid gap-4 md:grid-cols-2">
            <section className="rounded-2xl bg-white p-5 shadow-sm">
              <h2 className="font-bold text-neutral-900">{t('fpo.supply')}</h2>
              {supply.length===0? <p className="mt-2 text-sm text-neutral-500">{t('buyer.noSupply')}</p> : (
                <ul className="mt-3 space-y-2">
                  {supply.map(s=>(
                    <li key={s.cropName} className="rounded-xl border border-neutral-200 p-3">
                      <p className="font-semibold">{s.cropName} · {s.reportCount} reports</p>
                      <p className="text-sm">{range(s.quantityMinTotal,s.quantityMaxTotal)} <span className="text-xs text-neutral-500">avg {Math.round(s.averageConfidence)}%</span></p>
                    </li>
                  ))}
                </ul>
              )}
            </section>
            <section className="rounded-2xl bg-white p-5 shadow-sm">
              <h2 className="font-bold text-neutral-900">{t('fpo.demand')}</h2>
              {!demand? <p className="mt-2 text-sm text-neutral-500">Select region and crop.</p> : (
                <div className="mt-2 text-sm">
                  <p className="font-semibold">{range(demand.estimatedMinTonnes, demand.estimatedMaxTonnes)} <span className="text-xs text-neutral-500">({demand.dataSource} · {Math.round(demand.confidence)}%)</span></p>
                  <p className="text-xs text-neutral-600">Confirmed {demand.confirmedDemandTonnes.toFixed(1)} t from {demand.confirmedBuyerCount} buyers · Observed {range(demand.absorptionMinTonnes, demand.absorptionMaxTonnes)}</p>
                  <ul className="mt-2 space-y-1">{demand.provenance.map((p,i)=><li key={i} className="text-xs text-neutral-500"><span className="font-semibold">{p.signal}</span> {p.sources.join('/')} — {p.detail}</li>)}</ul>
                </div>
              )}
            </section>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <section className="rounded-2xl bg-white p-5 shadow-sm">
              <h2 className="font-bold text-neutral-900">{t('buyer.pressure')}</h2>
              {!pressure? <p className="mt-2 text-sm text-neutral-500">No pressure data.</p> : (
                <div className="mt-2">
                  <div className="flex items-center gap-2"><StatusBadge band={pressure.band} /><span className="font-semibold text-sm">{pressure.band}</span></div>
                  <ul className="mt-2 space-y-1">{pressure.reasons.slice(0,4).map((r,i)=><li key={i} className="text-xs text-neutral-600"><span className="font-semibold">{r.component}:</span> {r.message}</li>)}</ul>
                </div>
              )}
            </section>
            <section className="rounded-2xl bg-white p-5 shadow-sm">
              <h2 className="font-bold text-neutral-900">{t('fpo.resources')}</h2>
              {!snapshot? <p className="mt-2 text-sm text-neutral-500">No resource data.</p> : (
                <dl className="mt-2 grid grid-cols-2 gap-3 text-sm">
                  <div className="rounded-xl bg-neutral-50 p-3"><dt className="text-xs text-neutral-500">Storage avail</dt><dd className="font-bold">{snapshot.storageAvailableTonnes.toFixed(1)} t</dd></div>
                  <div className="rounded-xl bg-neutral-50 p-3"><dt className="text-xs text-neutral-500">Transport avail</dt><dd className="font-bold">{snapshot.transportAvailableTonnes.toFixed(1)} t</dd></div>
                  <div className="rounded-xl bg-neutral-50 p-3"><dt className="text-xs text-neutral-500">Processing avail</dt><dd className="font-bold">{snapshot.processingAvailableTonnes.toFixed(1)} t</dd></div>
                  <div className="rounded-xl bg-neutral-50 p-3"><dt className="text-xs text-neutral-500">Market absorp.</dt><dd className="font-bold">{snapshot.marketAbsorptionMinTonnes.toFixed(0)}–{snapshot.marketAbsorptionMaxTonnes.toFixed(0)} t</dd></div>
                </dl>
              )}
            </section>
          </div>

          <section className="rounded-2xl bg-white p-5 shadow-sm">
            <h2 className="font-bold text-neutral-900">{t('fpo.members')} <span className="font-normal text-neutral-500">· {members.length}</span></h2>
            {members.length===0? <p className="mt-2 text-sm text-neutral-500">{t('fpo.emptyMembers')}</p> : (
              <div className="mt-3 overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="text-xs text-neutral-500"><tr><th className="py-2">Farmer</th><th>Verification</th><th>Reports</th><th>Validated</th><th></th></tr></thead>
                  <tbody>
                    {members.map(m=>(
                      <tr key={m.farmerUserId} className="border-t border-neutral-100">
                        <td className="py-2 font-medium">{m.farmerName}<br/><span className="text-xs text-neutral-500">{m.region}</span></td>
                        <td className="text-xs">{m.verificationStatus}<br/><span className="text-neutral-500">{m.onboardingState}</span></td>
                        <td className="text-center">{m.reportCount}</td>
                        <td className="text-center">{m.fpoValidated ? '✓' : '—'}</td>
                        <td><button disabled={m.fpoValidated || validating===m.farmerUserId} onClick={()=>void validate(m.farmerUserId)} className="rounded-lg bg-primary-700 px-3 py-1.5 text-xs font-semibold text-white disabled:bg-neutral-300">{validating===m.farmerUserId? '…':'Validate'}</button></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </>
      )}
      {error && <p role="alert" className="text-sm text-red-600">{error}</p>}
    </div>
  )
}
