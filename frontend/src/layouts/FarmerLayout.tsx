import { NavLink, Outlet } from 'react-router-dom'

const TABS = [
  { to: '/', label: 'Home', end: true, icon: '⌂' },
  { to: '/markets', label: 'Markets', icon: '▦' },
  { to: '/report', label: 'Report', icon: '+' },
  { to: '/alerts', label: 'Alerts', icon: '🔔' },
  { to: '/profile', label: 'Profile', icon: '○' },
]

/**
 * Farmer app shell: mobile-first, max phone width, bottom tab bar with
 * large touch targets and icon + text labels.
 */
export default function FarmerLayout() {
  return (
    <div className="min-h-screen bg-neutral-100">
      <div className="mx-auto flex min-h-screen w-full max-w-md flex-col bg-neutral-50">
        <main className="flex-1 space-y-4 p-4 pb-24">
          <Outlet />
        </main>
        <nav
          aria-label="Farmer navigation"
          className="fixed bottom-0 left-1/2 grid w-full max-w-md -translate-x-1/2 grid-cols-5 border-t border-neutral-200 bg-white"
        >
          {TABS.map((tab) => (
            <NavLink
              key={tab.to + tab.label}
              to={tab.to}
              end={tab.end}
              className={({ isActive }) =>
                `flex min-h-[64px] flex-col items-center justify-center gap-1 text-xs font-medium ${
                  isActive ? 'text-primary-700' : 'text-neutral-500'
                }`
              }
            >
              <span aria-hidden="true" className="text-xl leading-none">
                {tab.icon}
              </span>
              <span>{tab.label}</span>
            </NavLink>
          ))}
        </nav>
      </div>
    </div>
  )
}
