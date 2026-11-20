import { useMemo, useState } from 'react'
import Accordion from '@mui/material/Accordion'
import AccordionDetails from '@mui/material/AccordionDetails'
import AccordionSummary from '@mui/material/AccordionSummary'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Divider from '@mui/material/Divider'
import FormControl from '@mui/material/FormControl'
import FormControlLabel from '@mui/material/FormControlLabel'
import Grid from '@mui/material/Grid2'
import InputLabel from '@mui/material/InputLabel'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import MenuItem from '@mui/material/MenuItem'
import Select from '@mui/material/Select'
import Stack from '@mui/material/Stack'
import Switch from '@mui/material/Switch'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import EmptyState from '../ui/EmptyState'
import EstadoChip from '../ui/EstadoChip'
import SectionCard from '../ui/SectionCard'
import {
  PROVEEDORES_INTEGRACION,
  type CapacidadIntegracion,
  type EstadoIntegracion,
  type IntegracionConfig,
  type IntegracionConfigRequest,
  type IntegracionEvento,
  type ProveedorIntegracion,
  type TipoSincronizacionIntegracion,
} from '../../api/client'

type CampoIntegracion = {
  clave: string
  etiqueta: string
  secreto?: boolean
  tipo?: 'text' | 'url'
}

const CAMPOS: Record<ProveedorIntegracion, CampoIntegracion[]> = {
  SIIGO: [
    { clave: 'username', etiqueta: 'Usuario' },
    { clave: 'accessKey', etiqueta: 'Access key', secreto: true },
    { clave: 'url', etiqueta: 'URL base (opcional)', tipo: 'url' },
  ],
  ALEGRA: [
    { clave: 'email', etiqueta: 'Correo' },
    { clave: 'apiToken', etiqueta: 'Token API', secreto: true },
  ],
  ODOO: [
    { clave: 'url', etiqueta: 'URL de Odoo', tipo: 'url' },
    { clave: 'db', etiqueta: 'Base de datos' },
    { clave: 'username', etiqueta: 'Usuario' },
    { clave: 'password', etiqueta: 'Contraseña', secreto: true },
    { clave: 'webhookSecret', etiqueta: 'Secreto de webhook', secreto: true },
  ],
  WORLD_OFFICE: [
    { clave: 'apiToken', etiqueta: 'Token API', secreto: true },
    { clave: 'username', etiqueta: 'Usuario (alternativo)' },
    { clave: 'password', etiqueta: 'Contraseña (alternativa)', secreto: true },
    { clave: 'webhookSecret', etiqueta: 'Secreto de webhook', secreto: true },
  ],
  HELISA: [
    { clave: 'usuario', etiqueta: 'Usuario' },
    { clave: 'clave', etiqueta: 'Clave', secreto: true },
    { clave: 'url', etiqueta: 'URL del servicio', tipo: 'url' },
    { clave: 'sign', etiqueta: 'Firma', secreto: true },
    { clave: 'empresa', etiqueta: 'Empresa' },
    { clave: 'webhookSecret', etiqueta: 'Secreto de webhook', secreto: true },
  ],
  SHOPIFY: [
    { clave: 'shop', etiqueta: 'Dominio de la tienda' },
    { clave: 'accessToken', etiqueta: 'Token de acceso', secreto: true },
    { clave: 'webhookSecret', etiqueta: 'Secreto de webhook', secreto: true },
  ],
  WOOCOMMERCE: [
    { clave: 'url', etiqueta: 'URL de la tienda', tipo: 'url' },
    { clave: 'consumerKey', etiqueta: 'Consumer key', secreto: true },
    { clave: 'consumerSecret', etiqueta: 'Consumer secret', secreto: true },
    { clave: 'webhookSecret', etiqueta: 'Secreto de webhook', secreto: true },
  ],
  SAP_BUSINESS_ONE: [
    { clave: 'url', etiqueta: 'URL del servicio', tipo: 'url' },
    { clave: 'companyDB', etiqueta: 'Company DB' },
    { clave: 'username', etiqueta: 'Usuario' },
    { clave: 'password', etiqueta: 'Contraseña', secreto: true },
  ],
  DYNAMICS_365: [
    { clave: 'url', etiqueta: 'URL del servicio', tipo: 'url' },
    { clave: 'tenantId', etiqueta: 'Tenant ID' },
    { clave: 'clientId', etiqueta: 'Client ID' },
    { clave: 'clientSecret', etiqueta: 'Client secret', secreto: true },
  ],
  NETSUITE: [
    { clave: 'accountId', etiqueta: 'Account ID' },
    { clave: 'consumerKey', etiqueta: 'Consumer key', secreto: true },
    { clave: 'consumerSecret', etiqueta: 'Consumer secret', secreto: true },
    { clave: 'tokenId', etiqueta: 'Token ID' },
    { clave: 'tokenSecret', etiqueta: 'Token secret', secreto: true },
  ],
}

const CAPACIDADES: Record<CapacidadIntegracion, string> = {
  SYNC_CLIENTES: 'Clientes',
  SYNC_PRODUCTOS: 'Productos',
  EMITIR_FACTURA: 'Emitir factura',
  CONSULTAR_ESTADO: 'Consultar estado',
  REGISTRAR_PAGO: 'Registrar pago',
  RECIBIR_WEBHOOK: 'Recibir webhook',
}

const TONO_ESTADO: Record<EstadoIntegracion, 'success' | 'default' | 'error' | 'info'> = {
  CONECTADO: 'success',
  DESCONFIGURADO: 'default',
  ERROR: 'error',
  SINCRONIZANDO: 'info',
}

const TIPOS_SINCRONIZACION: TipoSincronizacionIntegracion[] = ['CLIENTE', 'PRODUCTO', 'FACTURA']

function formatearFecha(valor: string | null) {
  if (!valor) return 'Sin registro'
  const fecha = new Date(valor)
  if (Number.isNaN(fecha.getTime())) return 'Sin registro'
  return new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' }).format(fecha)
}

function credencialesDe(proveedor: ProveedorIntegracion, valores: Record<string, string> | undefined) {
  return CAMPOS[proveedor].reduce<Record<string, string>>((acc, campo) => {
    const valor = valores?.[campo.clave]?.trim()
    if (valor) acc[campo.clave] = valor
    return acc
  }, {})
}

type IntegracionesStatusPanelProps = {
  configuraciones: IntegracionConfig[]
  eventos: IntegracionEvento[]
  webhook: { proveedor: ProveedorIntegracion; token: string } | null
  accion: string | null
  sincronizando: ProveedorIntegracion | null
  onGuardar: (proveedor: ProveedorIntegracion, solicitud: IntegracionConfigRequest) => void
  onProbar: (proveedor: ProveedorIntegracion) => void
  onSincronizar: (proveedor: ProveedorIntegracion, tipo: TipoSincronizacionIntegracion) => void
  onReintentar: (eventoId: number) => void
}

export default function IntegracionesStatusPanel({
  configuraciones,
  eventos,
  webhook,
  accion,
  sincronizando,
  onGuardar,
  onProbar,
  onSincronizar,
  onReintentar,
}: IntegracionesStatusPanelProps) {
  const [valores, setValores] = useState<Record<string, Record<string, string>>>({})
  const [activos, setActivos] = useState<Record<string, boolean>>({})
  const [tipos, setTipos] = useState<Record<string, TipoSincronizacionIntegracion>>({})

  const porProveedor = useMemo(
    () => new Map(configuraciones.map((config) => [config.proveedor, config])),
    [configuraciones],
  )

  const configuracionesVisibles = useMemo(
    () =>
      PROVEEDORES_INTEGRACION.map((proveedor) => {
        const base = porProveedor.get(proveedor) ?? {
          id: null,
          proveedor,
          slug: proveedor.toLowerCase().replace(/_/g, '-'),
          activo: false,
          estado: 'DESCONFIGURADO' as EstadoIntegracion,
          configurada: false,
          capacidades: [],
          ultimoError: null,
          ultimaSincronizacion: null,
          webhookToken: null,
        }
        return sincronizando === proveedor ? { ...base, estado: 'SINCRONIZANDO' as EstadoIntegracion } : base
      }),
    [porProveedor, sincronizando],
  )

  const cambiarCampo = (proveedor: string, clave: string, valor: string) => {
    setValores((previos) => ({
      ...previos,
      [proveedor]: { ...(previos[proveedor] ?? {}), [clave]: valor },
    }))
  }

  return (
    <Stack spacing={2}>
      {webhook && (
        <Alert severity="success" aria-live="polite">
          <Stack spacing={0.5}>
            <Typography variant="subtitle2">
              Token de webhook generado para {webhook.proveedor}. Cópielo ahora: no se mostrará de nuevo.
            </Typography>
            <Typography component="code" sx={{ fontFamily: 'monospace', wordBreak: 'break-all' }}>
              {webhook.token}
            </Typography>
          </Stack>
        </Alert>
      )}

      <SectionCard
        title="Proveedores conectables"
        subtitle="Configure, pruebe y sincronice cada sistema externo de esta empresa."
        padded={false}
      >
        <Box sx={{ px: { xs: 1, md: 2 }, py: 1 }}>
          {configuracionesVisibles.map((config) => {
            const proveedor = config.proveedor
            const activo = activos[proveedor] ?? config.activo
            const tipo = tipos[proveedor] ?? 'CLIENTE'
            const cargandoGuardar = accion === `guardar:${proveedor}`
            const cargandoPrueba = accion === `probar:${proveedor}`
            const cargandoSync = accion === `sincronizar:${proveedor}`

            return (
              <Accordion key={proveedor} disableGutters elevation={0} sx={{ border: '1px solid', borderColor: 'divider', mb: 1.5 }}>
                <AccordionSummary expandIcon={<ExpandMoreIcon />} aria-controls={`panel-${proveedor}`}>
                  <Stack direction="row" spacing={1.5} alignItems="center" flexWrap="wrap" useFlexGap sx={{ width: '100%' }}>
                    <Box sx={{ minWidth: 0, flexGrow: 1 }}>
                      <Typography variant="h6" component="h3">
                        {proveedor}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        /{config.slug} · Última sincronización: {formatearFecha(config.ultimaSincronizacion)}
                      </Typography>
                    </Box>
                    <Chip
                      size="small"
                      color={TONO_ESTADO[config.estado] ?? 'default'}
                      label={config.estado}
                      aria-label={`Estado de ${proveedor}: ${config.estado}`}
                    />
                    <Chip
                      size="small"
                      variant="outlined"
                      label={config.configurada ? 'Configurada' : 'Sin configurar'}
                    />
                  </Stack>
                </AccordionSummary>
                <AccordionDetails>
                  <Stack spacing={2}>
                    {config.ultimoError && (
                      <Alert severity="error">
                        Último error: {config.ultimoError}
                      </Alert>
                    )}

                    <Box>
                      <Typography variant="subtitle2" sx={{ mb: 1 }}>
                        Capacidades
                      </Typography>
                      <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap>
                        {config.capacidades.length === 0 ? (
                          <Typography variant="body2" color="text.secondary">
                            Sin capacidades registradas
                          </Typography>
                        ) : (
                          config.capacidades.map((capacidad) => (
                            <Chip key={capacidad} size="small" variant="outlined" label={CAPACIDADES[capacidad] ?? capacidad} />
                          ))
                        )}
                      </Stack>
                    </Box>

                    <Divider />

                    <Grid container spacing={2}>
                      {CAMPOS[proveedor].map((campo) => (
                        <Grid key={campo.clave} size={{ xs: 12, md: 6 }}>
                          <TextField
                            label={campo.etiqueta}
                            type={campo.secreto ? 'password' : campo.tipo ?? 'text'}
                            value={valores[proveedor]?.[campo.clave] ?? ''}
                            onChange={(e) => cambiarCampo(proveedor, campo.clave, e.target.value)}
                            autoComplete="off"
                            fullWidth
                            slotProps={{ htmlInput: { 'aria-label': `${campo.etiqueta} de ${proveedor}` } }}
                          />
                        </Grid>
                      ))}
                    </Grid>

                    <FormControlLabel
                      control={
                        <Switch
                          checked={activo}
                          onChange={(e) => setActivos((previos) => ({ ...previos, [proveedor]: e.target.checked }))}
                          inputProps={{ 'aria-label': `Activar integración ${proveedor}` }}
                        />
                      }
                      label="Integración activa"
                    />

                    <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                      <Button
                        variant="contained"
                        loading={cargandoGuardar}
                        onClick={() =>
                          onGuardar(proveedor, {
                            activo,
                            credenciales: credencialesDe(proveedor, valores[proveedor]),
                            configuracion: {},
                          })
                        }
                      >
                        Guardar configuración
                      </Button>
                      <Button
                        variant="outlined"
                        loading={cargandoPrueba}
                        disabled={!config.configurada}
                        onClick={() => onProbar(proveedor)}
                      >
                        Probar conexión
                      </Button>
                    </Stack>

                    <Divider />

                    <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap alignItems="center">
                      <FormControl size="small" sx={{ minWidth: 160 }}>
                        <InputLabel id={`sync-${proveedor}-label`}>Tipo de sincronización</InputLabel>
                        <Select
                          labelId={`sync-${proveedor}-label`}
                          label="Tipo de sincronización"
                          value={tipo}
                          onChange={(e) => setTipos((previos) => ({ ...previos, [proveedor]: e.target.value as TipoSincronizacionIntegracion }))}
                        >
                          {TIPOS_SINCRONIZACION.map((opcion) => (
                            <MenuItem key={opcion} value={opcion}>
                              {opcion}
                            </MenuItem>
                          ))}
                        </Select>
                      </FormControl>
                      <Button
                        variant="outlined"
                        loading={cargandoSync}
                        disabled={!config.configurada}
                        onClick={() => onSincronizar(proveedor, tipo)}
                      >
                        Sincronizar
                      </Button>
                    </Stack>
                  </Stack>
                </AccordionDetails>
              </Accordion>
            )
          })}
        </Box>
      </SectionCard>

      <SectionCard title="Eventos de integración" subtitle="Últimos eventos registrados y sus reintentos." padded={false}>
        <Box sx={{ px: { xs: 2, md: 3 }, py: 2 }}>
          {eventos.length === 0 ? (
            <EmptyState
              title="Sin eventos"
              description="Aún no hay sincronizaciones o webhooks registrados para esta empresa."
            />
          ) : (
            <List disablePadding>
              {eventos.map((evento) => (
                <ListItem
                  key={evento.id}
                  divider
                  secondaryAction={
                    <Button
                      size="small"
                      variant="text"
                      loading={accion === `reintentar:${evento.id}`}
                      disabled={evento.estado === 'EXITOSO' || evento.intentos >= 5}
                      aria-label={`Reintentar evento ${evento.id}`}
                      onClick={() => onReintentar(evento.id)}
                    >
                      Reintentar
                    </Button>
                  }
                >
                  <ListItemText
                    primary={
                      <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                        <Typography variant="subtitle2" component="span">
                          {evento.proveedor}
                        </Typography>
                        <EstadoChip estado={evento.estado} />
                      </Stack>
                    }
                    secondary={
                      <Box component="span">
                        <Typography component="span" variant="body2" color="text.secondary">
                          {evento.tipo} · intentos: {evento.intentos} · próximo: {formatearFecha(evento.proximoIntento)} ·{' '}
                          {formatearFecha(evento.creadoEn)}
                        </Typography>
                        {evento.ultimoError && (
                          <Typography component="span" variant="body2" color="error.main" sx={{ display: 'block' }}>
                            {evento.ultimoError}
                          </Typography>
                        )}
                      </Box>
                    }
                  />
                </ListItem>
              ))}
            </List>
          )}
        </Box>
      </SectionCard>
    </Stack>
  )
}
