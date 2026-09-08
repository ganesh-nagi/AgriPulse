import { en, type AppStrings } from './en'

/**
 * Minimal i18n architecture. English ships now; add a dictionary and list
 * it in LANGUAGES for a new locale. Missing keys fall back to English.
 */
const DICTIONARIES: Record<string, Partial<AppStrings>> = { en }

export const LANGUAGES = [{ code: 'en', label: 'English' }]

const LANG_KEY = 'agripulse.lang'

export function getLanguage(): string {
  const saved = localStorage.getItem(LANG_KEY)
  return saved && DICTIONARIES[saved] ? saved : 'en'
}

export function setLanguage(code: string): void {
  if (DICTIONARIES[code]) localStorage.setItem(LANG_KEY, code)
}

export type StringKey = keyof AppStrings

export function t(key: StringKey): string {
  const lang = getLanguage()
  return DICTIONARIES[lang]?.[key] ?? en[key]
}
