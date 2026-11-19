import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import FormControlLabel from '@mui/material/FormControlLabel'
import Grid from '@mui/material/Grid2'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import Switch from '@mui/material/Switch'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import SaveIcon from '@mui/icons-material/Save'
import { api, type ConfigFe } from '../api/client'
import { useEmpresaActiva } from '../features/auth/useEmpresaActiva'
import LoadingState from '../components/ui/LoadingState'
import SectionCard from '../components/ui/SectionCard'

const VACIO: ConfigFe = {
  ambiente: 2,
  softwareId: '',
  softwarePin: '',
  softwareSecurityCode: null,
  testSetId: '',
  resolucionNumero: '',
  resolucionFecha: null,
  prefijo: '',
  rangoDesde: null,
  rangoHasta: null,
  rangoVigenciaDesde: null,
  rangoVigenciaHasta: null,
  canal: 'SIMULADO',
  pacProveedor: '',
  pacBaseUrl: '',
  pacApiKey: '',
  certificadoNombre: '',
  certificadoPassword: '',
  certificadoPath: '',
  responsabilidadFiscal: 'R-99-PN',
  codigoMunicipio: '',
  activo: true,
  tieneCertificado: false,
  tienePacKey: false,
  notificarAdquirente: false,
}

export default function ConfigFePage() {
  const navigate = useNavigate()
  const { empresaId, cargando: cargandoEmpresa } = useEmpresaActiva()
  const [config, setConfig] = useState<ConfigFe>(VACIO)
  const [mensaje, setMensaje] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)
  const [subiendoCert, setSubiendoCert] = useState(false)
  const [cargando, setCargando] = useState(true)
  const archivoRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (cargandoEmpresa) return
    if (empresaId == null) {
      setCargando(false)
      return
    }
    let activo = true
    setCargando(true)
    api
      .get<ConfigFe>(`/facturacion/config/${empresaId}`)
      .then((res) => {
        if (activo) {
          setConfig({ ...VACIO, ...res.data, softwarePin: '', pacApiKey: '', certificadoPassword: '' })
        }
      })
      .catch(() => {
        if (activo) setError('No se pudo cargar la configuración.')
      })
      .finally(() => {
        if (activo) setCargando(false)
      })
    return () => {
      activo = false
    }
  }, [empresaId, cargandoEmpresa])

  const set = <K extends keyof ConfigFe>(clave: K, valor: ConfigFe[K]) =>
    setConfig((c) => ({ ...c, [clave]: valor }))

  const onGuardar = async () => {
    if (empresaId == null) return
    setGuardando(true)
    setError(null)
    setMensaje(null)
    try {
      const { data } = await api.put<ConfigFe>(`/facturacion/config/${empresaId}`, config)
      setConfig({ ...VACIO, ...data, softwarePin: '', pacApiKey: '', certificadoPassword: '' })
      setMensaje('Configuración guardada.')
    } catch {
      setError('No se pudo guardar la configuración.')
    } finally {
      setGuardando(false)
    }
  }

  const subirCertificado = async (file: File) => {
    if (empresaId == null) return
    setSubiendoCert(true)
    setError(null)
    setMensaje(null)
    try {
      const fd = new FormData()
      fd.append('archivo', file)
      fd.append('password', config.certificadoPassword ?? '')
      const { data } = await api.post<ConfigFe>(`/facturacion/config/${empresaId}/certificado`, fd)
      setConfig({ ...VACIO, ...data, softwarePin: '', pacApiKey: '', certificadoPassword: '' })
      setMensaje('Certificado cargado.')
    } catch {
      setError('No se pudo subir el certificado.')
    } finally {
      setSubiendoCert(false)
    }
  }

  if (cargandoEmpresa || cargando) return <LoadingState label="Cargando configuración…" minHeight="40vh" />
  if (empresaId == null) return <Alert severity="info">No hay una empresa asociada.</Alert>

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <Stack
        direction={{ xs: 'column', md: 'row' }}
        spacing={2}
        sx={{ alignItems: { md: 'center' }, justifyContent: 'space-between' }}
      >
        <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
          <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/facturacion')}>
            Volver
          </Button>
          <Typography variant="h5" component="h1">
            Configuración DIAN
          </Typography>
        </Stack>
        <Stack direction="row" spacing={1} useFlexGap sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
          {config.tieneCertificado && <Chip color="success" label="Certificado cargado" />}
          {config.tienePacKey && <Chip color="success" label="API key PAC" />}
          <Button
            variant="contained"
            startIcon={<SaveIcon />}
            onClick={onGuardar}
            disabled={guardando || subiendoCert}
            loading={guardando}
          >
            Guardar
          </Button>
        </Stack>
      </Stack>

      {mensaje && <Alert severity="success">{mensaje}</Alert>}
      {error && <Alert severity="error">{error}</Alert>}

      <Box
        aria-busy={guardando}
        sx={{
          display: 'grid',
          gap: 3,
          pointerEvents: guardando ? 'none' : undefined,
          opacity: guardando ? 0.6 : 1,
        }}
      >
        <SectionCard title="General">
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField select fullWidth label="Ambiente" value={config.ambiente} onChange={(e) => set('ambiente', Number(e.target.value))}>
              <MenuItem value={2}>Habilitación (2)</MenuItem>
              <MenuItem value={1}>Producción (1)</MenuItem>
            </TextField>
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField select fullWidth label="Canal de transmisión" value={config.canal} onChange={(e) => set('canal', e.target.value)}>
              <MenuItem value="SIMULADO">Simulado</MenuItem>
              <MenuItem value="PAC">PAC</MenuItem>
              <MenuItem value="DIAN">DIAN directo</MenuItem>
            </TextField>
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField fullWidth label="Prefijo" value={config.prefijo ?? ''} onChange={(e) => set('prefijo', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField fullWidth label="Responsabilidad fiscal" value={config.responsabilidadFiscal} onChange={(e) => set('responsabilidadFiscal', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField fullWidth label="Software ID" value={config.softwareId ?? ''} onChange={(e) => set('softwareId', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField fullWidth type="password" label="Software PIN" placeholder={config.softwarePin ? '' : '••••••'} value={config.softwarePin ?? ''} onChange={(e) => set('softwarePin', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField fullWidth label="Test Set ID" value={config.testSetId ?? ''} onChange={(e) => set('testSetId', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField fullWidth label="Código municipio (DANE)" value={config.codigoMunicipio ?? ''} onChange={(e) => set('codigoMunicipio', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <FormControlLabel
              control={
                <Switch
                  checked={config.notificarAdquirente}
                  onChange={(e) => set('notificarAdquirente', e.target.checked)}
                />
              }
              label="Enviar factura al adquirente por correo"
            />
          </Grid>
        </Grid>
      </SectionCard>

      <SectionCard title="Resolución de facturación">
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField fullWidth label="Número de resolución" value={config.resolucionNumero ?? ''} onChange={(e) => set('resolucionNumero', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 2 }}>
            <TextField fullWidth type="date" InputLabelProps={{ shrink: true }} label="Fecha" value={config.resolucionFecha ?? ''} onChange={(e) => set('resolucionFecha', e.target.value || null)} />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField fullWidth type="number" label="Rango desde" value={config.rangoDesde ?? ''} onChange={(e) => set('rangoDesde', e.target.value === '' ? null : Number(e.target.value))} />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField fullWidth type="number" label="Rango hasta" value={config.rangoHasta ?? ''} onChange={(e) => set('rangoHasta', e.target.value === '' ? null : Number(e.target.value))} />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField fullWidth type="date" InputLabelProps={{ shrink: true }} label="Vigencia desde" value={config.rangoVigenciaDesde ?? ''} onChange={(e) => set('rangoVigenciaDesde', e.target.value || null)} />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField fullWidth type="date" InputLabelProps={{ shrink: true }} label="Vigencia hasta" value={config.rangoVigenciaHasta ?? ''} onChange={(e) => set('rangoVigenciaHasta', e.target.value || null)} />
          </Grid>
        </Grid>
      </SectionCard>

      <SectionCard title="PAC / Certificado">
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField fullWidth label="Proveedor PAC" value={config.pacProveedor ?? ''} onChange={(e) => set('pacProveedor', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 5 }}>
            <TextField fullWidth label="URL base PAC" value={config.pacBaseUrl ?? ''} onChange={(e) => set('pacBaseUrl', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField fullWidth type="password" label="API key PAC" placeholder={config.pacApiKey ? '' : '••••••'} value={config.pacApiKey ?? ''} onChange={(e) => set('pacApiKey', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField fullWidth label="Nombre del certificado" value={config.certificadoNombre ?? ''} onChange={(e) => set('certificadoNombre', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField fullWidth label="Ruta del certificado (.p12)" value={config.certificadoPath ?? ''} onChange={(e) => set('certificadoPath', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField fullWidth type="password" label="Contraseña del certificado" placeholder={config.certificadoPassword ? '' : '••••••'} value={config.certificadoPassword ?? ''} onChange={(e) => set('certificadoPassword', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
              <Button
                variant="outlined"
                disabled={subiendoCert || guardando}
                loading={subiendoCert}
                onClick={() => archivoRef.current?.click()}
              >
                Subir certificado (.p12)
              </Button>
              <Typography variant="body2" color="text.secondary">
                {config.tieneCertificado
                  ? `Certificado cargado: ${config.certificadoNombre ?? 'sí'}`
                  : 'Sin certificado cargado'}
              </Typography>
              <input
                ref={archivoRef}
                type="file"
                accept=".p12,.pfx"
                hidden
                onChange={(e) => {
                  const f = e.target.files?.[0]
                  if (f) subirCertificado(f)
                  e.target.value = ''
                }}
              />
            </Stack>
          </Grid>
          </Grid>
        </SectionCard>
      </Box>
    </Box>
  )
}
