import StatusBadge from '../../components/StatusBadge'
import { percent, range } from '../../utils/format'

/**
 * Farmer home: regional picture only (ranges + confidence + source labels).
 * No individual farmer data is ever shown here.
 */
export default function HomePage() {
  return (
    <div className="space-y-4">
      <section className="rounded-2xl bg-white p-6 shadow-sm">
        <p className="text-sm text-neutral-500">Tomato · Your region</p>
        <h1 className="mt-1 text-xl font-bold text-neutral-900">This week</h1>
        <dl className="mt-4 space-y-3 text-sm">
          <div className="flex items-center justify-between">
            <dt className="text-neutral-600">Supply (ESTIMATED)</dt>
            <dd className="font-semibold">{range(90, 110)}</dd>
          </div>
          <div className="flex items-center justify-between">
            <dt className="text-neutral-600">Demand (ESTIMATED)</dt>
            <dd className="font-semibold">{range(90, 110)}</dd>
          </div>
          <div className="flex items-center justify-between">
            <dt className="text-neutral-600">Confidence</dt>
            <dd className="font-semibold">{percent(74)}</dd>
          </div>
          <div className="flex items-center justify-between">
            <dt className="text-neutral-600">Market pressure</dt>
            <dd>
              <StatusBadge band="MODERATE" />
            </dd>
          </div>
        </dl>
      </section>
    </div>
  )
}
