import { useCallback, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { api, clearTokens, EVENTO_SESION, saveTokens, session } from '../../api/client'
import type { LoginResponse, PerfilUsuario } from '../../api/client'
import { AuthContext } from './AuthContext'
import type { Sesion } from './AuthContext'

function perfilSesion(p: PerfilUsuario): Sesion {
  return {
    username: p.username,
    roles: p.roles,
    empresaId: p.empresaId,
    empresaRazonSocial: p.empresaRazonSocial,
    empresaNit: p.empresaNit,
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [sesion, setSesion] = useState<Sesion | null>(session)

  const obtenerPerfil = useCallback(
    () => api.get<PerfilUsuario>('/auth/me').then((res) => perfilSesion(res.data)),
    [],
  )

  useEffect(() => {
    if (!session()) return
    obtenerPerfil().then(setSesion).catch(() => undefined)
  }, [obtenerPerfil])

  const login = useCallback(
    async (username: string, password: string) => {
      const { data } = await api.post<LoginResponse>('/auth/login', { username, password })
      saveTokens(data.accessToken, data.refreshToken)
      setSesion({ username: data.username, roles: data.roles, empresaId: null })
      window.dispatchEvent(new CustomEvent(EVENTO_SESION))
      obtenerPerfil().then(setSesion).catch(() => undefined)
    },
    [obtenerPerfil],
  )

  const logout = useCallback(() => {
    const refresh = localStorage.getItem('magnetixdian_refresh')
    if (refresh) {
      api.post('/auth/logout', { refreshToken: refresh }).catch(() => undefined)
    }
    clearTokens()
    setSesion(null)
    window.dispatchEvent(new CustomEvent(EVENTO_SESION))
  }, [])

  return <AuthContext.Provider value={{ sesion, login, logout }}>{children}</AuthContext.Provider>
}