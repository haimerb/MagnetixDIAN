import { useCallback, useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Divider from '@mui/material/Divider'
import FormControl from '@mui/material/FormControl'
import Grid from '@mui/material/Grid2'
import InputLabel from '@mui/material/InputLabel'
import LinearProgress from '@mui/material/LinearProgress'
import MenuItem from '@mui/material/MenuItem'
import Select from '@mui/material/Select'
import Stack from '@mui/material/Stack'
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
  type Plan,
  type Suscripcion,
} from '../api/client'

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

export default function SuscripcionPage() {
  const { empresaId, cargando: cargandoEmpresa } = useEmpresaActiva()
  const [planes, setPlanes] = useState<Plan[]>([])
  const [suscripcion, setSuscripcion] = useState<Suscripcion | null>(null)
  const [cargando, setCargando] = useState(true)
  const [procesando, setProcesando] = useState<string | null>(null)
  const [proveedor, setProveedor] = useState('SIMULADO')
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
    setProcesando(codigo)
    setError(null)
    setOk(null)
    try {
      const pago = await iniciarCheckout(empresaId, codigo, proveedor)
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
        <PageHeader overline="Facturación" title="Suscripción" />
        <Alert severity="info">
          No hay una empresa asociada a su usuario. Contacte al administrador.
        </Alert>
      </Box>
    )
  }

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="Facturación"
        title="Suscripción y planes"
        subtitle="Consulte su plan actual, el consumo del mes y cambie de plan cuando lo necesite."
      />

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
          <FormControl size="small" sx={{ minWidth: 200 }}>
            <InputLabel id="pasarela-label">Pasarela de pago</InputLabel>
            <Select
              labelId="pasarela-label"
              label="Pasarela de pago"
              value={proveedor}
              onChange={(e) => setProveedor(e.target.value)}
            >
              {PASARELAS.map((p) => (
                <MenuItem key={p.valor} value={p.valor}>
                  {p.etiqueta}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        }
      >
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
