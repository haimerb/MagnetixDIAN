import { useContext } from 'react'
import { ThemeModeContext, type ContextoTema } from './contexto'

export function useThemeMode(): ContextoTema {
  const ctx = useContext(ThemeModeContext)
  if (!ctx) throw new Error('useThemeMode debe usarse dentro de ThemeModeProvider')
  return ctx
}
