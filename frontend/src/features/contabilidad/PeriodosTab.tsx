import { useCallback, useEffect, useMemo, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid2'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import {
  cerrarPeriodoContable,
  crearPeriodoContable,
  listarPeriodosContables,
  type PeriodoContable,
} from '../../api/client'
import EstadoChip from '../../components/ui/EstadoChip'
import SectionCard from '../../components/ui/SectionCard'
import { useNotificaciones } from '../notificaciones/contexto'
import TablaDatos, { type ColumnaTabla, type FilaTabla } from './TablaDatos'

type PeriodosTabProps = {
  empresaId: number | null
  cargandoEmpresa: boolean
}

const COLUMNAS: ColumnaTabla[] = [
  { id: 'periodo', label: 'Periodo' },
  { id: 'rango', label: 'Rango' },
  { id: 'estado', label: 'Estado' },
]

function numeroEntero(valor: string, maximo?: number): number | null {
  const numero = Number(valor)
  if (!Number.isInteger(numero) || numero <= 0 || (maximo != null && numero > maximo)) return null
  return numero
}

function inicioMes(anio: number, mes: number): string {
  return `${anio}-${String(mes).padStart(2, '0')}-01`
}

function finMes(anio: number, mes: number): string {
  const ultimoDia = new Date(Date.UTC(anio, mes, 0)).getUTCDate()
  return `${anio}-${String(mes).padStart(2, '0')}-${String(ultimoDia).padStart(2, '0')}`
}

function valoresIniciales() {
  const ahora = new Date()
  const anio = ahora.getFullYear()
  const mes = ahora.getMonth() + 1
  return {
    anio: String(anio),
    mes: String(mes),
    fechaDesde: inicioMes(anio, mes),
    fechaHasta: finMes(anio, mes),
  }
}

const INICIALES = valoresIniciales()

export default function PeriodosTab({ empresaId, cargandoEmpresa }: PeriodosTabProps) {
  const { notificar } = useNotificaciones()
  const [periodos, setPeriodos] = useState<PeriodoContable[]>([])
  const [periodoSeleccionadoId, setPeriodoSeleccionadoId] = useState<number | null>(null)
  const [anio, setAnio] = useState(INICIALES.anio)
  const [mes, setMes] = useState(INICIALES.mes)
  const [fechaDesde, setFechaDesde] = useState(INICIALES.fechaDesde)
  const [fechaHasta, setFechaHasta] = useState(INICIALES.fechaHasta)
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
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
        setPeriodoSeleccionadoId((actual) => {
          if (actual != null && datos.some((periodo) => periodo.id === actual)) return actual
          return datos.find((periodo) => periodo.estado === 'ABIERTO')?.id ?? datos[0]?.id ?? null
        })
      })
      .catch(() => setError('No se pudieron cargar los periodos contables.'))
      .finally(() => setCargando(false))
  }, [empresaId])

  useEffect(() => {
    cargarPeriodos()
  }, [cargarPeriodos])

  const crearPeriodo = useCallback(async () => {
    if (empresaId == null) return
    const anioNumero = numeroEntero(anio)
    const mesNumero = numeroEntero(mes, 12)
    if (anioNumero == null || mesNumero == null || !fechaDesde || !fechaHasta) {
      setError('Complete el año, el mes y el rango de fechas del periodo.')
      return
    }
    setGuardando(true)
    setError(null)
    try {
      const creado = await crearPeriodoContable(empresaId, {
        anio: anioNumero,
        mes: mesNumero,
        fechaDesde,
        fechaHasta,
      })
      setPeriodoSeleccionadoId(creado.id)
      await cargarPeriodos()
      notificar(`Periodo ${creado.anio}/${String(creado.mes).padStart(2, '0')} abierto.`, 'success')
    } catch {
      setError('No se pudo crear el periodo contable.')
    } finally {
      setGuardando(false)
    }
  }, [empresaId, anio, mes, fechaDesde, fechaHasta, cargarPeriodos, notificar])

  const cerrarPeriodo = useCallback(async () => {
    if (empresaId == null || periodoSeleccionadoId == null) return
    setGuardando(true)
    setError(null)
    try {
      const asiento = await cerrarPeriodoContable(empresaId, periodoSeleccionadoId)
      await cargarPeriodos()
      notificar(`Periodo cerrado con el asiento ${asiento.numero}.`, 'success')
    } catch {
      setError('No se pudo cerrar el periodo contable.')
    } finally {
      setGuardando(false)
    }
  }, [empresaId, periodoSeleccionadoId, cargarPeriodos, notificar])

  const periodoSeleccionado = useMemo(
    () => periodos.find((periodo) => periodo.id === periodoSeleccionadoId) ?? null,
    [periodos, periodoSeleccionadoId],
  )

  const filas = useMemo<FilaTabla[]>(
    () =>
      periodos.map((periodo) => ({
        id: periodo.id,
        celdas: {
          periodo: `${periodo.anio}/${String(periodo.mes).padStart(2, '0')}`,
          rango: `${periodo.fechaDesde} — ${periodo.fechaHasta}`,
          estado: <EstadoChip estado={periodo.estado} />,
        },
      })),
    [periodos],
  )

  const actualizarAnio = (valor: string) => {
    setAnio(valor)
    const anioNumero = numeroEntero(valor)
    const mesNumero = numeroEntero(mes, 12)
    if (anioNumero != null && mesNumero != null) {
      setFechaDesde(inicioMes(anioNumero, mesNumero))
      setFechaHasta(finMes(anioNumero, mesNumero))
    }
  }

  const actualizarMes = (valor: string) => {
    setMes(valor)
    const anioNumero = numeroEntero(anio)
    const mesNumero = numeroEntero(valor, 12)
    if (anioNumero != null && mesNumero != null) {
      setFechaDesde(inicioMes(anioNumero, mesNumero))
      setFechaHasta(finMes(anioNumero, mesNumero))
    }
  }

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      {empresaId == null && !cargandoEmpresa && (
        <Alert severity="info">No hay una empresa asociada para consultar la contabilidad.</Alert>
      )}
      {error && <Alert severity="error">{error}</Alert>}

      <SectionCard
        title="Gestión de periodos"
        subtitle="Crear periodos abiertos y cerrarlos con su asiento contable"
      >
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 2 }}>
            <TextField fullWidth label="Año" type="number" value={anio} onChange={(evento) => actualizarAnio(evento.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 2 }}>
            <TextField fullWidth label="Mes" type="number" value={mes} onChange={(evento) => actualizarMes(evento.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField
              fullWidth
              label="Desde"
              type="date"
              value={fechaDesde}
              slotProps={{ inputLabel: { shrink: true } }}
              onChange={(evento) => setFechaDesde(evento.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField
              fullWidth
              label="Hasta"
              type="date"
              value={fechaHasta}
              slotProps={{ inputLabel: { shrink: true } }}
              onChange={(evento) => setFechaHasta(evento.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
              <Button
                variant="contained"
                loading={guardando}
                disabled={empresaId == null}
                onClick={() => void crearPeriodo()}
              >
                Crear y abrir periodo
              </Button>
              <Button
                variant="outlined"
                loading={guardando}
                disabled={empresaId == null || periodoSeleccionado == null || periodoSeleccionado.estado === 'CERRADO'}
                onClick={() => void cerrarPeriodo()}
              >
                Cerrar periodo seleccionado
              </Button>
            </Stack>
          </Grid>
        </Grid>
      </SectionCard>

      <Alert severity="info">
        Cerrar un periodo genera el asiento de cierre y bloquea nuevos movimientos contables en ese rango.
      </Alert>

      <SectionCard
        title="Periodos registrados"
        subtitle="Seleccione un periodo para cerrarlo"
        padded={false}
      >
        <TablaDatos
          ariaLabel="Periodos contables"
          columnas={COLUMNAS}
          filas={filas}
          cargando={cargando || cargandoEmpresa}
          minWidth={720}
          tituloVacio="Sin periodos contables"
          descripcionVacio="Cree el primer periodo para comenzar a contabilizar."
          onSeleccionar={(id) => setPeriodoSeleccionadoId(Number(id))}
          seleccionado={periodoSeleccionadoId}
        />
      </SectionCard>
    </Box>
  )
}
