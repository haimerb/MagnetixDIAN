import { useCallback, useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import FormControl from '@mui/material/FormControl'
import Grid from '@mui/material/Grid2'
import InputLabel from '@mui/material/InputLabel'
import MenuItem from '@mui/material/MenuItem'
import Select from '@mui/material/Select'
import TextField from '@mui/material/TextField'
import BalanceOutlinedIcon from '@mui/icons-material/BalanceOutlined'
import {
  obtenerBalanceComprobacion,
  obtenerBalanceGeneral,
  obtenerEstadoResultados,
  obtenerFlujoEfectivo,
  type BalanceComprobacion,
  type EstadoFinanciero,
  type FlujoEfectivo,
} from '../../api/client'
import SectionCard from '../../components/ui/SectionCard'
import StatCard from '../../components/ui/StatCard'
import { money } from '../facturacion/calculos'
import TablaDatos, { type ColumnaTabla, type FilaTabla } from './TablaDatos'

type BalancesTabProps = {
  empresaId: number | null
  cargandoEmpresa: boolean
}

type TipoBalance = 'comprobacion' | 'general' | 'resultados' | 'flujo'

const COLUMNAS_SALDOS: ColumnaTabla[] = [
  { id: 'grupo', label: 'Grupo' },
  { id: 'codigo', label: 'Código' },
  { id: 'nombre', label: 'Cuenta' },
  { id: 'saldoInicial', label: 'Inicial', align: 'right' },
  { id: 'debito', label: 'Débito', align: 'right' },
  { id: 'credito', label: 'Crédito', align: 'right' },
  { id: 'saldoFinal', label: 'Final', align: 'right' },
]

const COLUMNAS_FLUJO: ColumnaTabla[] = [
  { id: 'fecha', label: 'Fecha' },
  { id: 'codigoCuenta', label: 'Cuenta' },
  { id: 'numeroAsiento', label: 'Asiento' },
  { id: 'descripcion', label: 'Descripción' },
  { id: 'debito', label: 'Entrada', align: 'right' },
  { id: 'credito', label: 'Salida', align: 'right' },
  { id: 'saldo', label: 'Acumulado', align: 'right' },
]

function hoy() {
  return new Date().toISOString().slice(0, 10)
}

export default function BalancesTab({ empresaId, cargandoEmpresa }: BalancesTabProps) {
  const [tipo, setTipo] = useState<TipoBalance>('comprobacion')
  const [fechaDesde, setFechaDesde] = useState(hoy)
  const [fechaHasta, setFechaHasta] = useState(hoy)
  const [comprobacion, setComprobacion] = useState<BalanceComprobacion | null>(null)
  const [estado, setEstado] = useState<EstadoFinanciero | null>(null)
  const [flujo, setFlujo] = useState<FlujoEfectivo | null>(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const consultar = useCallback(async () => {
    if (empresaId == null) {
      setCargando(false)
      return
    }
    setCargando(true)
    setError(null)
    try {
      if (tipo === 'comprobacion') {
        setComprobacion(await obtenerBalanceComprobacion(empresaId, fechaDesde, fechaHasta))
      } else if (tipo === 'general') {
        setEstado(await obtenerBalanceGeneral(empresaId, fechaHasta))
      } else if (tipo === 'resultados') {
        setEstado(await obtenerEstadoResultados(empresaId, fechaDesde, fechaHasta))
      } else {
        setFlujo(await obtenerFlujoEfectivo(empresaId, fechaDesde, fechaHasta))
      }
    } catch {
      setError('No se pudo consultar el reporte contable.')
    } finally {
      setCargando(false)
    }
  }, [empresaId, fechaDesde, fechaHasta, tipo])

  useEffect(() => {
    consultar()
  }, [consultar])

  const filas = comprobacion
    ? comprobacion.cuentas.map((cuenta) => ({
        id: cuenta.codigo,
        celdas: {
          grupo: cuenta.tipo,
          codigo: cuenta.codigo,
          nombre: cuenta.nombre,
          saldoInicial: money(cuenta.saldoInicial),
          debito: money(cuenta.debito),
          credito: money(cuenta.credito),
          saldoFinal: money(cuenta.saldoFinal),
        },
      }))
    : (estado?.grupos ?? []).flatMap((grupo) =>
        grupo.cuentas.map((cuenta) => ({
          id: `${grupo.nombre}-${cuenta.codigo}`,
          celdas: {
            grupo: grupo.nombre,
            codigo: cuenta.codigo,
            nombre: cuenta.nombre,
            saldoInicial: money(cuenta.saldoInicial),
            debito: money(cuenta.debito),
            credito: money(cuenta.credito),
            saldoFinal: money(cuenta.saldoFinal),
          },
        })),
      )

  const filasFlujo: FilaTabla[] = (flujo?.movimientos ?? []).map((movimiento) => ({
    id: `${movimiento.numeroAsiento}-${movimiento.codigoCuenta}-${movimiento.fecha}`,
    celdas: {
      fecha: movimiento.fecha,
      codigoCuenta: movimiento.codigoCuenta,
      numeroAsiento: movimiento.numeroAsiento,
      descripcion: movimiento.descripcion ?? '—',
      debito: money(movimiento.debito),
      credito: money(movimiento.credito),
      saldo: money(movimiento.saldo),
    },
  }))

  const total = tipo === 'comprobacion'
    ? (comprobacion?.totalSaldoFinal ?? 0)
    : (estado?.total ?? flujo?.neto ?? 0)

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      {error && <Alert severity="error">{error}</Alert>}
      <SectionCard title="Filtros" subtitle="Tipo de reporte y periodo">
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 4 }}>
            <FormControl fullWidth size="small">
              <InputLabel id="tipo-balance-label">Reporte</InputLabel>
              <Select
                labelId="tipo-balance-label"
                value={tipo}
                onChange={(event) => setTipo(event.target.value as TipoBalance)}
              >
                <MenuItem value="comprobacion">Balance de comprobación</MenuItem>
                <MenuItem value="general">Balance general</MenuItem>
                <MenuItem value="resultados">Estado de resultados</MenuItem>
                <MenuItem value="flujo">Flujo de efectivo</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          {tipo !== 'general' && (
            <Grid size={{ xs: 12, md: 3 }}>
              <TextField
                fullWidth
                size="small"
                type="date"
                label="Desde"
                slotProps={{ inputLabel: { shrink: true } }}
                value={fechaDesde}
                onChange={(event) => setFechaDesde(event.target.value)}
              />
            </Grid>
          )}
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField
              fullWidth
              size="small"
              type="date"
              label={tipo === 'general' ? 'Fecha de corte' : 'Hasta'}
              slotProps={{ inputLabel: { shrink: true } }}
              value={fechaHasta}
              onChange={(event) => setFechaHasta(event.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 2 }}>
            <Button fullWidth variant="contained" onClick={consultar} disabled={empresaId == null}>
              Consultar
            </Button>
          </Grid>
        </Grid>
      </SectionCard>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 4 }}>
          <StatCard label="Total del reporte" value={money(total)} icon={<BalanceOutlinedIcon />} />
        </Grid>
      </Grid>

      <SectionCard title="Detalle" subtitle="Saldos por cuenta del reporte seleccionado" padded={false}>
        <Box sx={{ p: { xs: 2, md: 3 } }}>
          <TablaDatos
            ariaLabel="Detalle del balance"
            columnas={tipo === 'flujo' ? COLUMNAS_FLUJO : COLUMNAS_SALDOS}
            filas={tipo === 'flujo' ? filasFlujo : filas}
            cargando={cargando || cargandoEmpresa}
            iconoVacio={<BalanceOutlinedIcon />}
            tituloVacio="Sin saldos para el periodo"
            descripcionVacio="Asiente la facturación para generar movimientos contables."
          />
        </Box>
      </SectionCard>
    </Box>
  )
}
