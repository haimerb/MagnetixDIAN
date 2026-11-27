import { useCallback, useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Checkbox from '@mui/material/Checkbox'
import Chip from '@mui/material/Chip'
import Divider from '@mui/material/Divider'
import FormControl from '@mui/material/FormControl'
import FormControlLabel from '@mui/material/FormControlLabel'
import Grid from '@mui/material/Grid2'
import InputLabel from '@mui/material/InputLabel'
import LinearProgress from '@mui/material/LinearProgress'
import MenuItem from '@mui/material/MenuItem'
import Select from '@mui/material/Select'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import PageHeader from '../components/ui/PageHeader'
import SectionCard from '../components/ui/SectionCard'
import EstadoChip from '../components/ui/EstadoChip'
import LoadingState from '../components/ui/LoadingState'
import ApiKeysPanel from '../components/suscripcion/ApiKeysPanel'
import PagosPanel from '../components/suscripcion/PagosPanel'
import { useEmpresaActiva } from '../features/auth/useEmpresaActiva'
import {
  cancelarSuscripcion,
  iniciarCheckout,
  listarPlanes,
  obtenerSuscripcion,
  type MetodoPago,
  type Plan,
  type Suscripcion,
} from '../api/client'
import { METODOS_CHECKOUT, requiereDatosWompi } from '../api/erp/pagos'

const PASARELAS = [
  { valor: 'SIMULADO', etiqueta: 'Simulada (pruebas)' },
  { valor: 'WOMPI', etiqueta: 'Wompi' },
  { valor: 'MERCADOPAGO', etiqueta: 'MercadoPago' },
]

function formatearPrecio(plan: Plan) {
  if (plan.precioMensual <= 0) return 'Gratis'
  return new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: plan.moneda || 'COP',
    maximumFractionDigits: 0,
  }).format(plan.precioMensual)
}

function formatearLimite(valor: number) {
  return valor <= 0 ? 'Ilimitado' : new Intl.NumberFormat('es-CO').format(valor)
}

function BarraUso({ usado, limite, etiqueta }: { usado: number; limite: number; etiqueta: string }) {
  if (limite <= 0) {
    return (
      <Typography variant="body2" color="text.secondary">
        {etiqueta}: {new Intl.NumberFormat('es-CO').format(usado)} · ilimitado
      </Typography>
    )
  }
  const pct = Math.min(100, Math.round((usado / limite) * 100))
  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" sx={{ mb: 0.5 }}>
        <Typography variant="body2" color="text.secondary">
          {etiqueta}
        </Typography>
        <Typography variant="body2" color={pct >= 100 ? 'error.main' : 'text.secondary'}>
          {new Intl.NumberFormat('es-CO').format(usado)} / {new Intl.NumberFormat('es-CO').format(limite)}
        </Typography>
      </Stack>
      <LinearProgress
        variant="determinate"
        value={pct}
        color={pct >= 100 ? 'error' : pct >= 80 ? 'warning' : 'primary'}
        sx={{ borderRadius: 1, height: 8 }}
      />
    </Box>
  )
}

export default function SuscripcionPage({ ocultarEncabezado = false }: { ocultarEncabezado?: boolean }) {
  const { empresaId, cargando: cargandoEmpresa } = useEmpresaActiva()
  const [planes, setPlanes] = useState<Plan[]>([])
  const [suscripcion, setSuscripcion] = useState<Suscripcion | null>(null)
  const [cargando, setCargando] = useState(true)
  const [procesando, setProcesando] = useState<string | null>(null)
  const [proveedor, setProveedor] = useState('SIMULADO')
  const [metodo, setMetodo] = useState<MetodoPago>('CARD')
  const [aceptaTerminos, setAceptaTerminos] = useState(false)
  const [celular, setCelular] = useState('')
  const [tipoDocumento, setTipoDocumento] = useState('CC')
  const [numeroDocumento, setNumeroDocumento] = useState('')
  const [refrescoPagos, setRefrescoPagos] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [ok, setOk] = useState<string | null>(null)

  const cargar = useCallback(async () => {
    if (empresaId == null) {
      setCargando(false)
      return
    }
    setCargando(true)
    setError(null)
    try {
      const [planesData, suscripcionData] = await Promise.all([
        listarPlanes(),
        obtenerSuscripcion(empresaId),
      ])
      setPlanes(planesData)
      setSuscripcion(suscripcionData)
    } catch {
      setError('No se pudo cargar la información de planes y suscripción.')
    } finally {
      setCargando(false)
    }
  }, [empresaId])

  useEffect(() => {
    if (!cargandoEmpresa) void cargar()
  }, [cargandoEmpresa, cargar])

  const onCambiar = async (codigo: string) => {
    if (empresaId == null) return
    if (requiereDatosWompi(metodo) && !aceptaTerminos) {
      setError('Debe aceptar los términos y el tratamiento de datos personales de Wompi.')
      return
    }
    if (metodo === 'DAVIPLATA' && (!tipoDocumento.trim() || !numeroDocumento.trim())) {
      setError('Debe indicar el tipo y número de documento para Daviplata.')
      return
    }
    setProcesando(codigo)
    setError(null)
    setOk(null)
    try {
      const pago = await iniciarCheckout(empresaId, codigo, proveedor, metodo, {
        aceptaTerminos,
        ...(celular.trim() ? { celular: celular.trim() } : {}),
        ...(metodo === 'DAVIPLATA' ? {
          documentType: tipoDocumento.trim(),
          documentNumber: numeroDocumento.trim(),
        } : {}),
      })
      if (pago.url) {
        window.location.href = pago.url
        return
      }
      await cargar()
      setRefrescoPagos((n) => n + 1)
      setOk(`Pago aprobado. Plan ${codigo} activado.`)
    } catch (e: unknown) {
      const detalle = (e as { response?: { data?: { message?: string } } }).response?.data?.message
      setError(detalle ?? 'No se pudo iniciar el pago.')
    } finally {
      setProcesando(null)
    }
  }

  const onCancelar = async () => {
    if (empresaId == null) return
    setProcesando('cancelar')
    setError(null)
    setOk(null)
    try {
      await cancelarSuscripcion(empresaId)
      await cargar()
      setOk('Suscripción cancelada.')
    } catch (e: unknown) {
      const detalle = (e as { response?: { data?: { message?: string } } }).response?.data?.message
      setError(detalle ?? 'No se pudo cancelar la suscripción.')
    } finally {
      setProcesando(null)
    }
  }

  if (cargandoEmpresa || cargando) {
    return <LoadingState label="Cargando planes…" />
  }

  if (empresaId == null) {
    return (
      <Box component="section" sx={{ display: 'grid', gap: 3 }}>
        {!ocultarEncabezado && <PageHeader overline="Empresa" title="Suscripción" />}
        <Alert severity="info">
          No hay una empresa asociada a su usuario. Contacte al administrador.
        </Alert>
      </Box>
    )
  }

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      {!ocultarEncabezado && (
        <PageHeader
          overline="Empresa"
          title="Suscripción y planes"
          subtitle="Consulte su plan actual, el consumo del mes y cambie de plan cuando lo necesite."
        />
      )}

      {error && <Alert severity="error">{error}</Alert>}
      {ok && <Alert severity="success">{ok}</Alert>}

      {suscripcion ? (
        <SectionCard
          title={`Plan ${suscripcion.plan.nombre}`}
          subtitle={`${formatearPrecio(suscripcion.plan)} · facturado mensualmente`}
          actions={<EstadoChip estado={suscripcion.estado} />}
        >
          <Stack spacing={2.5}>
            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
              <Chip label={`Proveedor: ${suscripcion.proveedor}`} size="small" variant="outlined" />
              <Chip
                label={`Inicio: ${suscripcion.periodoInicio}`}
                size="small"
                variant="outlined"
              />
              {suscripcion.periodoFin && (
                <Chip label={`Fin: ${suscripcion.periodoFin}`} size="small" variant="outlined" />
              )}
            </Stack>
            <Divider />
            <BarraUso
              usado={suscripcion.uso.facturasMes}
              limite={suscripcion.plan.limiteFacturasMes}
              etiqueta="Documentos este mes"
            />
            <BarraUso
              usado={suscripcion.uso.usuarios}
              limite={suscripcion.plan.limiteUsuarios}
              etiqueta="Usuarios"
            />
            <Box>
              <Button
                color="error"
                variant="outlined"
                onClick={onCancelar}
                loading={procesando === 'cancelar'}
              >
                Cancelar suscripción
              </Button>
            </Box>
          </Stack>
        </SectionCard>
      ) : (
        <Alert severity="info">
          No tiene una suscripción activa. Elija un plan para habilitar los límites de uso.
        </Alert>
      )}

      <SectionCard
        title="Planes disponibles"
        padded={false}
        actions={
          <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
            <FormControl size="small" sx={{ minWidth: 180 }}>
              <InputLabel id="pasarela-label">Pasarela de pago</InputLabel>
              <Select
                labelId="pasarela-label"
                label="Pasarela de pago"
                value={proveedor}
                onChange={(e) => {
                  const siguiente = e.target.value
                  setProveedor(siguiente)
                  if (siguiente !== 'WOMPI' && requiereDatosWompi(metodo)) setMetodo('CARD')
                }}
              >
                {PASARELAS.map((p) => (
                  <MenuItem key={p.valor} value={p.valor}>
                    {p.etiqueta}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl size="small" sx={{ minWidth: 200 }}>
              <InputLabel id="metodo-label">Método de pago</InputLabel>
              <Select
                labelId="metodo-label"
                label="Método de pago"
                value={metodo}
                onChange={(e) => setMetodo(e.target.value as MetodoPago)}
              >
                {METODOS_CHECKOUT.map((opcion) => (
                  <MenuItem
                    key={opcion.value}
                    value={opcion.value}
                    disabled={proveedor !== 'WOMPI' && requiereDatosWompi(opcion.value)}
                  >
                    {opcion.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Stack>
        }
      >
        {requiereDatosWompi(metodo) && (
          <Box sx={{ px: { xs: 2, md: 3 }, pt: { xs: 2, md: 3 } }}>
            <Stack spacing={2}>
              {metodo === 'DAVIPLATA' && (
                <>
                  <TextField
                    select
                    size="small"
                    label="Tipo de documento"
                    value={tipoDocumento}
                    onChange={(e) => setTipoDocumento(e.target.value)}
                    sx={{ maxWidth: 260 }}
                  >
                    <MenuItem value="CC">Cédula de ciudadanía</MenuItem>
                    <MenuItem value="CE">Cédula de extranjería</MenuItem>
                    <MenuItem value="TI">Tarjeta de identidad</MenuItem>
                  </TextField>
                  <TextField
                    size="small"
                    label="Número de documento"
                    value={numeroDocumento}
                    onChange={(e) => setNumeroDocumento(e.target.value)}
                    sx={{ maxWidth: 260 }}
                  />
                </>
              )}
              <TextField
                size="small"
                label="Celular"
                value={celular}
                onChange={(e) => setCelular(e.target.value)}
                placeholder="3107654321"
                sx={{ maxWidth: 260 }}
              />
              <FormControlLabel
                control={
                  <Checkbox
                    checked={aceptaTerminos}
                    onChange={(e) => setAceptaTerminos(e.target.checked)}
                  />
                }
                label="Acepto las políticas de privacidad y el tratamiento de datos personales de Wompi"
              />
            </Stack>
          </Box>
        )}
        <Grid container spacing={2} sx={{ p: { xs: 2, md: 3 } }}>
          {planes.map((plan) => {
            const actual = suscripcion?.plan.codigo === plan.codigo
            return (
              <Grid key={plan.id} size={{ xs: 12, md: 4 }}>
                <Box
                  sx={{
                    border: '1px solid',
                    borderColor: actual ? 'primary.main' : 'divider',
                    borderRadius: 2,
                    p: 2.5,
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 1.5,
                    bgcolor: actual ? 'action.selected' : 'transparent',
                  }}
                >
                  <Stack direction="row" alignItems="center" justifyContent="space-between">
                    <Typography variant="h6" component="h3">
                      {plan.nombre}
                    </Typography>
                    {actual && (
                      <Chip
                        icon={<CheckCircleOutlineIcon />}
                        label="Actual"
                        color="primary"
                        size="small"
                      />
                    )}
                  </Stack>
                  <Typography variant="h5">{formatearPrecio(plan)}</Typography>
                  {plan.descripcion && (
                    <Typography variant="body2" color="text.secondary">
                      {plan.descripcion}
                    </Typography>
                  )}
                  <Divider />
                  <Stack spacing={0.5} sx={{ flexGrow: 1 }}>
                    <Typography variant="body2">
                      Documentos/mes: <strong>{formatearLimite(plan.limiteFacturasMes)}</strong>
                    </Typography>
                    <Typography variant="body2">
                      Usuarios: <strong>{formatearLimite(plan.limiteUsuarios)}</strong>
                    </Typography>
                  </Stack>
                  <Button
                    variant={actual ? 'outlined' : 'contained'}
                    disabled={actual}
                    loading={procesando === plan.codigo}
                    onClick={() => onCambiar(plan.codigo)}
                  >
                    {actual ? 'Plan actual' : 'Cambiar a este plan'}
                  </Button>
                </Box>
              </Grid>
            )
          })}
        </Grid>
      </SectionCard>

      <PagosPanel key={refrescoPagos} empresaId={empresaId} />

      <ApiKeysPanel empresaId={empresaId} />
    </Box>
  )
}
