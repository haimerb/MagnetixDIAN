import { useCallback, useEffect, useMemo, useState } from 'react'
import { obtenerHabilitacionDian } from '../../api/erp/dian'
import type { HabilitacionDian } from '../../api/client'

export function useHabilitacionDian(empresaId: number | null) {
  const [habilitacion, setHabilitacion] = useState<HabilitacionDian | null>(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [version, setVersion] = useState(0)

  const recargar = useCallback(() => setVersion((valor) => valor + 1), [])

  useEffect(() => {
    if (empresaId == null) {
      setHabilitacion(null)
      setCargando(false)
      return
    }
    let activo = true
    setCargando(true)
    setError(null)
    obtenerHabilitacionDian(empresaId)
      .then((datos) => {
        if (activo) setHabilitacion(datos)
      })
      .catch(() => {
        if (activo) {
          setHabilitacion(null)
          setError('No se pudo consultar la habilitación DIAN de la empresa.')
        }
      })
      .finally(() => {
        if (activo) setCargando(false)
      })
    return () => {
      activo = false
    }
  }, [empresaId, version])

  const puedeEmitir = useMemo(() => habilitacion?.estado === 'PRODUCCION', [habilitacion])
  const testSetAprobado = useMemo(
    () => habilitacion?.estado === 'TEST_SET_APROBADO' || habilitacion?.estado === 'PRODUCCION',
    [habilitacion],
  )

  return { habilitacion, cargando, error, puedeEmitir, testSetAprobado, recargar, setHabilitacion }
}
