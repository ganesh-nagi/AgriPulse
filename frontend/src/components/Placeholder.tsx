interface Props {
  title: string
  description: string
}

/** Foundation placeholder for screens built in later phases. */
export default function Placeholder({ title, description }: Props) {
  return (
    <section className="rounded-2xl bg-white p-6 shadow-sm">
      <h1 className="text-xl font-bold text-neutral-900">{title}</h1>
      <p className="mt-2 text-neutral-600">{description}</p>
    </section>
  )
}
