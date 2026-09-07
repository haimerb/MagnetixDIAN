import { useCallback, useState } from 'react'
import type { ReactNode } from 'react'
import { api, clearTokens, saveTokens, session } from '../../api/client'
import type { LoginResponse } from '../../api/client'
import { AuthContext } from './AuthContext'
import type { Sesion } from './AuthContext'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [sesion, setSesion] = useState<Sesion | null>(session)

  const login = useCallback(async (username: string, password: string) => {
    const { data } = await api.post<LoginResponse>('/auth/login', { username, password })
    saveTokens(data.accessToken, data.refreshToken)
    setSesion({ username: data.username, roles: data.roles })
  }, [])

  const logout = useCallback(() => {
    clearTokens()
    setSesion(null)
  }, [])

  return <AuthContext.Provider value={{ sesion, login, logout }}>{children}</AuthContext.Provider>
}