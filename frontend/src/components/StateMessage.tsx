import { t } from '../i18n'

interface Props {
  kind: 'loading' | 'error' | 'empty'
  message: string
  onRetry?: () => void
}

/** Consistent loading / error / empty states across farmer screens. */
export default function StateMessage({ kind, message, onRetry }: Props) {
  if (kind === 'loading') {
    return (
      <section className="rounded-2xl bg-white p-6 text-center shadow-sm" aria-live="polite">
        <p className="text-2xl" aria-hidden="true">
          ◌
        </p>
        <p className="mt-2 text-sm text-neutral-500">{t('state.loading')}</p>
      </section>
    )
  }
  return (
    <section className="rounded-2xl bg-white p-6 text-center shadow-sm" role="alert">
      <p className="text-2xl" aria-hidden="true">
        {kind === 'error' ? '⚠' : '○'}
      </p>
      <p className="mt-2 text-sm text-neutral-600">{message}</p>
      {kind === 'error' && onRetry && (
        <button
          onClick={onRetry}
          className="mt-4 w-full rounded-xl bg-primary-700 px-4 py-3 font-semibold text-white"
        >
          {t('state.retry')}
        </button>
      )}
    </section>
  )
}
