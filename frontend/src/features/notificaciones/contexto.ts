import type { AlertColor } from '@mui/material/Alert'
import { createContext, useContext } from 'react'

export type ContextoNotificaciones = {
  notificar: (mensaje: string, severidad?: AlertColor) => void
}

export const NotificacionesContext = createContext<ContextoNotificaciones | null>(null)

export function useNotificaciones() {
  const ctx = useContext(NotificacionesContext)
  if (!ctx) throw new Error('useNotificaciones debe usarse dentro de NotificacionesProvider')
  return ctx
}
