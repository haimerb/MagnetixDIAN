import { useCallback, useEffect, useState } from 'react'
import { Link as RouterLink, useSearchParams } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid2'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { listarPlanes } from '../../api/client'
import type { Pago, Plan } from '../../api/client'
import { crearCheckoutEmpresa, estadoWebhookPago, PASARELA_PRUEBA } from '../../api/erp/pagos'
import type { EstadoWebhookPago } from '../../api/erp/pagos'
import EmptyState from '../../components/ui/EmptyState'
import LoadingState from '../../components/ui/LoadingState'
import Logo from '../../components/ui/Logo'
import SectionCard from '../../components/ui/SectionCard'
import { money } from '../../features/facturacion/calculos'

export default function CheckoutPage() {
  const [params] = useSearchParams()
  const empresaIdParam = Number(params.get('empresa'))
  const empresaId = Number.isInteger(empresaIdParam) && empresaIdParam > 0 ? empresaIdParam : null
  const [planes, setPlanes] = useState<Plan[]>([])
  const [planCodigo, setPlanCodigo] = useState(params.get('plan') ?? '')
  const [email, setEmail] = useState(params.get('email') ?? '')
  const [pago, setPago] = useState<Pago | null>(null)
  const [webhook, setWebhook] = useState<EstadoWebhookPago | null>(null)
  const [cargando, setCargando] = useState(true)
  const [procesando, setProcesando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let activo = true
    listarPlanes()
      .then((datos) => {
        if (!activo) return
        setPlanes(datos)
        setPlanCodigo((actual) => actual || datos[0]?.codigo || '')
      })
      .catch(() => {
        if (activo) setError('No se pudieron cargar los planes disponibles.')
      })
      .finally(() => {
        if (activo) setCargando(false)
      })
    return () => {
      activo = false
    }
  }, [])

  const planSeleccionado = planes.find((plan) => plan.codigo === planCodigo) ?? null

  const onPagar = useCallback(async () => {
    if (empresaId == null || !planCodigo) return
    setProcesando(true)
    setError(null)
    setWebhook(null)
    try {
      const resultado = await crearCheckoutEmpresa(empresaId, {
        planCodigo,
        proveedor: PASARELA_PRUEBA,
        metodo: 'CARD',
        email: email.trim() || undefined,
      })
      setPago(resultado)
    } catch {
      setError('El pago de prueba no pudo completarse. Revise los datos e intente de nuevo.')
    } finally {
      setProcesando(false)
    }
  }, [email, empresaId, planCodigo])

  const onConsultarWebhook = useCallback(async () => {
    if (empresaId == null || !pago) return
    setError(null)
    try {
      setWebhook(await estadoWebhookPago(empresaId, pago.referencia))
    } catch {
      setError('No se pudo consultar el estado del webhook.')
    }
  }, [empresaId, pago])

  if (cargando) return <LoadingState label="Cargando checkout…" minHeight="70vh" />

  return (
    <Box
      component="main"
      sx={{ minHeight: '100vh', bgcolor: 'background.default', py: { xs: 3, md: 6 }, px: 2 }}
    >
      <Grid container spacing={3} sx={{ maxWidth: 1040, mx: 'auto' }}>
        <Grid size={{ xs: 12 }}>
          <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
            <Logo size={32} />
            <Typography variant="h5" component="h1">
              Finalizar suscripción
            </Typography>
          </Stack>
        </Grid>

        {empresaId == null && (
          <Grid size={{ xs: 12 }}>
            <EmptyState
              title="Seleccione una empresa desde el registro"
              description="El checkout requiere una empresa activa. Cree su cuenta para continuar con el pago de prueba."
              action={<Button component={RouterLink} to="/registro" variant="contained">Crear cuenta</Button>}
            />
          </Grid>
        )}

        {error && (
          <Grid size={{ xs: 12 }}>
            <Alert severity="error">{error}</Alert>
          </Grid>
        )}

        <Grid size={{ xs: 12, md: 7 }}>
          <SectionCard title="Plan y método de pago" subtitle="Pago de prueba sin credenciales reales">
            <Stack spacing={2}>
              <TextField
                select
                label="Plan"
                value={planCodigo}
                onChange={(evento) => setPlanCodigo(evento.target.value)}
                disabled={planes.length === 0}
                fullWidth
              >
                {planes.length === 0 && <MenuItem value="" disabled>No hay planes disponibles</MenuItem>}
                {planes.map((plan) => (
                  <MenuItem key={plan.codigo} value={plan.codigo}>
                    {plan.nombre} · {money(plan.precioMensual)} / mes
                  </MenuItem>
                ))}
              </TextField>
              <TextField
                label="Correo para verificación"
                type="email"
                value={email}
                onChange={(evento) => setEmail(evento.target.value)}
                placeholder="nombre@empresa.com"
                fullWidth
              />
              <Alert severity="info">
                Se usa la pasarela simulada: el pago se aprueba al instante y no genera cargos.
              </Alert>
              <Button
                variant="contained"
                size="large"
                loading={procesando}
                disabled={empresaId == null || !planCodigo || procesando}
                onClick={() => void onPagar()}
              >
                Pagar con pasarela de prueba
              </Button>
            </Stack>
          </SectionCard>
        </Grid>

        <Grid size={{ xs: 12, md: 5 }}>
          <SectionCard title="Resumen">
            {planSeleccionado ? (
              <Stack spacing={1.5}>
                <Typography variant="body2" color="text.secondary">Plan seleccionado</Typography>
                <Typography variant="h6">{planSeleccionado.nombre}</Typography>
                <Typography variant="body2">{planSeleccionado.descripcion ?? 'Suscripción mensual'}</Typography>
                <Typography variant="h5">{money(planSeleccionado.precioMensual)}</Typography>
                <Typography variant="caption" color="text.secondary">
                  {planSeleccionado.limiteFacturasMes} facturas y {planSeleccionado.limiteUsuarios} usuario(s) al mes.
                </Typography>
              </Stack>
            ) : (
              <EmptyState title="Sin plan seleccionado" description="Elija un plan para ver el resumen." />
            )}
          </SectionCard>
        </Grid>

        {pago && (
          <Grid size={{ xs: 12 }}>
            <Paper variant="outlined" sx={{ p: 3, display: 'grid', gap: 1.5 }}>
              <Typography variant="h6" component="h2">
                Resultado del pago
              </Typography>
              <Typography>Referencia: {pago.referencia}</Typography>
              <Typography>Estado: {pago.estado}</Typography>
              <Typography>Proveedor: {pago.proveedor}</Typography>
              <Typography>Monto: {money(pago.monto)}</Typography>
              <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
                <Button variant="outlined" onClick={() => void onConsultarWebhook()}>
                  Consultar estado del webhook
                </Button>
                <Button component={RouterLink} to={`/verificar-email?email=${encodeURIComponent(email)}&referencia=${encodeURIComponent(pago.referencia)}`} variant="contained">
                  Verificar correo
                </Button>
                <Button component={RouterLink} to={`/app/habilitacion-dian?empresa=${empresaId}`} variant="outlined">
                  Habilitar DIAN
                </Button>
              </Stack>
              {webhook && (
                <Alert severity={webhook.firmado ? 'success' : 'error'}>
                  Webhook {webhook.estado} · firma {webhook.firmado ? 'válida' : 'ausente o inválida'}
                  {webhook.mensaje ? ` · ${webhook.mensaje}` : ''}
                </Alert>
              )}
            </Paper>
          </Grid>
        )}
      </Grid>
    </Box>
  )
}
