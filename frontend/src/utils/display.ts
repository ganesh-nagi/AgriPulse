import { t } from '../i18n'
import type {
  DemandEstimate,
  PressureAssessment,
  PressureBand,
  ResourceSnapshot,
} from '../types'

/**
 * Presentation mappings only. Bands, ranges and confidence come from the
 * server; this file only turns them into short farmer-friendly labels.
 */

export function supplyCondition(assessment: PressureAssessment): { label: string; hint: string } {
  const text = assessment.reasons
    .filter((r) => r.component === 'SUPPLY')
    .map((r) => r.message)
    .join(' ')
  if (/20%\+ above/i.test(text)) return { label: t('supply.veryHigh'), hint: t('supply.veryHighHint') }
  if (/above estimated demand/i.test(text)) return { label: t('supply.high'), hint: t('supply.highHint') }
  if (/shortage/i.test(text)) return { label: t('supply.low'), hint: t('supply.lowHint') }
  return { label: t('supply.moderate'), hint: t('supply.moderateHint') }
}

export function demandStrength(demand: DemandEstimate): { label: string; hint: string } {
  const base = Math.max(demand.estimatedMaxTonnes, 0.000001)
  const share = demand.confirmedDemandTonnes / base
  if (share >= 0.6) return { label: t('demand.strong'), hint: t('demand.strongHint') }
  if (share >= 0.3) return { label: t('demand.steady'), hint: t('demand.steadyHint') }
  return { label: t('demand.weak'), hint: t('demand.weakHint') }
}

export function marketCondition(band: PressureBand): string {
  switch (band) {
    case 'LOW':
      return t('pressure.calm')
    case 'MODERATE':
      return t('pressure.watch')
    case 'HIGH':
      return t('pressure.tight')
    case 'CRITICAL':
      return t('pressure.strained')
  }
}

export function resourceCondition(
  snapshot: ResourceSnapshot,
  effective: number,
): { label: string; hint: string } {
  const base = Math.max(effective, 0.000001)
  const storageOk = snapshot.storageAvailableTonnes / base
  const transportOk = snapshot.transportAvailableTonnes / base
  if (storageOk >= 1 && transportOk >= 1)
    return { label: t('resources.good'), hint: `${Math.round(snapshot.storageAvailableTonnes)} t · ${Math.round(snapshot.transportAvailableTonnes)} t` }
  if (snapshot.storageAvailableTonnes <= 0 && snapshot.transportAvailableTonnes <= 0)
    return { label: t('resources.none'), hint: '' }
  return { label: t('resources.tight'), hint: `${Math.round(snapshot.storageAvailableTonnes)} t · ${Math.round(snapshot.transportAvailableTonnes)} t` }
}

/** Reasons worth surfacing as alerts: binding constraints and shortage. */
export function importantReasons(assessment: PressureAssessment): { icon: string; text: string }[] {
  const out: { icon: string; text: string }[] = []
  for (const r of assessment.reasons) {
    if (r.component === 'SUPPLY' && /above|shortage|20%/i.test(r.message))
      out.push({ icon: '⚠', text: r.message })
    if (r.component === 'STORAGE' && /limited|no storage/i.test(r.message))
      out.push({ icon: '▤', text: r.message })
    if (r.component === 'TRANSPORT' && /constrained|no transport/i.test(r.message))
      out.push({ icon: '🚚', text: r.message })
    if (r.component === 'DEMAND' && /escalates/i.test(r.message))
      out.push({ icon: '○', text: r.message })
  }
  return out
}
