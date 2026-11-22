import { useMemo, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid2'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import CloudSyncOutlinedIcon from '@mui/icons-material/CloudSyncOutlined'
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline'
import ReplayIcon from '@mui/icons-material/Replay'
import TaskAltOutlinedIcon from '@mui/icons-material/TaskAltOutlined'
import {
  migrarContabilidad,
  obtenerReporteMigracion,
  PROVEEDORES_INTEGRACION,
  reintentarMigracion,
} from '../api/client'
import type {
  MigracionContableDto,
  ProveedorIntegracion,
  ResultadoMigracionDto,
} from '../api/client'
import ConfirmDialog from '../components/ui/ConfirmDialog'
import EmptyState from '../components/ui/EmptyState'
import EstadoChip from '../components/ui/EstadoChip'
import LoadingState from '../components/ui/LoadingState'
import PageHeader from '../components/ui/PageHeader'
import SectionCard from '../components/ui/SectionCard'
import StatCard from '../components/ui/StatCard'
import { useEmpresaActiva } from '../features/auth/useEmpresaActiva'
import { useNotificaciones } from '../features/notificaciones/contexto'

export default function MigracionContablePage() {
  const { empresaId, cargando: cargandoEmpresa } = useEmpresaActiva()
  const { notificar } = useNotificaciones()
  const [proveedor, setProveedor] = useState<ProveedorIntegracion>('SIIGO')
  const [confirmando, setConfirmando] = useState(false)
  const [procesando, setProcesando] = useState(false)
  const [cargandoReporte, setCargandoReporte] = useState(false)
  const [migracionId, setMigracionId] = useState<number | null>(null)
  const [migracion, setMigracion] = useState<MigracionContableDto | null>(null)
  const [reporte, setReporte] = useState<ResultadoMigracionDto | null>(null)
  const [error, setError] = useState<string | null>(null)

  const itemsConError = useMemo(
    () => (reporte?.items ?? []).filter((item) => item.estado === 'ERROR' || Boolean(item.error)),
    [reporte],
  )

  const cargarReporte = async (id: number, proveedorActual: ProveedorIntegracion) => {
    if (empresaId == null) return
    setCargandoReporte(true)
    try {
      setReporte(await obtenerReporteMigracion(empresaId, proveedorActual, id))
    } finally {
      setCargandoReporte(false)
    }
  }

  const onMigrar = async () => {
    if (empresaId == null) return
    setProcesando(true)
    setError(null)
    try {
      const resultado = await migrarContabilidad(empresaId, proveedor)
      setMigracionId(resultado.id)
      setMigracion(resultado)
      notificar(`Migración ${proveedor} iniciada.`, 'success')
      await cargarReporte(resultado.id, proveedor)
    } finally {
      setProcesando(false)
      setConfirmando(false)
    }
  }

  const onReintentar = async () => {
    if (empresaId == null || migracionId == null) return
    setProcesando(true)
    setError(null)
    try {
      const resultado = await reintentarMigracion(empresaId, proveedor, migracionId)
      setMigracion(resultado)
      notificar('Migración reintentada.', 'success')
      await cargarReporte(migracionId, proveedor)
    } finally {
      setProcesando(false)
    }
  }

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="Integraciones"
        title="Migración contable"
        subtitle="Migre plan de cuentas, saldos, terceros y asientos desde su sistema contable actual."
        actions={
          <Stack direction="row" spacing={1}>
            <Button
              variant="outlined"
              startIcon={<ReplayIcon />}
              disabled={empresaId == null || migracionId == null || procesando}
              loading={procesando}
              onClick={onReintentar}
            >
              Reintentar
            </Button>
            <Button
              variant="contained"
              startIcon={<CloudSyncOutlinedIcon />}
              disabled={empresaId == null || procesando}
              onClick={() => setConfirmando(true)}
            >
              Migrar
            </Button>
          </Stack>
        }
      />

      <SectionCard title="Origen de la migración" subtitle="Seleccione el proveedor y confirme la operación">
        <Grid container spacing={2} sx={{ alignItems: 'center' }}>
          <Grid size={{ xs: 12, md: 6 }}>
            <TextField
              select
              label="Proveedor"
              value={proveedor}
              onChange={(event) => setProveedor(event.target.value as ProveedorIntegracion)}
            >
              {PROVEEDORES_INTEGRACION.map((valor) => (
                <MenuItem key={valor} value={valor}>
                  {valor}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
            <Button
              variant="outlined"
              disabled={empresaId == null || migracionId == null || cargandoReporte}
              onClick={() => migracionId != null && cargarReporte(migracionId, proveedor)}
            >
              Ver reporte de la última migración
            </Button>
          </Grid>
        </Grid>
      </SectionCard>

      {empresaId == null && !cargandoEmpresa && (
        <Alert severity="info">No hay una empresa asociada para migrar información contable.</Alert>
      )}
      {error && <Alert severity="error">{error}</Alert>}

      {cargandoReporte ? (
        <LoadingState label="Cargando reporte de migración…" compacto />
      ) : reporte ? (
        <>
          <Grid container spacing={2}>
            <Grid size={{ xs: 6, md: 3 }}>
              <StatCard label="Exitosos" value={reporte.exitosos} tone="success" icon={<TaskAltOutlinedIcon />} />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <StatCard label="Omitidos" value={reporte.omitidos} tone="warning" />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <StatCard label="Errores" value={reporte.errores} tone="error" icon={<ErrorOutlineIcon />} />
            </Grid>
            <Grid size={{ xs: 6, md: 3 }}>
              <StatCard label="Total" value={reporte.total} tone="info" />
            </Grid>
          </Grid>

          <SectionCard
            title={`Migración #${migracion?.id ?? migracionId ?? '—'}`}
            subtitle={
              migracion
                ? `${migracion.proveedor} · ${migracion.etapas.join(' → ')}`
                : `Reporte de ${proveedor}`
            }
            actions={migracion ? <EstadoChip estado={migracion.estado} /> : undefined}
            padded={false}
          >
            {migracion?.mensaje && (
              <Box sx={{ px: 3, pt: 2 }}>
                <Alert severity="info">{migracion.mensaje}</Alert>
              </Box>
            )}
            {reporte.items.length === 0 ? (
              <EmptyState icon={<TaskAltOutlinedIcon />} title="Sin detalles en el reporte" />
            ) : (
              <TableContainer>
                <Table size="small" aria-label="Detalles de la migración" sx={{ minWidth: 720 }}>
                  <TableHead>
                    <TableRow>
                      <TableCell>Etapa</TableCell>
                      <TableCell>Identificador externo</TableCell>
                      <TableCell>Estado</TableCell>
                      <TableCell>Error</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {reporte.items.map((item) => (
                      <TableRow key={`${item.id}-${item.externalId}`}>
                        <TableCell>{item.etapa}</TableCell>
                        <TableCell>{item.externalId}</TableCell>
                        <TableCell>
                          <EstadoChip estado={item.estado} />
                        </TableCell>
                        <TableCell>{item.error ?? '—'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </SectionCard>

          {itemsConError.length > 0 && (
            <Alert severity="warning">
              {itemsConError.length} registro(s) requieren revisión. Puede reintentar la migración.
            </Alert>
          )}
        </>
      ) : (
        <EmptyState
          icon={<CloudSyncOutlinedIcon />}
          title="Sin migraciones consultadas"
          description="Ejecute una migración o consulte el reporte de la última operación."
        />
      )}

      <ConfirmDialog
        abierto={confirmando}
        titulo="Migrar contabilidad"
        mensaje={`Se iniciará la migración contable desde ${proveedor}. ¿Desea continuar?`}
        textoConfirmar="Migrar"
        color="primary"
        cargando={procesando}
        onConfirmar={onMigrar}
        onCancelar={() => setConfirmando(false)}
      />
    </Box>
  )
}
