interface Props {
  icon: string
  label: string
  value: string
  hint?: string
}

/** Icon + label + short explanation row. Low-literacy friendly. */
export default function InfoRow({ icon, label, value, hint }: Props) {
  return (
    <div className="flex items-center gap-3 py-3">
      <span
        aria-hidden="true"
        className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-neutral-100 text-xl"
      >
        {icon}
      </span>
      <div className="min-w-0 flex-1">
        <p className="text-sm text-neutral-500">{label}</p>
        <p className="text-base font-bold text-neutral-900">{value}</p>
        {hint && <p className="truncate text-xs text-neutral-500">{hint}</p>}
      </div>
    </div>
  )
}
