import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import CssBaseline from '@mui/material/CssBaseline'
import { ThemeProvider } from '@mui/material/styles'
import { api, EVENTO_SESION, session } from '../api/client'
import type { Branding, EmpresaAdmin, PerfilUsuario } from '../api/client'
import { CLAVE_TEMA, ThemeModeContext, modoInicial, type ContextoTema } from './contexto'
import { createAppTheme, type ThemeMode } from './theme'

export default function ThemeModeProvider({ children }: { children: ReactNode }) {
  const [mode, setMode] = useState<ThemeMode>(modoInicial)
  const [branding, setBranding] = useState<Branding | null>(null)
  const [intento, setIntento] = useState(0)
  const theme = useMemo(() => createAppTheme(mode, branding), [mode, branding])

  useEffect(() => {
    const actualizar = () => setIntento((n) => n + 1)
    window.addEventListener(EVENTO_SESION, actualizar)
    return () => window.removeEventListener(EVENTO_SESION, actualizar)
  }, [])

  useEffect(() => {
    let activo = true
    if (!session()) {
      setBranding(null)
      return () => {
        activo = false
      }
    }
    const cargar = async () => {
      const { data: perfil } = await api.get<PerfilUsuario>('/auth/me', { skipGlobalError: true })
      let empresaId = perfil.empresaId
      if (empresaId == null && perfil.roles?.includes('ROLE_ADMIN')) {
        const { data: empresas } = await api.get<EmpresaAdmin[]>('/admin/empresas', {
          skipGlobalError: true,
        })
        empresaId = empresas[0]?.id ?? null
      }
      if (empresaId == null) {
        if (activo) setBranding(null)
        return
      }
      const { data } = await api.get<Branding>(`/empresas/${empresaId}/branding`, {
        skipGlobalError: true,
      })
      if (activo) setBranding(data)
    }
    cargar().catch(() => {
      if (activo) setBranding(null)
    })
    return () => {
      activo = false
    }
  }, [intento])

  useEffect(() => {
    const meta = document.querySelector<HTMLMetaElement>('meta[name="theme-color"]')
    if (meta && branding?.pwaThemeColor) meta.content = branding.pwaThemeColor
  }, [branding])

  const actualizarBranding = useCallback((valor: Branding | null) => setBranding(valor), [])

  const valor = useMemo<ContextoTema>(
    () => ({
      mode,
      alternar: () =>
        setMode((actual) => {
          const siguiente: ThemeMode = actual === 'dark' ? 'light' : 'dark'
          window.localStorage.setItem(CLAVE_TEMA, siguiente)
          return siguiente
        }),
      branding,
      actualizarBranding,
    }),
    [mode, branding, actualizarBranding],
  )

  return (
    <ThemeModeContext.Provider value={valor}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        {children}
      </ThemeProvider>
    </ThemeModeContext.Provider>
  )
}
