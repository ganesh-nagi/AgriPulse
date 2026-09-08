import type { PressureBand } from '../types'

const BAND_STYLE: Record<PressureBand, { icon: string; classes: string }> = {
  LOW: { icon: '●', classes: 'bg-green-50 text-green-800 border-green-200' },
  MODERATE: { icon: '◐', classes: 'bg-yellow-50 text-yellow-800 border-yellow-200' },
  HIGH: { icon: '◑', classes: 'bg-orange-50 text-orange-800 border-orange-200' },
  CRITICAL: { icon: '⬤', classes: 'bg-red-50 text-red-800 border-red-200' },
}

interface Props {
  band: PressureBand
}

/**
 * Status badge: always icon + text label, never color alone (accessibility rule).
 */
export default function StatusBadge({ band }: Props) {
  const style = BAND_STYLE[band]
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-sm font-medium ${style.classes}`}
    >
      <span aria-hidden="true">{style.icon}</span>
      <span>{band}</span>
    </span>
  )
}
