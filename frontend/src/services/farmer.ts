import { apiGet } from './api'
import type {
  CropOption,
  DemandEstimate,
  Farm,
  GeoKind,
  GeoNode,
  MarketNode,
  OnboardingStatus,
  PressureAssessment,
  ResourceSnapshot,
  SupplyReport,
} from '../types'

function iso(date: Date): string {
  return date.toISOString().slice(0, 10)
}

/** Default intelligence window: last 30 days of demand, next 60 days ahead. */
export function defaultWindow(): { from: string; to: string } {
  const now = new Date()
  return {
    from: iso(new Date(now.getTime() - 30 * 86400000)),
    to: iso(new Date(now.getTime() + 60 * 86400000)),
  }
}

export function getFarms(): Promise<Farm[]> {
  return apiGet<Farm[]>('/api/farmer/farms/mine')
}

export function getCrops(): Promise<CropOption[]> {
  return apiGet<CropOption[]>('/api/crops')
}

export function getPressure(
  cropId: number,
  region: string,
  from: string,
  to: string,
): Promise<PressureAssessment> {
  const q = `cropId=${cropId}&region=${encodeURIComponent(region)}&from=${from}&to=${to}`
  return apiGet<PressureAssessment>(`/api/pressure?${q}`)
}

export function getDemand(
  cropId: number,
  region: string,
  from: string,
  to: string,
): Promise<DemandEstimate> {
  const q = `cropId=${cropId}&region=${encodeURIComponent(region)}&from=${from}&to=${to}`
  return apiGet<DemandEstimate>(`/api/demand/estimate?${q}`)
}

export function getSnapshot(
  region: string,
  cropId: number,
  date: string,
): Promise<ResourceSnapshot> {
  const q = `region=${encodeURIComponent(region)}&cropId=${cropId}&date=${date}`
  return apiGet<ResourceSnapshot>(`/api/resources/snapshot?${q}`)
}

export function getMarkets(region: string): Promise<MarketNode[]> {
  return apiGet<MarketNode[]>(`/api/markets?region=${encodeURIComponent(region)}`)
}

export function getNearby(
  lat: number,
  lon: number,
  radiusKm: number,
  kinds?: GeoKind[],
  crop?: string,
): Promise<GeoNode[]> {
  const params = new URLSearchParams({
    lat: String(lat),
    lon: String(lon),
    radiusKm: String(radiusKm),
  })
  kinds?.forEach((k) => params.append('kinds', k))
  if (crop) params.set('crop', crop)
  return apiGet<GeoNode[]>(`/api/geo/nearby?${params.toString()}`)
}

export function getMyReports(): Promise<SupplyReport[]> {
  return apiGet<SupplyReport[]>('/api/reports/mine')
}

export function getVerification(): Promise<OnboardingStatus> {
  return apiGet<OnboardingStatus>('/api/verification/status')
}
