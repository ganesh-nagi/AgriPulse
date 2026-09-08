import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-neutral-100 p-6">
      <div className="rounded-2xl bg-white p-8 text-center shadow-sm">
        <h1 className="text-xl font-bold">Page not found</h1>
        <Link to="/" className="mt-4 inline-block font-semibold text-primary-700">
          Go home
        </Link>
      </div>
    </div>
  )
}
