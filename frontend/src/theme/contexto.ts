import { createContext } from 'react'
import type { Branding } from '../api/client'
import type { ThemeMode } from './theme'

export type ContextoTema = {
  mode: ThemeMode
  alternar: () => void
  branding: Branding | null
  actualizarBranding: (branding: Branding | null) => void
}

export const ThemeModeContext = createContext<ContextoTema | null>(null)

export const CLAVE_TEMA = 'magnetixdian_theme'

export function modoInicial(): ThemeMode {
  if (typeof window === 'undefined') return 'light'
  const guardado = window.localStorage.getItem(CLAVE_TEMA)
  if (guardado === 'light' || guardado === 'dark') return guardado
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}
