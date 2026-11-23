import { useCallback, useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import FormControl from '@mui/material/FormControl'
import Grid from '@mui/material/Grid2'
import InputLabel from '@mui/material/InputLabel'
import MenuItem from '@mui/material/MenuItem'
import Pagination from '@mui/material/Pagination'
import Select from '@mui/material/Select'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined'
import {
  listarAuxiliaresContables,
  listarLibroDiario,
  listarLibroMayor,
  type AsientoContable,
  type MovimientoContable,
  type Pagina,
} from '../../api/client'
import SectionCard from '../../components/ui/SectionCard'
import { money } from '../facturacion/calculos'
import TablaDatos, { type ColumnaTabla, type FilaTabla } from './TablaDatos'

type LibrosTabProps = {
  empresaId: number | null
  cargandoEmpresa: boolean
}

type TipoLibro = 'diario' | 'mayor' | 'auxiliares'

const COLUMNAS_DIARIO: ColumnaTabla[] = [
  { id: 'numero', label: 'Número' },
  { id: 'fecha', label: 'Fecha' },
  { id: 'descripcion', label: 'Descripción' },
  { id: 'debito', label: 'Débito', align: 'right' },
  { id: 'credito', label: 'Crédito', align: 'right' },
]

const COLUMNAS_MOVIMIENTOS: ColumnaTabla[] = [
  { id: 'fecha', label: 'Fecha' },
  { id: 'codigoCuenta', label: 'Cuenta' },
  { id: 'nombreCuenta', label: 'Nombre' },
  { id: 'numeroAsiento', label: 'Asiento' },
  { id: 'descripcion', label: 'Descripción' },
  { id: 'debito', label: 'Débito', align: 'right' },
  { id: 'credito', label: 'Crédito', align: 'right' },
  { id: 'saldo', label: 'Saldo', align: 'right' },
]

export default function LibrosTab({ empresaId, cargandoEmpresa }: LibrosTabProps) {
  const [tipo, setTipo] = useState<TipoLibro>('diario')
  const [fechaDesde, setFechaDesde] = useState('')
  const [fechaHasta, setFechaHasta] = useState('')
  const [codigoCuenta, setCodigoCuenta] = useState('')
  const [paginaActual, setPaginaActual] = useState(0)
  const [diario, setDiario] = useState<Pagina<AsientoContable> | null>(null)
  const [movimientos, setMovimientos] = useState<Pagina<MovimientoContable> | null>(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const consultar = useCallback(async () => {
    if (empresaId == null) {
      setCargando(false)
      return
    }
    setCargando(true)
    setError(null)
    const filtros = {
      fechaDesde: fechaDesde || undefined,
      fechaHasta: fechaHasta || undefined,
      codigoCuenta: codigoCuenta || undefined,
      pagina: paginaActual,
      tamano: 20,
    }
    try {
      if (tipo === 'diario') {
        setDiario(await listarLibroDiario(empresaId, filtros))
      } else if (tipo === 'mayor') {
        setMovimientos(await listarLibroMayor(empresaId, filtros))
      } else {
        setMovimientos(await listarAuxiliaresContables(empresaId, filtros))
      }
    } catch {
      setError('No se pudo consultar el libro contable.')
    } finally {
      setCargando(false)
    }
  }, [codigoCuenta, empresaId, fechaDesde, fechaHasta, paginaActual, tipo])

  useEffect(() => {
    consultar()
  }, [consultar])

  const totalPaginas =
    tipo === 'diario' ? (diario?.totalPaginas ?? 0) : (movimientos?.totalPaginas ?? 0)

  const filas: FilaTabla[] =
    tipo === 'diario'
      ? (diario?.contenido ?? []).map((asiento) => ({
          id: asiento.id,
          celdas: {
            numero: asiento.numero,
            fecha: asiento.fecha,
            descripcion: asiento.descripcion,
            debito: money(asiento.totalDebito),
            credito: money(asiento.totalCredito),
          },
        }))
      : (movimientos?.contenido ?? []).map((movimiento) => ({
          id: `${movimiento.numeroAsiento}-${movimiento.codigoCuenta}-${movimiento.fecha}`,
          celdas: {
            fecha: movimiento.fecha,
            codigoCuenta: movimiento.codigoCuenta,
            nombreCuenta: movimiento.nombreCuenta,
            numeroAsiento: movimiento.numeroAsiento,
            descripcion: movimiento.descripcion ?? '—',
            debito: money(movimiento.debito),
            credito: money(movimiento.credito),
            saldo: money(movimiento.saldo),
          },
        }))

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      {error && <Alert severity="error">{error}</Alert>}
      <SectionCard title="Filtros" subtitle="Rango de fechas y tipo de libro">
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 4 }}>
            <FormControl fullWidth size="small">
              <InputLabel id="tipo-libro-label">Libro</InputLabel>
              <Select
                labelId="tipo-libro-label"
                value={tipo}
                onChange={(event) => {
                  setTipo(event.target.value as TipoLibro)
                  setPaginaActual(0)
                }}
              >
                <MenuItem value="diario">Libro diario</MenuItem>
                <MenuItem value="mayor">Libro mayor</MenuItem>
                <MenuItem value="auxiliares">Auxiliares</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField
              fullWidth
              size="small"
              type="date"
              label="Desde"
              slotProps={{ inputLabel: { shrink: true } }}
              value={fechaDesde}
              onChange={(event) => {
                setFechaDesde(event.target.value)
                setPaginaActual(0)
              }}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField
              fullWidth
              size="small"
              type="date"
              label="Hasta"
              slotProps={{ inputLabel: { shrink: true } }}
              value={fechaHasta}
              onChange={(event) => {
                setFechaHasta(event.target.value)
                setPaginaActual(0)
              }}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 2 }}>
            <Button fullWidth variant="contained" onClick={consultar} disabled={empresaId == null}>
              Consultar
            </Button>
          </Grid>
          {tipo !== 'diario' && (
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField
                fullWidth
                size="small"
                label="Código de cuenta"
                value={codigoCuenta}
                onChange={(event) => {
                  setCodigoCuenta(event.target.value)
                  setPaginaActual(0)
                }}
              />
            </Grid>
          )}
        </Grid>
      </SectionCard>

      <SectionCard
        title={tipo === 'diario' ? 'Libro diario' : tipo === 'mayor' ? 'Libro mayor' : 'Libros auxiliares'}
        subtitle="Movimientos contabilizados del periodo seleccionado"
        padded={false}
      >
        <Stack spacing={2} sx={{ p: { xs: 2, md: 3 } }}>
          <TablaDatos
            ariaLabel="Movimientos contables"
            columnas={tipo === 'diario' ? COLUMNAS_DIARIO : COLUMNAS_MOVIMIENTOS}
            filas={filas}
            cargando={cargando || cargandoEmpresa}
            iconoVacio={<MenuBookOutlinedIcon />}
            tituloVacio="Sin movimientos"
            descripcionVacio="No hay movimientos contabilizados para los filtros seleccionados."
          />
          {totalPaginas > 1 && (
            <Pagination
              count={totalPaginas}
              page={paginaActual + 1}
              onChange={(_, pagina) => setPaginaActual(pagina - 1)}
              color="primary"
              shape="rounded"
            />
          )}
        </Stack>
      </SectionCard>
    </Box>
  )
}
