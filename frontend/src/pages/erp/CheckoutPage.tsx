import { useCallback, useEffect, useState } from 'react'
import { Link as RouterLink, useSearchParams } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Checkbox from '@mui/material/Checkbox'
import FormControlLabel from '@mui/material/FormControlLabel'
import Grid from '@mui/material/Grid2'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { listarPlanes } from '../../api/client'
import type { Plan } from '../../api/client'
import {
  crearCheckoutBilling,
  estadoWebhookPago,
  METODOS_CHECKOUT,
  PROVEEDORES_CHECKOUT,
  requiereDatosWompi,
} from '../../api/erp/pagos'
import type {
  CheckoutBillingResponse,
  EstadoWebhookPago,
  MetodoPago,
  ProveedorCheckout,
} from '../../api/erp/pagos'
import EmptyState from '../../components/ui/EmptyState'
import LoadingState from '../../components/ui/LoadingState'
import Logo from '../../components/ui/Logo'
import SectionCard from '../../components/ui/SectionCard'
import { money } from '../../features/facturacion/calculos'

function mensajeServidor(error: unknown, respaldo: string) {
  const respuesta = error as { response?: { data?: { message?: string } } }
  return respuesta.response?.data?.message ?? respaldo
}

function urlImagenQr(valor: string | null | undefined) {
  if (!valor) return null
  return valor.startsWith('data:') ? valor : `data:image/svg+xml;base64,${valor}`
}

function metodoPermitido(proveedor: ProveedorCheckout, metodo: MetodoPago) {
  return proveedor === 'WOMPI' || !requiereDatosWompi(metodo)
}

export default function CheckoutPage() {
  const [params] = useSearchParams()
  const empresaIdParam = Number(params.get('empresa'))
  const empresaId = Number.isInteger(empresaIdParam) && empresaIdParam > 0 ? empresaIdParam : null
  const [planes, setPlanes] = useState<Plan[]>([])
  const [planCodigo, setPlanCodigo] = useState(params.get('plan') ?? '')
  const [proveedor, setProveedor] = useState<ProveedorCheckout>('WOMPI')
  const [metodo, setMetodo] = useState<MetodoPago>('CARD')
  const [email, setEmail] = useState(params.get('email') ?? '')
  const [celular, setCelular] = useState('')
  const [tipoDocumento, setTipoDocumento] = useState('CC')
  const [numeroDocumento, setNumeroDocumento] = useState('')
  const [nombrePagador, setNombrePagador] = useState('')
  const [aceptaTerminos, setAceptaTerminos] = useState(false)
  const [checkout, setCheckout] = useState<CheckoutBillingResponse | null>(null)
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
  const imagenQr = urlImagenQr(checkout?.qrImage)

  const onPagar = useCallback(async () => {
    if (!planCodigo || !email.trim()) return
    if (requiereDatosWompi(metodo) && !aceptaTerminos) {
      setError('Debe aceptar los términos y el tratamiento de datos personales de Wompi.')
      return
    }
    if (metodo === 'DAVIPLATA' && (!tipoDocumento.trim() || !numeroDocumento.trim())) {
      setError('Debe indicar el tipo y número de documento para Daviplata.')
      return
    }
    setProcesando(true)
    setError(null)
    setWebhook(null)
    setCheckout(null)
    try {
      setCheckout(await crearCheckoutBilling({
        planCodigo,
        proveedor,
        correo: email.trim(),
        metodo,
        aceptaTerminos,
        ...(celular.trim() ? { celular: celular.trim() } : {}),
        ...(nombrePagador.trim() ? { payerName: nombrePagador.trim() } : {}),
        ...(metodo === 'DAVIPLATA' ? {
          documentType: tipoDocumento.trim(),
          documentNumber: numeroDocumento.trim(),
        } : {}),
        ...(empresaId == null ? {} : { empresaId }),
      }))
    } catch (errorPago) {
      setError(mensajeServidor(errorPago, 'No se pudo iniciar el checkout. Revise los datos e intente de nuevo.'))
    } finally {
      setProcesando(false)
    }
  }, [aceptaTerminos, celular, email, empresaId, metodo, nombrePagador, numeroDocumento, planCodigo, proveedor, tipoDocumento])

  const onConsultarWebhook = useCallback(async () => {
    if (!checkout) return
    setError(null)
    try {
      setWebhook(await estadoWebhookPago(checkout.referencia))
    } catch (errorEstado) {
      setError(mensajeServidor(errorEstado, 'No se pudo consultar el estado del webhook.'))
    }
  }, [checkout])

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

        {error && (
          <Grid size={{ xs: 12 }}>
            <Alert severity="error">{error}</Alert>
          </Grid>
        )}

        <Grid size={{ xs: 12, md: 7 }}>
          <SectionCard title="Plan y método de pago" subtitle="Checkout seguro con pasarela real">
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
                select
                label="Pasarela de pago"
                value={proveedor}
                onChange={(evento) => {
                  const siguiente = evento.target.value as ProveedorCheckout
                  setProveedor(siguiente)
                  if (!metodoPermitido(siguiente, metodo)) setMetodo('CARD')
                }}
                fullWidth
              >
                {PROVEEDORES_CHECKOUT.map((opcion) => (
                  <MenuItem key={opcion.value} value={opcion.value}>
                    {opcion.label}
                  </MenuItem>
                ))}
              </TextField>
              <TextField
                select
                label="Método de pago"
                value={metodo}
                onChange={(evento) => setMetodo(evento.target.value as MetodoPago)}
                fullWidth
              >
                {METODOS_CHECKOUT.map((opcion) => (
                  <MenuItem
                    key={opcion.value}
                    value={opcion.value}
                    disabled={!metodoPermitido(proveedor, opcion.value)}
                  >
                    {opcion.label}
                  </MenuItem>
                ))}
              </TextField>
              <TextField
                label="Correo del pagador"
                type="email"
                value={email}
                onChange={(evento) => setEmail(evento.target.value)}
                placeholder="nombre@empresa.com"
                helperText="Debe coincidir con el correo usado al registrar la empresa."
                fullWidth
              />
              {metodo === 'DAVIPLATA' && (
                <>
                  <TextField
                    select
                    label="Tipo de documento"
                    value={tipoDocumento}
                    onChange={(evento) => setTipoDocumento(evento.target.value)}
                    fullWidth
                  >
                    <MenuItem value="CC">Cédula de ciudadanía</MenuItem>
                    <MenuItem value="CE">Cédula de extranjería</MenuItem>
                    <MenuItem value="TI">Tarjeta de identidad</MenuItem>
                  </TextField>
                  <TextField
                    label="Número de documento"
                    value={numeroDocumento}
                    onChange={(evento) => setNumeroDocumento(evento.target.value)}
                    fullWidth
                  />
                </>
              )}
              {(metodo === 'DAVIPLATA' || metodo === 'NEQUI' || metodo === 'PSE') && (
                <>
                  <TextField
                    label="Nombre del pagador"
                    value={nombrePagador}
                    onChange={(evento) => setNombrePagador(evento.target.value)}
                    fullWidth
                  />
                  <TextField
                    label="Celular"
                    value={celular}
                    onChange={(evento) => setCelular(evento.target.value)}
                    placeholder="3107654321"
                    fullWidth
                  />
                </>
              )}
              {requiereDatosWompi(metodo) && (
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={aceptaTerminos}
                      onChange={(evento) => setAceptaTerminos(evento.target.checked)}
                    />
                  }
                  label="Acepto las políticas de privacidad y el tratamiento de datos personales de Wompi"
                />
              )}
              <Alert severity="info">
                El pago se confirma con webhook firmado. La suscripción se activa automáticamente y podrá descargar
                el comprobante desde el estado del pago.
              </Alert>
              <Button
                variant="contained"
                size="large"
                loading={procesando}
                disabled={
                  !planCodigo
                  || !email.trim()
                  || procesando
                  || (requiereDatosWompi(metodo) && !aceptaTerminos)
                  || (metodo === 'DAVIPLATA' && (!tipoDocumento.trim() || !numeroDocumento.trim()))
                }
                onClick={() => void onPagar()}
              >
                Continuar al pago
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

        {checkout && (
          <Grid size={{ xs: 12 }}>
            <Paper variant="outlined" sx={{ p: 3, display: 'grid', gap: 1.5 }}>
              <Typography variant="h6" component="h2">
                Checkout iniciado
              </Typography>
              <Typography>Referencia: {checkout.referencia}</Typography>
              <Typography>Estado: {checkout.estado}</Typography>
              <Typography>Monto: {money(checkout.monto)}</Typography>
              {checkout.tipo === 'QR' && imagenQr && (
                <Stack spacing={1} sx={{ alignItems: 'center', py: 2 }}>
                  <Box
                    component="img"
                    src={imagenQr}
                    alt={`QR de pago ${checkout.referencia}`}
                    sx={{
                      width: 220,
                      height: 220,
                      p: 1,
                      bgcolor: 'background.paper',
                      border: '1px solid',
                      borderColor: 'divider',
                      borderRadius: 2,
                    }}
                  />
                  <Typography variant="body2" color="text.secondary">
                    Escanea el QR con la app de Bancolombia o Nequi.
                  </Typography>
                  {checkout.qrId && (
                    <Typography variant="caption" color="text.secondary">
                      ID QR: {checkout.qrId}
                    </Typography>
                  )}
                </Stack>
              )}
              <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
                {checkout.tipo !== 'QR' && (
                  <Button component="a" href={checkout.checkoutUrl} target="_blank" rel="noreferrer" variant="contained">
                    {checkout.tipo === 'OTP' ? 'Continuar a Daviplata (OTP)' : `Ir al checkout de ${proveedor}`}
                  </Button>
                )}
                <Button variant="outlined" onClick={() => void onConsultarWebhook()}>
                  Consultar estado del webhook
                </Button>
                <Button
                  component={RouterLink}
                  to={`/verificar-email?email=${encodeURIComponent(email)}&referencia=${encodeURIComponent(checkout.referencia)}`}
                  variant="outlined"
                >
                  Verificar correo
                </Button>
                {empresaId != null && (
                  <Button
                    component={RouterLink}
                    to={`/app/habilitacion-dian?empresa=${empresaId}`}
                    variant="text"
                  >
                    Habilitar DIAN
                  </Button>
                )}
              </Stack>
              {webhook && (
                <Alert severity={webhook.firmado && webhook.estado === 'APROBADO' ? 'success' : 'info'}>
                  Webhook {webhook.estado} · firma {webhook.firmado ? 'válida' : 'ausente o inválida'}
                  {webhook.eventId ? ` · evento ${webhook.eventId}` : ''}
                  {webhook.comprobanteNumero ? ` · comprobante ${webhook.comprobanteNumero}` : ''}
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
