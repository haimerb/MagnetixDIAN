import { useMemo, useState, type ReactNode } from 'react'
import CssBaseline from '@mui/material/CssBaseline'
import { ThemeProvider } from '@mui/material/styles'
import { CLAVE_TEMA, ThemeModeContext, modoInicial, type ContextoTema } from './contexto'
import { createAppTheme, type ThemeMode } from './theme'

export default function ThemeModeProvider({ children }: { children: ReactNode }) {
  const [mode, setMode] = useState<ThemeMode>(modoInicial)
  const theme = useMemo(() => createAppTheme(mode), [mode])

  const valor = useMemo<ContextoTema>(
    () => ({
      mode,
      alternar: () =>
        setMode((actual) => {
          const siguiente: ThemeMode = actual === 'dark' ? 'light' : 'dark'
          window.localStorage.setItem(CLAVE_TEMA, siguiente)
          return siguiente
        }),
    }),
    [mode],
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
