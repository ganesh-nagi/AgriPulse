/** Display helpers. Formatting only - no business rules. */

export function tonnes(value: number): string {
  return `${value.toLocaleString('en-IN', { maximumFractionDigits: 1 })} t`
}

export function range(min: number, max: number): string {
  return `${tonnes(min)} – ${tonnes(max)}`
}

export function percent(value: number): string {
  return `${Math.round(value)}%`
}
