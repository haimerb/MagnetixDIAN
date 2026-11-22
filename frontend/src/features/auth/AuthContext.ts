import { createContext } from 'react'

export type Sesion = {
  username: string
  roles: string[]
  empresaId: number | null
  empresaRazonSocial?: string | null
  empresaNit?: string | null
}

export type AuthContextValue = {
  sesion: Sesion | null
  login: (username: string, password: string) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)