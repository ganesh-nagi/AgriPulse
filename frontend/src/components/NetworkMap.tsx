import { useEffect, useRef, useState } from 'react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { t } from '../i18n'
import { getNearby } from '../services/farmer'
import type { GeoKind, GeoNode } from '../types'

const KIND_META: Record<GeoKind, { icon: string; color: string }> = {
  MARKET: { icon: '▦', color: '#15803d' },
  STORAGE: { icon: '▤', color: '#1d4ed8' },
  PROCESSING: { icon: '⚙', color: '#b45309' },
}

const RADII = [25, 50, 100, 250]

interface Props {
  cropName: string | null
  fallbackLat: number | null
  fallbackLon: number | null
}

function markerIcon(node: GeoNode): L.DivIcon {
  const meta = KIND_META[node.kind]
  return L.divIcon({
    className: 'agripulse-marker',
    html: `<div style="display:flex;flex-direction:column;align-items:center;gap:0">
      <div style="width:34px;height:34px;border-radius:9999px;background:#fff;border:2px solid ${meta.color};display:flex;align-items:center;justify-content:center;font-size:17px;line-height:1">${meta.icon}</div>
      <div style="margin-top:2px;max-width:110px;background:rgba(255,255,255,.92);border:1px solid #e5e5e5;border-radius:6px;padding:0 5px;font-size:10px;font-weight:600;color:#171717;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${node.name}</div>
    </div>`,
    iconSize: [110, 58],
    iconAnchor: [55, 34],
    popupAnchor: [0, -34],
  })
}

function userIcon(): L.DivIcon {
  return L.divIcon({
    className: 'agripulse-user',
    html: `<div style="width:18px;height:18px;border-radius:9999px;background:#1d4ed8;border:3px solid #fff;box-shadow:0 1px 4px rgba(0,0,0,.35)"></div>`,
    iconSize: [18, 18],
    iconAnchor: [9, 9],
  })
}

/**
 * Network map: nearby public infrastructure on OpenStreetMap tiles.
 * Only zone/market-level nodes; never farmer locations. Clicking a
 * marker opens a concise popup with distance and state.
 */
export default function NetworkMap({ cropName, fallbackLat, fallbackLon }: Props) {
  const containerRef = useRef<HTMLDivElement>(null)
  const mapRef = useRef<L.Map | null>(null)
  const layerRef = useRef<L.LayerGroup | null>(null)
  const [center, setCenter] = useState<{ lat: number; lon: number } | null>(null)
  const [located, setLocated] = useState(false)
  const [radiusKm, setRadiusKm] = useState(100)
  const [kinds, setKinds] = useState<Set<GeoKind>>(
    new Set<GeoKind>(['MARKET', 'STORAGE', 'PROCESSING']),
  )
  const [nodes, setNodes] = useState<GeoNode[]>([])
  const [failed, setFailed] = useState(false)

  const KIND_LABEL: Record<GeoKind, string> = {
    MARKET: t('map.market'),
    STORAGE: t('map.storage'),
    PROCESSING: t('map.processing'),
  }

  // Locate once: browser geolocation, else region fallback.
  useEffect(() => {
    let cancelled = false
    const fallback =
      fallbackLat != null && fallbackLon != null ? { lat: fallbackLat, lon: fallbackLon } : null
    if (!('geolocation' in navigator)) {
      if (fallback && !cancelled) setCenter(fallback)
      return
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        if (!cancelled) {
          setCenter({ lat: pos.coords.latitude, lon: pos.coords.longitude })
          setLocated(true)
        }
      },
      () => {
        if (!cancelled && fallback) setCenter(fallback)
      },
      { timeout: 8000, maximumAge: 300000 },
    )
    return () => {
      cancelled = true
    }
  }, [fallbackLat, fallbackLon])

  // Fetch nodes when center / filters change.
  useEffect(() => {
    if (!center) return
    let cancelled = false
    setFailed(false)
    getNearby(center.lat, center.lon, radiusKm, [...kinds], cropName ?? undefined)
      .then((list) => {
        if (!cancelled) setNodes(list)
      })
      .catch(() => {
        if (!cancelled) setFailed(true)
      })
    return () => {
      cancelled = true
    }
  }, [center, radiusKm, kinds, cropName])

  // Init map once; refresh markers on data change.
  useEffect(() => {
    if (!containerRef.current || !center) return
    if (!mapRef.current) {
      mapRef.current = L.map(containerRef.current, { zoomControl: true }).setView(
        [center.lat, center.lon],
        10,
      )
      L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 18,
        attribution: '&copy; OpenStreetMap contributors',
      }).addTo(mapRef.current)
      layerRef.current = L.layerGroup().addTo(mapRef.current)
    } else {
      mapRef.current.setView([center.lat, center.lon], mapRef.current.getZoom())
    }
    const layer = layerRef.current
    if (!layer) return
    layer.clearLayers()
    L.marker([center.lat, center.lon], { icon: userIcon(), keyboard: false })
      .bindPopup(t('map.you'))
      .addTo(layer)
    for (const node of nodes) {
      L.marker([node.latitude, node.longitude], { icon: markerIcon(node) })
        .bindPopup(
          `<b>${node.name}</b><br/>${node.region} · ${node.distanceKm.toFixed(1)} km<br/>${node.summary}`,
        )
        .addTo(layer)
    }
    return () => {
      // Map instance persists; layers are rebuilt above.
    }
  }, [center, nodes])

  useEffect(() => {
    return () => {
      mapRef.current?.remove()
      mapRef.current = null
    }
  }, [])

  function toggleKind(kind: GeoKind) {
    setKinds((prev) => {
      const next = new Set(prev)
      if (next.has(kind)) {
        if (next.size > 1) next.delete(kind)
      } else {
        next.add(kind)
      }
      return next
    })
  }

  function useMyLocation() {
    if (!('geolocation' in navigator)) return
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setCenter({ lat: pos.coords.latitude, lon: pos.coords.longitude })
        setLocated(true)
      },
      () => {},
      { timeout: 8000 },
    )
  }

  return (
    <section className="rounded-2xl bg-white p-5 shadow-sm">
      <div className="flex items-center justify-between gap-2">
        <h2 className="text-base font-bold text-neutral-900">{t('map.title')}</h2>
        <button
          onClick={useMyLocation}
          className="rounded-xl border border-neutral-300 px-3 py-2 text-sm font-semibold text-neutral-700"
        >
          {t('map.useLocation')}
        </button>
      </div>
      {!located && center && (
        <p className="mt-1 text-xs text-neutral-500">{t('map.approx')}</p>
      )}
      <div className="mt-3 flex flex-wrap items-center gap-2">
        {(Object.keys(KIND_META) as GeoKind[]).map((kind) => (
          <button
            key={kind}
            onClick={() => toggleKind(kind)}
            aria-pressed={kinds.has(kind)}
            className={`rounded-full border px-3 py-1.5 text-xs font-semibold ${
              kinds.has(kind)
                ? 'border-primary-700 bg-primary-50 text-primary-800'
                : 'border-neutral-300 text-neutral-500'
            }`}
          >
            <span aria-hidden="true">{KIND_META[kind].icon} </span>
            {KIND_LABEL[kind]}
          </button>
        ))}
        <select
          aria-label={t('map.radius')}
          value={radiusKm}
          onChange={(e) => setRadiusKm(Number(e.target.value))}
          className="rounded-full border border-neutral-300 bg-white px-3 py-1.5 text-xs font-semibold"
        >
          {RADII.map((r) => (
            <option key={r} value={r}>
              {r} km
            </option>
          ))}
        </select>
      </div>
      {center ? (
        <div
          ref={containerRef}
          className="mt-3 h-72 w-full overflow-hidden rounded-xl border border-neutral-200"
          role="application"
          aria-label={t('map.title')}
        />
      ) : (
        <p className="mt-3 text-sm text-neutral-500">{t('map.noLocation')}</p>
      )}
      {failed && (
        <p role="alert" className="mt-2 text-sm text-red-600">
          {t('state.failed')}
        </p>
      )}
      {center && !failed && (
        <p className="mt-2 text-xs text-neutral-500">
          {nodes.length === 0
            ? t('map.empty')
            : t('map.count')
                .replace('{n}', String(nodes.length))
                .replace('{r}', String(radiusKm))}
        </p>
      )}
    </section>
  )
}
