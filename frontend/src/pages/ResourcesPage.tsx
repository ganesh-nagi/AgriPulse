import { useCallback, useEffect, useState } from 'react'
import StateMessage from '../components/StateMessage'
import { apiGet } from '../services/api'
import type { StorageFacility, TransportResource } from '../types'
import { tonnes } from '../utils/format'
import { t } from '../i18n'

function role(): string | null { return localStorage.getItem('agripulse.role') }

function occupancyPct(f: StorageFacility){ return f.capacityTonnes>0 ? Math.round(f.occupiedTonnes/f.capacityTonnes*100) : 0 }

export default function ResourcesPage(){
  const r = role()
  const isStorage = r==='STORAGE_OPERATOR'
  const isTransport = r==='TRANSPORTER'
  // if neither (e.g. admin preview) show both
  const showStorage = isStorage || (!isStorage && !isTransport)
  const showTransport = isTransport || (!isStorage && !isTransport)

  const [storages,setStorages]=useState<StorageFacility[]>([])
  const [transports,setTransports]=useState<TransportResource[]>([])
  const [loading,setLoading]=useState(true)
  const [error,setError]=useState<string|null>(null)

  const load = useCallback(async()=>{
    setLoading(true); setError(null)
    try{
      const promises: Promise<unknown>[] = []
      if(showStorage) promises.push(apiGet<StorageFacility[]>('/api/storage/mine').then(setStorages).catch(()=>setStorages([])))
      else setStorages([])
      if(showTransport) promises.push(apiGet<TransportResource[]>('/api/transport/mine').then(setTransports).catch(()=>setTransports([])))
      else setTransports([])
      await Promise.all(promises)
    } catch(e:unknown){ setError(e instanceof Error? e.message : t('state.failed'))}
    finally{ setLoading(false)}
  },[showStorage,showTransport])

  useEffect(()=>{ void load()},[load])

  if(loading) return <StateMessage kind="loading" message="" />
  if(error) return <StateMessage kind="error" message={error} onRetry={()=>void load()} />

  const totalCap = storages.reduce((a,b)=>a+b.capacityTonnes,0)
  const totalOcc = storages.reduce((a,b)=>a+b.occupiedTonnes,0)
  const totalAvail = storages.reduce((a,b)=>a+b.availableTonnes,0)
  const totalFleet = transports.reduce((a,b)=>a+b.capacityTonnes,0)
  const availFleet = transports.filter(t=>t.status==='AVAILABLE').reduce((a,b)=>a+b.capacityTonnes,0)

  return (
    <div className="space-y-4">
      {showStorage && (
        <>
          <section className="rounded-2xl bg-white p-5 shadow-sm">
            <h1 className="text-lg font-bold text-neutral-900">{t('storage.title')}</h1>
            <div className="mt-3 grid grid-cols-3 gap-3">
              <div className="rounded-xl bg-neutral-50 p-3 text-center"><p className="text-xs text-neutral-500">{t('common.capacity')}</p><p className="text-lg font-bold">{tonnes(totalCap)}</p></div>
              <div className="rounded-xl bg-neutral-50 p-3 text-center"><p className="text-xs text-neutral-500">Occupied</p><p className="text-lg font-bold">{tonnes(totalOcc)}</p></div>
              <div className="rounded-xl bg-green-50 p-3 text-center"><p className="text-xs text-green-700">{t('common.available')}</p><p className="text-lg font-bold text-green-800">{tonnes(totalAvail)}</p></div>
            </div>
            {storages.length>0 && <p className="mt-2 text-xs text-neutral-500">{storages.length} facilities · utilization {totalCap? Math.round(totalOcc/totalCap*100):0}%</p>}
          </section>
          <section className="rounded-2xl bg-white p-5 shadow-sm">
            <h2 className="font-bold text-neutral-900">Facilities</h2>
            {storages.length===0? <p className="mt-2 text-sm text-neutral-500">{t('storage.empty')}</p> : (
              <ul className="mt-3 space-y-3">
                {storages.map(f=>(
                  <li key={f.id} className="rounded-xl border border-neutral-200 p-4">
                    <div className="flex items-center justify-between">
                      <h3 className="font-semibold text-neutral-900">{f.name}</h3>
                      <span className="text-xs text-neutral-500">{f.region}</span>
                    </div>
                    <div className="mt-2 h-2 rounded-full bg-neutral-100">
                      <div className="h-2 rounded-full bg-primary-600" style={{width: `${occupancyPct(f)}%`}} />
                    </div>
                    <dl className="mt-2 grid grid-cols-2 gap-2 text-sm">
                      <div><dt className="text-xs text-neutral-500">Capacity</dt><dd className="font-medium">{tonnes(f.capacityTonnes)}</dd></div>
                      <div><dt className="text-xs text-neutral-500">Available</dt><dd className="font-bold text-green-700">{tonnes(f.availableTonnes)}</dd></div>
                    </dl>
                    <p className="mt-2 text-xs text-neutral-500">Crops: {(f as unknown as Record<string,unknown>).cropCompatibility as string || '— any —'} · Status balanced by occupancy</p>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}

      {showTransport && (
        <>
          <section className="rounded-2xl bg-white p-5 shadow-sm">
            <h1 className="text-lg font-bold text-neutral-900">{t('transport.title')}</h1>
            <div className="mt-3 grid grid-cols-3 gap-3">
              <div className="rounded-xl bg-neutral-50 p-3 text-center"><p className="text-xs text-neutral-500">Fleet</p><p className="text-lg font-bold">{tonnes(totalFleet)}</p></div>
              <div className="rounded-xl bg-green-50 p-3 text-center"><p className="text-xs text-green-700">Available</p><p className="text-lg font-bold text-green-800">{tonnes(availFleet)}</p></div>
              <div className="rounded-xl bg-neutral-50 p-3 text-center"><p className="text-xs text-neutral-500">Utilization</p><p className="text-lg font-bold">{transports.length? Math.round((totalFleet-availFleet)/Math.max(totalFleet,1)*100):0}%</p></div>
            </div>
            <p className="mt-2 text-xs text-neutral-500">{transports.length} resources · {transports.filter(t=>t.status==='AVAILABLE').length} available</p>
          </section>
          <section className="rounded-2xl bg-white p-5 shadow-sm">
            <h2 className="font-bold text-neutral-900">Fleet</h2>
            {transports.length===0? <p className="mt-2 text-sm text-neutral-500">{t('transport.empty')}</p> : (
              <ul className="mt-3 space-y-2">
                {transports.map(tr=>(
                  <li key={tr.id} className="flex items-center justify-between rounded-xl border border-neutral-200 p-3">
                    <div>
                      <p className="font-semibold">{tonnes(tr.capacityTonnes)} · {tr.originRegion} → {tr.destRegion ?? '—'}</p>
                      <p className="text-xs text-neutral-500">{tr.status}</p>
                    </div>
                    <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${tr.status==='AVAILABLE'?'bg-green-50 text-green-700 border border-green-200':'bg-neutral-100 text-neutral-600'}`}>{tr.status}</span>
                  </li>
                ))}
              </ul>
            )}
            <div className="mt-4 rounded-xl bg-blue-50 p-3">
              <p className="text-sm font-semibold text-blue-900">{t('transport.demand')}</p>
              <p className="text-xs text-blue-700">Available capacity vs regional pressure — keep fleet ready when pressure is HIGH/CRITICAL and storage is limited.</p>
            </div>
          </section>
        </>
      )}
    </div>
  )
}
