import { Link, Outlet } from 'react-router-dom'

interface Props {
  title: string
}

/** Simple top-nav shell for information-dense stakeholder screens. */
export default function StakeholderLayout({ title }: Props) {
  return (
    <div className="min-h-screen bg-neutral-100">
      <header className="border-b border-neutral-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
          <Link to="/" className="text-lg font-bold text-primary-700">
            AgriPulse
          </Link>
          <span className="text-sm text-neutral-500">{title}</span>
        </div>
      </header>
      <main className="mx-auto max-w-5xl space-y-4 p-4">
        <Outlet />
      </main>
    </div>
  )
}
