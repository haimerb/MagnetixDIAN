import { useContext } from 'react'
import { AuthContext } from './AuthContext'
import type { Sesion } from './AuthContext'

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return ctx
}

export function hasRole(sesion: Sesion | null, rol: string) {
  return sesion?.roles.includes(rol) ?? false
}