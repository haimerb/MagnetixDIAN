import { useCallback, useEffect, useMemo, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid2'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import AccountBalanceWalletOutlinedIcon from '@mui/icons-material/AccountBalanceWalletOutlined'
import BalanceOutlinedIcon from '@mui/icons-material/BalanceOutlined'
import FactCheckOutlinedIcon from '@mui/icons-material/FactCheckOutlined'
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined'
import { conciliarContabilidad, listarAsientosContables } from '../api/client'
import type { AsientoContable, ResultadoConciliacion } from '../api/client'
import EmptyState from '../components/ui/EmptyState'
import PageHeader from '../components/ui/PageHeader'
import SectionCard from '../components/ui/SectionCard'
import StatCard from '../components/ui/StatCard'
import TableSkeleton from '../components/ui/TableSkeleton'
import { useEmpresaActiva } from '../features/auth/useEmpresaActiva'
import { useNotificaciones } from '../features/notificaciones/contexto'
import { money } from '../features/facturacion/calculos'

export default function ContabilidadPage() {
  const { empresaId, cargando: cargandoEmpresa } = useEmpresaActiva()
  const { notificar } = useNotificaciones()
  const [asientos, setAsientos] = useState<AsientoContable[]>([])
  const [seleccionado, setSeleccionado] = useState<number | null>(null)
  const [conciliacion, setConciliacion] = useState<ResultadoConciliacion | null>(null)
  const [cargando, setCargando] = useState(true)
  const [conciliando, setConciliando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const cargar = useCallback(() => {
    if (empresaId == null) {
      setCargando(false)
      return
    }
    setCargando(true)
    setError(null)
    listarAsientosContables(empresaId)
      .then((datos) => {
        setAsientos(datos)
        setSeleccionado((actual) => actual ?? datos[0]?.id ?? null)
      })
      .catch(() => setError('No se pudieron cargar los asientos contables.'))
      .finally(() => setCargando(false))
  }, [empresaId])

  useEffect(() => {
    cargar()
  }, [cargar])

  const detalle = useMemo(
    () => asientos.find((asiento) => asiento.id === seleccionado) ?? null,
    [asientos, seleccionado],
  )

  const totales = useMemo(
    () => ({
      debito: asientos.reduce((suma, asiento) => suma + (asiento.totalDebito ?? 0), 0),
      credito: asientos.reduce((suma, asiento) => suma + (asiento.totalCredito ?? 0), 0),
    }),
    [asientos],
  )

  const onConciliar = async () => {
    if (empresaId == null) return
    setConciliando(true)
    setError(null)
    try {
      setConciliacion(await conciliarContabilidad(empresaId))
      notificar('Conciliación contable completada.', 'success')
    } finally {
      setConciliando(false)
    }
  }

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="Contabilidad"
        title="Asientos contables"
        subtitle="Revise los asientos generados desde RADIAN y concilie los saldos contra los documentos."
        actions={
          <Button
            variant="contained"
            startIcon={<BalanceOutlinedIcon />}
            disabled={empresaId == null || conciliando}
            loading={conciliando}
            onClick={onConciliar}
          >
            Conciliar
          </Button>
        }
      />

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <StatCard
            label="Asientos"
            value={asientos.length}
            icon={<ReceiptLongOutlinedIcon />}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <StatCard label="Total débito" value={money(totales.debito)} tone="info" icon={<AccountBalanceWalletOutlinedIcon />} />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <StatCard label="Total crédito" value={money(totales.credito)} tone="success" icon={<FactCheckOutlinedIcon />} />
        </Grid>
      </Grid>

      {empresaId == null && !cargandoEmpresa && (
        <Alert severity="info">No hay una empresa asociada para consultar la contabilidad.</Alert>
      )}
      {error && <Alert severity="error">{error}</Alert>}

      {conciliacion && (
        <SectionCard title="Resultado de la conciliación" subtitle="Comparación entre documentos y asientos">
          <Grid container spacing={2}>
            <Grid size={{ xs: 6, md: 3 }}>
              <StatCard label="Coincidencias" value={conciliacion.coincidencias} tone="success" />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <StatCard label="Diferencias" value={conciliacion.diferencias} tone="error" />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <StatCard label="Documentos" value={conciliacion.totalDocumentos} tone="info" />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <StatCard label="Sin asiento" value={conciliacion.documentosSinAsiento} />
            </Grid>
          </Grid>
        </SectionCard>
      )}

      <SectionCard title="Asientos" subtitle="Seleccione un asiento para ver sus líneas" padded={false}>
        {cargando || cargandoEmpresa ? (
          <TableSkeleton filas={5} columnas={5} label="Cargando asientos contables…" />
        ) : asientos.length === 0 ? (
          <EmptyState
            icon={<ReceiptLongOutlinedIcon />}
            title="Sin asientos contables"
            description="Cause documentos RADIAN y genere sus asientos desde la bandeja de RADIAN."
          />
        ) : (
          <TableContainer>
            <Table size="small" aria-label="Asientos contables" sx={{ minWidth: 640 }}>
              <TableHead>
                <TableRow>
                  <TableCell>Número</TableCell>
                  <TableCell>Fecha</TableCell>
                  <TableCell>Evento</TableCell>
                  <TableCell>Descripción</TableCell>
                  <TableCell align="right">Débito</TableCell>
                  <TableCell align="right">Crédito</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {asientos.map((asiento) => (
                  <TableRow
                    key={asiento.id}
                    hover
                    selected={asiento.id === seleccionado}
                    tabIndex={0}
                    onClick={() => setSeleccionado(asiento.id)}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault()
                        setSeleccionado(asiento.id)
                      }
                    }}
                    sx={{ cursor: 'pointer' }}
                  >
                    <TableCell sx={{ fontWeight: 600 }}>{asiento.numero}</TableCell>
                    <TableCell>{asiento.fecha}</TableCell>
                    <TableCell>{asiento.tipoEvento}</TableCell>
                    <TableCell>{asiento.descripcion}</TableCell>
                    <TableCell align="right">{money(asiento.totalDebito)}</TableCell>
                    <TableCell align="right">{money(asiento.totalCredito)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </SectionCard>

      <SectionCard title="Líneas del asiento" subtitle={detalle ? `${detalle.numero} · ${detalle.descripcion}` : undefined} padded={false}>
        {detalle == null ? (
          <EmptyState icon={<FactCheckOutlinedIcon />} title="Sin asiento seleccionado" />
        ) : detalle.lineas.length === 0 ? (
          <EmptyState icon={<FactCheckOutlinedIcon />} title="El asiento no tiene líneas" />
        ) : (
          <TableContainer>
            <Table size="small" aria-label="Líneas del asiento" sx={{ minWidth: 640 }}>
              <TableHead>
                <TableRow>
                  <TableCell>Cuenta</TableCell>
                  <TableCell>Nombre</TableCell>
                  <TableCell>Descripción</TableCell>
                  <TableCell align="right">Débito</TableCell>
                  <TableCell align="right">Crédito</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {detalle.lineas.map((linea) => (
                  <TableRow key={linea.id}>
                    <TableCell>{linea.codigoCuenta}</TableCell>
                    <TableCell>{linea.nombreCuenta}</TableCell>
                    <TableCell>{linea.descripcion ?? '—'}</TableCell>
                    <TableCell align="right">{money(linea.debito)}</TableCell>
                    <TableCell align="right">{money(linea.credito)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </SectionCard>
    </Box>
  )
}
