import { useCallback, useEffect, useState } from 'react'

export function useRecursoEmpresa<T>(empresaId: number | null, cargar: (empresaId: number) => Promise<T[]>) {
  const [datos, setDatos] = useState<T[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [version, setVersion] = useState(0)

  const recargar = useCallback(() => setVersion((valor) => valor + 1), [])

  useEffect(() => {
    if (empresaId == null) {
      setDatos([])
      setCargando(false)
      return
    }
    let activo = true
    setCargando(true)
    setError(null)
    cargar(empresaId)
      .then((resultado) => {
        if (activo) setDatos(resultado)
      })
      .catch(() => {
        if (activo) {
          setDatos([])
          setError('No se pudo cargar la información. Intente de nuevo.')
        }
      })
      .finally(() => {
        if (activo) setCargando(false)
      })
    return () => {
      activo = false
    }
  }, [empresaId, cargar, version])

  return { datos, cargando, error, recargar }
}
