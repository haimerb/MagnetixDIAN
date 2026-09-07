import { createContext } from 'react'

export type Sesion = { username: string; roles: string[] }

export type AuthContextValue = {
  sesion: Sesion | null
  login: (username: string, password: string) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)