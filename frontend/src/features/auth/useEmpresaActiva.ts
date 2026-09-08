import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import type { EmpresaAdmin } from '../../api/client'
import { useAuth } from './useAuth'

export function useEmpresaActiva() {
  const { sesion } = useAuth()
  const [empresaId, setEmpresaId] = useState<number | null>(sesion?.empresaId ?? null)
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    let activo = true
    if (sesion && sesion.empresaId != null) {
      setEmpresaId(sesion.empresaId)
      setCargando(false)
      return () => {
        activo = false
      }
    }
    api
      .get<EmpresaAdmin[]>('/admin/empresas')
      .then((res) => {
        if (activo) setEmpresaId(res.data[0]?.id ?? null)
      })
      .catch(() => undefined)
      .finally(() => {
        if (activo) setCargando(false)
      })
    return () => {
      activo = false
    }
  }, [sesion])

  return { empresaId, cargando }
}