import Alert, { type AlertColor } from '@mui/material/Alert'
import Snackbar from '@mui/material/Snackbar'
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { EVENTO_ERROR_API } from '../../api/client'
import { NotificacionesContext } from './contexto'

type Notificacion = { mensaje: string; severidad: AlertColor }

export function NotificacionesProvider({ children }: { children: ReactNode }) {
  const [actual, setActual] = useState<Notificacion | null>(null)

  const notificar = useCallback((mensaje: string, severidad: AlertColor = 'info') => {
    setActual({ mensaje, severidad })
  }, [])

  useEffect(() => {
    const handler = (evento: Event) => {
      const detalle = (evento as CustomEvent<string>).detail
      setActual({ mensaje: detalle || 'Ocurrió un error inesperado.', severidad: 'error' })
    }
    window.addEventListener(EVENTO_ERROR_API, handler)
    return () => window.removeEventListener(EVENTO_ERROR_API, handler)
  }, [])

  const valor = useMemo(() => ({ notificar }), [notificar])

  return (
    <NotificacionesContext.Provider value={valor}>
      {children}
      <Snackbar
        open={actual != null}
        autoHideDuration={6000}
        onClose={() => setActual(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert
          severity={actual?.severidad ?? 'info'}
          variant="filled"
          onClose={() => setActual(null)}
        >
          {actual?.mensaje}
        </Alert>
      </Snackbar>
    </NotificacionesContext.Provider>
  )
}
