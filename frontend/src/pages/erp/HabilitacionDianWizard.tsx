import { useMemo, useState } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import FormControlLabel from '@mui/material/FormControlLabel'
import Grid from '@mui/material/Grid2'
import Radio from '@mui/material/Radio'
import RadioGroup from '@mui/material/RadioGroup'
import Step from '@mui/material/Step'
import StepLabel from '@mui/material/StepLabel'
import Stepper from '@mui/material/Stepper'
import Switch from '@mui/material/Switch'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import type { HabilitacionDian, ResultadoTestSetDian } from '../../api/client'
import {
  CONFIRMACION_SWITCHOVER,
  consultarEstadoTestSet,
  enviarDocumentosTestSet,
  guardarHabilitacionDian,
  MODOS_OPERACION_DIAN,
  modoPorDefecto,
  switchoverProduccion,
} from '../../api/erp/dian'
import EmptyState from '../../components/ui/EmptyState'
import EstadoChip from '../../components/ui/EstadoChip'
import LoadingState from '../../components/ui/LoadingState'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import { useEmpresaActiva } from '../../features/auth/useEmpresaActiva'
import { DOCUMENTOS_TEST_SET } from '../../features/erp/testSet'
import { useHabilitacionDian } from '../../features/erp/useHabilitacionDian'
import { useNotificaciones } from '../../features/notificaciones/contexto'

const PASOS = ['Modo de operación', 'Certificado y resolución', 'TEST SET', 'Switchover']

export default function HabilitacionDianWizard() {
  const { empresaId, cargando: cargandoEmpresa } = useEmpresaActiva()
  const { habilitacion, cargando, error, puedeEmitir, testSetAprobado, recargar, setHabilitacion } =
    useHabilitacionDian(empresaId)
  const { notificar } = useNotificaciones()

  const [paso, setPaso] = useState(0)
  const [modo, setModo] = useState<string>(() => habilitacion?.modo ?? modoPorDefecto())
  const [resolucionNumero, setResolucionNumero] = useState(() => habilitacion?.resolucionNumero ?? '')
  const [resolucionFecha, setResolucionFecha] = useState(() => habilitacion?.resolucionFecha ?? '')
  const [prefijo, setPrefijo] = useState(() => habilitacion?.prefijo ?? '')
  const [rangoDesde, setRangoDesde] = useState(() => (habilitacion?.rangoDesde ?? '') as number | string)
  const [rangoHasta, setRangoHasta] = useState(() => (habilitacion?.rangoHasta ?? '') as number | string)
  const [testSetId, setTestSetId] = useState(() => habilitacion?.testSetId ?? '')
  const [softwareId, setSoftwareId] = useState(() => habilitacion?.softwareId ?? '')
  const [notificarAdquirente, setNotificarAdquirente] = useState(
    () => habilitacion?.notificarAdquirente ?? false,
  )
  const [resultadoTest, setResultadoTest] = useState<ResultadoTestSetDian | null>(null)
  const [confirmacion, setConfirmacion] = useState('')
  const [observaciones, setObservaciones] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [errorLocal, setErrorLocal] = useState<string | null>(null)

  const documentosPorTrack = useMemo(
    () => new Map((resultadoTest?.documentos ?? []).map((documento) => [documento.trackId, documento])),
    [resultadoTest],
  )

  const requierePt = useMemo(
    () => MODOS_OPERACION_DIAN.find((item) => item.codigo === modo)?.requierePt ?? false,
    [modo],
  )

  const guardarModo = async () => {
    if (empresaId == null) return
    setGuardando(true)
    setErrorLocal(null)
    try {
      const datos = await guardarHabilitacionDian(empresaId, {
        modo: modo as HabilitacionDian['modo'],
        resolucionNumero: resolucionNumero || '',
        resolucionFecha,
        prefijo,
        rangoDesde: typeof rangoDesde === 'number' ? rangoDesde : null,
        rangoHasta: typeof rangoHasta === 'number' ? rangoHasta : null,
        testSetId,
        softwareId,
        notificarAdquirente,
      })
      setHabilitacion(datos)
      setPaso(1)
      notificar('Modo de operación guardado.', 'success')
    } catch {
      setErrorLocal('No se pudo guardar el modo de operación.')
    } finally {
      setGuardando(false)
    }
  }

  const guardarResolucion = async () => {
    if (empresaId == null) return
    setGuardando(true)
    setErrorLocal(null)
    try {
      const datos = await guardarHabilitacionDian(empresaId, {
        modo: modo as HabilitacionDian['modo'],
        resolucionNumero,
        resolucionFecha,
        prefijo,
        rangoDesde: typeof rangoDesde === 'number' ? rangoDesde : null,
        rangoHasta: typeof rangoHasta === 'number' ? rangoHasta : null,
        testSetId,
        softwareId,
        notificarAdquirente,
      })
      setHabilitacion(datos)
      setPaso(2)
      notificar('Resolución y rango guardados.', 'success')
    } catch {
      setErrorLocal('No se pudo guardar la resolución. Revise los campos obligatorios.')
    } finally {
      setGuardando(false)
    }
  }

  const enviarTest = async () => {
    if (empresaId == null || !testSetId.trim()) return
    setGuardando(true)
    setErrorLocal(null)
    try {
      const resultado = await enviarDocumentosTestSet(
        empresaId,
        DOCUMENTOS_TEST_SET.map((documento) => ({
          trackId: documento.trackId,
          tipoDocumento: documento.tipoDocumento,
          descripcion: documento.descripcion,
          estado: 'PENDIENTE',
          mensaje: null,
        })),
      )
      setResultadoTest(resultado)
      notificar('TEST SET enviado. Consulte el resultado por trackId.', 'success')
    } catch {
      setErrorLocal('No se pudo enviar el TEST SET a la DIAN.')
    } finally {
      setGuardando(false)
    }
  }

  const consultarTest = async () => {
    if (empresaId == null || !testSetId.trim()) return
    setGuardando(true)
    setErrorLocal(null)
    try {
      const resultado = await consultarEstadoTestSet(empresaId, testSetId.trim())
      setResultadoTest(resultado)
      if (resultado.estado === 'TEST_SET_APROBADO') {
        recargar()
        setPaso(3)
      }
    } catch {
      setErrorLocal('No se pudo consultar el estado del TEST SET.')
    } finally {
      setGuardando(false)
    }
  }

  const confirmarSwitchover = async () => {
    if (empresaId == null || confirmacion !== CONFIRMACION_SWITCHOVER || !testSetAprobado) return
    setGuardando(true)
    setErrorLocal(null)
    try {
      const datos = await switchoverProduccion(empresaId, {
        confirmacion: CONFIRMACION_SWITCHOVER,
        testSetId: testSetId.trim(),
        resolucionNumero,
        observaciones: observaciones.trim() || undefined,
      })
      setHabilitacion(datos)
      notificar('Switchover a producción completado.', 'success')
    } catch {
      setErrorLocal('No se pudo completar el switchover a producción.')
    } finally {
      setGuardando(false)
    }
  }

  if (cargandoEmpresa || cargando) return <LoadingState label="Cargando habilitación DIAN…" />

  if (empresaId == null) {
    return (
      <EmptyState
        title="Sin empresa activa"
        description="Necesita una empresa asociada para configurar la habilitación DIAN."
        action={<Button component={RouterLink} to="/registro" variant="contained">Crear empresa</Button>}
      />
    )
  }

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="Empresa"
        title="Habilitación DIAN"
        subtitle="Configure el modo de operación, valide el TEST SET y active la facturación en producción."
        actions={
          <Button component={RouterLink} to="/app/facturacion/nueva" variant="outlined">
            Ir a facturación
          </Button>
        }
      />

      <Alert severity={puedeEmitir ? 'success' : 'warning'}>
        {puedeEmitir
          ? 'Habilitación aprobada: la emisión de facturas está permitida.'
          : 'La emisión de facturas está bloqueada hasta completar el switchover a producción.'}
      </Alert>
      {error && <Alert severity="error">{error}</Alert>}
      {errorLocal && <Alert severity="error">{errorLocal}</Alert>}

      <Stepper activeStep={paso} alternativeLabel>
        {PASOS.map((etiqueta) => (
          <Step key={etiqueta}>
            <StepLabel>{etiqueta}</StepLabel>
          </Step>
        ))}
      </Stepper>

      {paso === 0 && (
        <SectionCard title="Paso 1 · Modo de operación" subtitle="Seleccione quién es titular de la habilitación.">
          <RadioGroup value={modo} onChange={(evento) => setModo(evento.target.value)}>
            {MODOS_OPERACION_DIAN.map((opcion) => (
              <FormControlLabel
                key={opcion.codigo}
                value={opcion.codigo}
                control={<Radio />}
                label={
                  <Box>
                    <Typography variant="body1">{opcion.etiqueta}</Typography>
                    <Typography variant="caption" color="text.secondary">{opcion.descripcion}</Typography>
                  </Box>
                }
              />
            ))}
          </RadioGroup>
          {requierePt && (
            <Alert severity="info" sx={{ mt: 2 }}>
              Este modo requiere resolución de proveedor tecnológico vigente.
            </Alert>
          )}
          <Box sx={{ mt: 3 }}>
            <Button variant="contained" loading={guardando} onClick={() => void guardarModo()}>
              Guardar y continuar
            </Button>
          </Box>
        </SectionCard>
      )}

      {paso === 1 && (
        <SectionCard title="Paso 2 · Certificado y resolución" subtitle="Datos que respaldan la numeración autorizada.">
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                label="Número de resolución"
                value={resolucionNumero}
                onChange={(evento) => setResolucionNumero(evento.target.value)}
                fullWidth
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                label="Fecha de resolución"
                type="date"
                slotProps={{ inputLabel: { shrink: true } }}
                value={resolucionFecha}
                onChange={(evento) => setResolucionFecha(evento.target.value)}
                fullWidth
              />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField label="Prefijo" value={prefijo} onChange={(evento) => setPrefijo(evento.target.value)} fullWidth />
            </Grid>
            <Grid size={{ xs: 6, md: 4 }}>
              <TextField
                label="Rango desde"
                type="number"
                value={rangoDesde}
                onChange={(evento) => setRangoDesde(evento.target.value === '' ? '' : Number(evento.target.value))}
                fullWidth
              />
            </Grid>
            <Grid size={{ xs: 6, md: 4 }}>
              <TextField
                label="Rango hasta"
                type="number"
                value={rangoHasta}
                onChange={(evento) => setRangoHasta(evento.target.value === '' ? '' : Number(evento.target.value))}
                fullWidth
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField label="Software ID" value={softwareId} onChange={(evento) => setSoftwareId(evento.target.value)} fullWidth />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                label="Test Set ID"
                value={testSetId}
                onChange={(evento) => setTestSetId(evento.target.value)}
                fullWidth
              />
            </Grid>
            <Grid size={{ xs: 12 }}>
              <FormControlLabel
                control={
                  <Switch
                    checked={notificarAdquirente}
                    onChange={(evento) => setNotificarAdquirente(evento.target.checked)}
                  />
                }
                label="Notificar al adquirente por correo"
              />
            </Grid>
          </Grid>
          <Box sx={{ mt: 3, display: 'flex', gap: 1 }}>
            <Button onClick={() => setPaso(0)}>Atrás</Button>
            <Button
              variant="contained"
              loading={guardando}
              disabled={!resolucionNumero.trim() || !testSetId.trim()}
              onClick={() => void guardarResolucion()}
            >
              Guardar y continuar
            </Button>
          </Box>
        </SectionCard>
      )}

      {paso === 2 && (
        <SectionCard
          title="Paso 3 · TEST SET"
          subtitle="8 facturas, 1 nota débito y 1 nota crédito con estado por trackId."
        >
          <Alert severity="info" sx={{ mb: 2 }}>
            El TEST SET tiene {DOCUMENTOS_TEST_SET.length} documentos: 8 + 1 + 1.
          </Alert>
          <Box component="ul" sx={{ m: 0, pl: 2, display: 'grid', gap: 1 }}>
            {DOCUMENTOS_TEST_SET.map((documento) => {
              const estado = documentosPorTrack.get(documento.trackId)?.estado ?? 'PENDIENTE'
              return (
                <Box component="li" key={documento.trackId} sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                  <Typography variant="body2" sx={{ minWidth: 120 }}>{documento.trackId}</Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ flexGrow: 1 }}>
                    {documento.descripcion}
                  </Typography>
                  <EstadoChip estado={estado} />
                </Box>
              )
            })}
          </Box>
          <Box sx={{ mt: 3, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
            <Button onClick={() => setPaso(1)}>Atrás</Button>
            <Button variant="outlined" loading={guardando} disabled={!testSetId.trim()} onClick={() => void enviarTest()}>
              Enviar TEST SET
            </Button>
            <Button variant="contained" loading={guardando} disabled={!testSetId.trim()} onClick={() => void consultarTest()}>
              Consultar estado del TEST SET
            </Button>
          </Box>
          {resultadoTest && (
            <Alert severity={resultadoTest.estado === 'TEST_SET_APROBADO' ? 'success' : 'info'} sx={{ mt: 2 }}>
              {resultadoTest.aprobados}/{resultadoTest.total} aprobados · {resultadoTest.rechazados} rechazados
            </Alert>
          )}
          {!testSetAprobado && (
            <Alert severity="warning" sx={{ mt: 2 }}>
              No puede avanzar al switchover sin TEST_SET_APROBADO.
            </Alert>
          )}
        </SectionCard>
      )}

      {paso === 3 && (
        <SectionCard title="Paso 4 · Switchover a producción" subtitle="Confirmación explícita y registro de auditoría.">
          {!testSetAprobado && (
            <Alert severity="error">El TEST SET debe estar aprobado antes del switchover.</Alert>
          )}
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                label={`Escriba ${CONFIRMACION_SWITCHOVER} para confirmar`}
                value={confirmacion}
                onChange={(evento) => setConfirmacion(evento.target.value)}
                fullWidth
              />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                label="Observaciones (opcional)"
                value={observaciones}
                onChange={(evento) => setObservaciones(evento.target.value)}
                fullWidth
              />
            </Grid>
          </Grid>
          <Box sx={{ mt: 3, display: 'flex', gap: 1 }}>
            <Button onClick={() => setPaso(2)}>Atrás</Button>
            <Button
              variant="contained"
              color="primary"
              loading={guardando}
              disabled={!testSetAprobado || confirmacion !== CONFIRMACION_SWITCHOVER}
              onClick={() => void confirmarSwitchover()}
            >
              Confirmar switchover a producción
            </Button>
          </Box>
          <Box sx={{ mt: 3 }}>
            <Typography variant="h6" component="h3" gutterBottom>
              Auditoría visible
            </Typography>
            {(habilitacion?.auditoria ?? []).length === 0 ? (
              <EmptyState title="Sin eventos de auditoría" description="Las acciones de esta configuración aparecerán aquí." />
            ) : (
              <Box component="ul" sx={{ m: 0, pl: 2, display: 'grid', gap: 1 }}>
                {habilitacion?.auditoria.map((evento) => (
                  <Box component="li" key={evento.id}>
                    <Typography variant="body2">
                      {evento.createdAt} · {evento.accion} · {evento.usuario ?? 'sistema'}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">{evento.detalle}</Typography>
                  </Box>
                ))}
              </Box>
            )}
          </Box>
        </SectionCard>
      )}
    </Box>
  )
}
