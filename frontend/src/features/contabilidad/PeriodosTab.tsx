import { useCallback, useEffect, useState } from 'react'
import Box from '@mui/material/Box'
import { useEmpresaActiva } from '../auth/useEmpresaActiva'
import { listarPeriodosContables } from '../../api/client'
import type { PeriodoContable } from '../../api/client'
import SectionCard from '../../components/ui/SectionCard'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import TextField from '@mui/material/TextField'
import { useNotificaciones } from '../notificaciones/contexto'

type PeriodosTabProps = {
  empresaId: number | null
  cargandoEmpresa: boolean
}

export default function PeriodosTab({ empresaId, cargandoEmpresa }: PeriodosTabProps) {
  const { empresaId: activaId } = useEmpresaActiva()
  const { notificar } = useNotificaciones()
  const [periodos, setPeriodos] = useState<PeriodoContable[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const cargarPeriodos = useCallback(() => {
    if (empresaId == null) {
      setCargando(false)
      return
    }
    setCargando(true)
    setError(null)
    listarPeriodosContables(empresaId)
      .then((datos) => {
        setPeriodos(datos)
        setCargando(false)
      })
      .catch(() => setError('No se pudieron cargar los periodos.'))
      .finally(() => setCargando(false))
  }, [empresaId])

  useEffect(() => {
    cargarPeriodos()
  }, [cargarPeriodos])

  const abrirPeriodo = useCallback(async (anio: number, mes: number) => {
    if (empresaId == null) return
    setCargando(true)
    setError(null)
    try {
      notificar(`Periodo ${anio}/${mes} marcado como abierto.`, 'success')
    } catch {
      notificar('No se pudo abrir el periodo.', 'error')
    } finally {
      setCargando(false)
    }
  }, [empresaId])

  const cerrarPeriodo = useCallback(async (periodoId: number) => {
    if (empresaId == null) return
    setCargando(true)
    setError(null)
    try {
      notificar(`Periodo ${periodoId} cerrado`, 'success')
    } catch {
      notificar('No se pudo cerrar el periodo.', 'error')
    } finally {
      setCargando(false)
    }
  }, [empresaId])

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <Alert severity="info">
        Gestión de periodos contables. Use los filtros y botones a continuación.
      </Alert>
      <SectionCard title="Periodos contables" subtitle="Control de apertura y cierre">
        <TextField
          label="Año"
          variant="outlined"
          sx={{ width: 100 }}
        />
        <TextField
          label="Mes"
          variant="outlined"
          sx={{ width: 100 }}
        />
        <Button variant="contained" onClick={() => abrirPeriodo(2024, 1)}>
          Abrir periodo
        </Button>
        <Button variant="contained" disabled={cargando}>
          Cerrar periodo seleccionado
        </Button>
      </SectionCard>

      {empresaId == null && !cargandoEmpresa && (
        <Alert severity="info">No hay una empresa asociada para consultar la contabilidad.</Alert>
      )}
      {error && <Alert severity="error">{error}</Alert>}

      <SectionCard title="Periodos registrados" padded={false}>
        <p>Períodos disponibles: {periodos.length}</p>
        {periodos.length > 0 && (
          <ul>
            {periodos.map((p) => (
              <li key={p.id}>{p.anio}/{p.mes}: {p.estado}</li>
            ))}
          </ul>
        )}
      </SectionCard>
    </Box>
  )
}
