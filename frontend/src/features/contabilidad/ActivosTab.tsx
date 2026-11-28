import { useCallback, useEffect, useMemo, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid2'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import {
  crearActivoErp,
  darBajaActivoErp,
  depreciarActivoErp,
  listarActivosErp,
  listarDepreciacionesErp,
  type ActivoErp,
  type DepreciacionErp,
} from '../../api/client'
import EstadoChip from '../../components/ui/EstadoChip'
import SectionCard from '../../components/ui/SectionCard'
import { money } from '../facturacion/calculos'
import { useNotificaciones } from '../notificaciones/contexto'
import TablaDatos, { type ColumnaTabla, type FilaTabla } from './TablaDatos'

type ActivosTabProps = {
  empresaId: number | null
  cargandoEmpresa: boolean
}

const COLUMNAS_ACTIVOS: ColumnaTabla[] = [
  { id: 'codigo', label: 'Código' },
  { id: 'nombre', label: 'Activo' },
  { id: 'categoria', label: 'Categoría' },
  { id: 'fechaAlta', label: 'Alta' },
  { id: 'valorAdquisicion', label: 'Adquisición', align: 'right' },
  { id: 'valorLibros', label: 'Valor libros', align: 'right' },
  { id: 'depreciacionAcumulada', label: 'Depreciado', align: 'right' },
  { id: 'estado', label: 'Estado' },
]

const COLUMNAS_DEPRECIACIONES: ColumnaTabla[] = [
  { id: 'periodo', label: 'Periodo' },
  { id: 'fecha', label: 'Fecha' },
  { id: 'codigoActivo', label: 'Activo' },
  { id: 'valor', label: 'Depreciación', align: 'right' },
  { id: 'depreciacionAcumulada', label: 'Acumulada', align: 'right' },
  { id: 'valorLibros', label: 'Valor libros', align: 'right' },
  { id: 'estado', label: 'Estado' },
]

function hoy() {
  return new Date().toISOString().slice(0, 10)
}

function numero(valor: string): number | null {
  if (valor.trim() === '') return null
  const resultado = Number(valor)
  return Number.isFinite(resultado) ? resultado : null
}

function mensajeBackend(error: unknown, respaldo: string): string {
  const datos = (error as { response?: { data?: unknown } }).response?.data
  if (datos && typeof datos === 'object') {
    const mensaje = (datos as { message?: unknown }).message
    if (typeof mensaje === 'string' && mensaje.trim()) return mensaje
  }
  return respaldo
}

export default function ActivosTab({ empresaId, cargandoEmpresa }: ActivosTabProps) {
  const { notificar } = useNotificaciones()
  const [activos, setActivos] = useState<ActivoErp[]>([])
  const [depreciaciones, setDepreciaciones] = useState<DepreciacionErp[]>([])
  const [seleccionadoId, setSeleccionadoId] = useState<number | null>(null)
  const [codigo, setCodigo] = useState('')
  const [nombre, setNombre] = useState('')
  const [categoria, setCategoria] = useState('')
  const [fechaAlta, setFechaAlta] = useState(hoy())
  const [valorAdquisicion, setValorAdquisicion] = useState('')
  const [valorResidual, setValorResidual] = useState('0')
  const [vidaUtilMeses, setVidaUtilMeses] = useState('60')
  const [periodo, setPeriodo] = useState(hoy().slice(0, 7))
  const [fechaDepreciacion, setFechaDepreciacion] = useState(hoy())
  const [motivoBaja, setMotivoBaja] = useState('')
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const cargarDatos = useCallback(() => {
    if (empresaId == null) {
      setCargando(false)
      return
    }
    setCargando(true)
    setError(null)
    Promise.all([listarActivosErp(empresaId), listarDepreciacionesErp(empresaId)])
      .then(([listaActivos, listaDepreciaciones]) => {
        setActivos(listaActivos)
        setDepreciaciones(listaDepreciaciones)
        setSeleccionadoId((actual) => {
          if (actual != null && listaActivos.some((activo) => activo.id === actual)) return actual
          return listaActivos[0]?.id ?? null
        })
      })
      .catch(() => setError('No se pudieron cargar los activos fijos.'))
      .finally(() => setCargando(false))
  }, [empresaId])

  useEffect(() => {
    cargarDatos()
  }, [cargarDatos])

  const activoSeleccionado = useMemo(
    () => activos.find((activo) => activo.id === seleccionadoId) ?? null,
    [activos, seleccionadoId],
  )

  const filasActivos = useMemo<FilaTabla[]>(
    () =>
      activos.map((activo) => ({
        id: activo.id,
        celdas: {
          codigo: activo.codigo,
          nombre: activo.nombre,
          categoria: activo.categoria ?? '—',
          fechaAlta: activo.fechaAlta,
          valorAdquisicion: money(activo.valorAdquisicion),
          valorLibros: money(activo.valorLibros),
          depreciacionAcumulada: money(activo.depreciacionAcumulada),
          estado: <EstadoChip estado={activo.estado} />,
        },
      })),
    [activos],
  )

  const filasDepreciaciones = useMemo<FilaTabla[]>(
    () =>
      depreciaciones.map((depreciacion) => ({
        id: depreciacion.id,
        celdas: {
          periodo: depreciacion.periodo,
          fecha: depreciacion.fecha,
          codigoActivo: depreciacion.codigoActivo ?? '—',
          valor: money(depreciacion.valor),
          depreciacionAcumulada: money(depreciacion.depreciacionAcumulada),
          valorLibros: money(depreciacion.valorLibros),
          estado: <EstadoChip estado={depreciacion.estado} />,
        },
      })),
    [depreciaciones],
  )

  const crearActivo = useCallback(async () => {
    if (empresaId == null) return
    const adquisicion = numero(valorAdquisicion)
    const residual = numero(valorResidual) ?? 0
    const vida = numero(vidaUtilMeses)
    if (!codigo.trim() || !nombre.trim() || adquisicion == null || adquisicion <= 0 || vida == null || vida <= 0) {
      setError('Complete código, nombre, valor de adquisición y vida útil del activo.')
      return
    }
    if (residual < 0 || residual >= adquisicion) {
      setError('El valor residual debe ser menor que el valor de adquisición.')
      return
    }
    setGuardando(true)
    setError(null)
    try {
      const creado = await crearActivoErp(empresaId, {
        codigo: codigo.trim(),
        nombre: nombre.trim(),
        categoria: categoria.trim() || null,
        fechaAlta: fechaAlta || hoy(),
        valorAdquisicion: adquisicion,
        valorResidual: residual,
        vidaUtilMeses: vida,
        observaciones: null,
      })
      setSeleccionadoId(creado.id)
      await cargarDatos()
      notificar(`Activo ${creado.codigo} registrado.`, 'success')
    } catch (error) {
      setError(mensajeBackend(error, 'No se pudo registrar el activo fijo.'))
    } finally {
      setGuardando(false)
    }
  }, [empresaId, codigo, nombre, categoria, fechaAlta, valorAdquisicion, valorResidual, vidaUtilMeses, cargarDatos, notificar])

  const depreciar = useCallback(async () => {
    if (empresaId == null || activoSeleccionado == null) return
    if (!periodo.trim()) {
      setError('Ingrese el periodo de depreciación.')
      return
    }
    setGuardando(true)
    setError(null)
    try {
      const depreciacion = await depreciarActivoErp(empresaId, activoSeleccionado.id, {
        periodo: periodo.trim(),
        fecha: fechaDepreciacion || hoy(),
      })
      await cargarDatos()
      notificar(`Depreciación de ${depreciacion.periodo} registrada por ${money(depreciacion.valor)}.`, 'success')
    } catch {
      setError('No se pudo registrar la depreciación.')
    } finally {
      setGuardando(false)
    }
  }, [empresaId, activoSeleccionado, periodo, fechaDepreciacion, cargarDatos, notificar])

  const darBaja = useCallback(async () => {
    if (empresaId == null || activoSeleccionado == null) return
    setGuardando(true)
    setError(null)
    try {
      await darBajaActivoErp(empresaId, activoSeleccionado.id, {
        fechaBaja: hoy(),
        motivo: motivoBaja.trim() || null,
      })
      await cargarDatos()
      notificar(`Activo ${activoSeleccionado.codigo} dado de baja.`, 'success')
    } catch {
      setError('No se pudo dar de baja el activo.')
    } finally {
      setGuardando(false)
    }
  }, [empresaId, activoSeleccionado, motivoBaja, cargarDatos, notificar])

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      {empresaId == null && !cargandoEmpresa && (
        <Alert severity="info">No hay una empresa asociada para consultar los activos fijos.</Alert>
      )}
      {error && <Alert severity="error">{error}</Alert>}

      <SectionCard
        title="Registrar activo fijo"
        subtitle="El valor libros inicial equivale al valor de adquisición"
      >
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 2 }}>
            <TextField fullWidth label="Código" value={codigo} onChange={(evento) => setCodigo(evento.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField fullWidth label="Nombre" value={nombre} onChange={(evento) => setNombre(evento.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 2 }}>
            <TextField fullWidth label="Categoría" value={categoria} onChange={(evento) => setCategoria(evento.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 2 }}>
            <TextField
              fullWidth
              label="Fecha de alta"
              type="date"
              value={fechaAlta}
              slotProps={{ inputLabel: { shrink: true } }}
              onChange={(evento) => setFechaAlta(evento.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 2 }}>
            <TextField
              fullWidth
              label="Valor de adquisición"
              type="number"
              value={valorAdquisicion}
              onChange={(evento) => setValorAdquisicion(evento.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 2 }}>
            <TextField
              fullWidth
              label="Valor residual"
              type="number"
              value={valorResidual}
              onChange={(evento) => setValorResidual(evento.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 2 }}>
            <TextField
              fullWidth
              label="Vida útil (meses)"
              type="number"
              value={vidaUtilMeses}
              onChange={(evento) => setVidaUtilMeses(evento.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <Button variant="contained" loading={guardando} disabled={empresaId == null} onClick={() => void crearActivo()}>
              Registrar activo
            </Button>
          </Grid>
        </Grid>
      </SectionCard>

      <SectionCard
        title="Depreciar activo"
        subtitle={`Activo seleccionado: ${activoSeleccionado ? activoSeleccionado.codigo : 'ninguno'}`}
      >
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField
              fullWidth
              label="Periodo"
              placeholder="YYYY-MM"
              value={periodo}
              onChange={(evento) => setPeriodo(evento.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField
              fullWidth
              label="Fecha"
              type="date"
              value={fechaDepreciacion}
              slotProps={{ inputLabel: { shrink: true } }}
              onChange={(evento) => setFechaDepreciacion(evento.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
            <TextField
              fullWidth
              label="Motivo de baja"
              value={motivoBaja}
              onChange={(evento) => setMotivoBaja(evento.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
              <Button
                variant="contained"
                loading={guardando}
                disabled={empresaId == null || activoSeleccionado == null}
                onClick={() => void depreciar()}
              >
                Calcular depreciación
              </Button>
              <Button
                variant="outlined"
                loading={guardando}
                disabled={empresaId == null || activoSeleccionado == null || activoSeleccionado.estado === 'BAJA'}
                onClick={() => void darBaja()}
              >
                Dar de baja
              </Button>
            </Stack>
          </Grid>
        </Grid>
      </SectionCard>

      <Alert severity="info">
        Cada depreciación calculada genera su asiento automático con cargo a gasto y abono a depreciación acumulada.
      </Alert>

      <SectionCard title="Activos registrados" subtitle="Seleccione un activo para depreciarlo o darlo de baja" padded={false}>
        <TablaDatos
          ariaLabel="Activos fijos"
          columnas={COLUMNAS_ACTIVOS}
          filas={filasActivos}
          cargando={cargando || cargandoEmpresa}
          minWidth={980}
          tituloVacio="Sin activos fijos"
          descripcionVacio="Registre el primer activo para comenzar la depreciación."
          onSeleccionar={(id) => setSeleccionadoId(Number(id))}
          seleccionado={seleccionadoId}
        />
      </SectionCard>

      <SectionCard title="Depreciaciones registradas" subtitle="Movimientos de depreciación con trazabilidad contable" padded={false}>
        <TablaDatos
          ariaLabel="Depreciaciones"
          columnas={COLUMNAS_DEPRECIACIONES}
          filas={filasDepreciaciones}
          cargando={cargando || cargandoEmpresa}
          minWidth={900}
          tituloVacio="Sin depreciaciones"
          descripcionVacio="Las depreciaciones calculadas aparecerán aquí."
        />
      </SectionCard>
    </Box>
  )
}
