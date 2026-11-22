import { useMemo, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Checkbox from '@mui/material/Checkbox'
import FormControlLabel from '@mui/material/FormControlLabel'
import Grid from '@mui/material/Grid2'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import SendIcon from '@mui/icons-material/Send'
import WhatsAppIcon from '@mui/icons-material/WhatsApp'
import { api, entregarFacturaWhatsapp } from '../api/client'
import type {
  EntregaWhatsAppResponse,
  FacturaRequest,
  LineaFacturaRequest,
  ResultadoEmision,
} from '../api/client'
import PageHeader from '../components/ui/PageHeader'
import SectionCard from '../components/ui/SectionCard'
import StatCard from '../components/ui/StatCard'
import { useEmpresaActiva } from '../features/auth/useEmpresaActiva'
import { useNotificaciones } from '../features/notificaciones/contexto'
import { money, totalesLinea } from '../features/facturacion/calculos'

type ResultadoRapido = { emision: ResultadoEmision; entrega: EntregaWhatsAppResponse | null }

export default function FacturaRapidaPage() {
  const { empresaId, cargando: cargandoEmpresa } = useEmpresaActiva()
  const { notificar } = useNotificaciones()
  const [nombre, setNombre] = useState('')
  const [numeroDocumento, setNumeroDocumento] = useState('')
  const [telefono, setTelefono] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [cantidad, setCantidad] = useState('1')
  const [valorUnitario, setValorUnitario] = useState('')
  const [conIva, setConIva] = useState(true)
  const [emitiendo, setEmitiendo] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [resultado, setResultado] = useState<ResultadoRapido | null>(null)

  const linea = useMemo<LineaFacturaRequest>(
    () => ({
      codigoProducto: null,
      descripcion,
      cantidad: Number(cantidad) || 0,
      unidadMedida: '94',
      valorUnitario: Number(valorUnitario) || 0,
      descuento: 0,
      ivaPct: conIva ? 19 : 0,
      incPct: 0,
      codigoUnspsc: null,
    }),
    [descripcion, cantidad, valorUnitario, conIva],
  )

  const total = useMemo(() => totalesLinea(linea).total, [linea])
  const valido = Boolean(
    nombre.trim() && descripcion.trim() && numeroDocumento.trim() && linea.cantidad > 0 && linea.valorUnitario > 0,
  )

  const onEmitir = async () => {
    if (empresaId == null || !valido) return
    setEmitiendo(true)
    setError(null)
    try {
      const solicitud: FacturaRequest = {
        tipoDocumento: '01',
        adquirente: {
          tipoDocumento: '31',
          numeroDocumento: numeroDocumento.trim(),
          digitoVerificacion: null,
          nombre: nombre.trim(),
          email: null,
          direccion: null,
          municipio: null,
          regimen: null,
          responsabilidadFiscal: null,
        },
        lineas: [linea],
        observaciones: null,
        formaPago: '1',
        medioPago: '10',
        tipoOperacion: '10',
        fechaVencimiento: null,
        cufeReferencia: null,
        numeroReferencia: null,
      }
      const { data: emision } = await api.post<ResultadoEmision>(
        `/facturacion/${empresaId}/emitir`,
        solicitud,
      )
      let entrega: EntregaWhatsAppResponse | null = null
      if (emision.id != null && telefono.trim()) {
        try {
          entrega = await entregarFacturaWhatsapp(empresaId, emision.id, telefono.trim())
        } catch {
          notificar('La factura se emitió pero no se pudo entregar por WhatsApp.', 'warning')
        }
      }
      setResultado({ emision, entrega })
      notificar(`Factura ${emision.numeroCompleto ?? ''} emitida.`, 'success')
    } catch (e: unknown) {
      const detalle = (e as { response?: { data?: { message?: string } } }).response?.data?.message
      setError(detalle ?? 'No se pudo emitir la factura.')
    } finally {
      setEmitiendo(false)
    }
  }

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="Facturación ágil"
        title="Factura rápida"
        subtitle="Emita una factura en segundos y entréguela por WhatsApp desde el móvil."
      />

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 7 }}>
          <SectionCard title="Datos de la factura" subtitle="Un solo ítem para una emisión inmediata">
            <Grid container spacing={2}>
              <Grid size={{ xs: 12, md: 6 }}>
                <TextField
                  label="Nombre del cliente"
                  value={nombre}
                  onChange={(event) => setNombre(event.target.value)}
                  required
                />
              </Grid>
              <Grid size={{ xs: 12, md: 6 }}>
                <TextField
                  label="Número de documento"
                  value={numeroDocumento}
                  onChange={(event) => setNumeroDocumento(event.target.value)}
                  required
                />
              </Grid>
              <Grid size={{ xs: 12, md: 6 }}>
                <TextField
                  label="WhatsApp"
                  value={telefono}
                  onChange={(event) => setTelefono(event.target.value)}
                  helperText="Opcional: si lo indica, se genera el enlace de entrega."
                />
              </Grid>
              <Grid size={{ xs: 12 }}>
                <TextField
                  label="Descripción"
                  value={descripcion}
                  onChange={(event) => setDescripcion(event.target.value)}
                  required
                />
              </Grid>
              <Grid size={{ xs: 12, md: 4 }}>
                <TextField
                  label="Cantidad"
                  value={cantidad}
                  onChange={(event) => setCantidad(event.target.value)}
                  inputProps={{ inputMode: 'numeric', min: 1 }}
                />
              </Grid>
              <Grid size={{ xs: 12, md: 4 }}>
                <TextField
                  label="Valor unitario"
                  value={valorUnitario}
                  onChange={(event) => setValorUnitario(event.target.value)}
                  inputProps={{ inputMode: 'decimal', min: 0 }}
                />
              </Grid>
              <Grid size={{ xs: 12, md: 4 }}>
                <FormControlLabel
                  control={<Checkbox checked={conIva} onChange={(event) => setConIva(event.target.checked)} />}
                  label="Incluir IVA 19%"
                />
              </Grid>
            </Grid>

            <Stack direction="row" spacing={2} sx={{ mt: 3 }}>
              <Button
                variant="contained"
                startIcon={<SendIcon />}
                disabled={empresaId == null || !valido || emitiendo || cargandoEmpresa}
                loading={emitiendo}
                onClick={onEmitir}
              >
                Emitir y entregar
              </Button>
            </Stack>
          </SectionCard>
        </Grid>

        <Grid size={{ xs: 12, md: 5 }}>
          <Stack spacing={2}>
            <StatCard label="Total de la factura" value={money(total)} tone="primary" />
            <SectionCard title="Entrega" subtitle="La factura queda lista para compartir">
              {resultado == null ? (
                <Typography variant="body2" color="text.secondary">
                  Complete los datos y emita. Si indicó un WhatsApp, se generará el enlace de entrega.
                </Typography>
              ) : (
                <Stack spacing={1.5}>
                  <Alert severity="success">
                    {resultado.emision.numeroCompleto ?? 'Documento'} · {resultado.emision.estado}
                  </Alert>
                  {resultado.emision.errores.length > 0 && (
                    <Alert severity="warning">
                      {resultado.emision.errores.map((e) => e.mensaje ?? e.codigo).join(' · ')}
                    </Alert>
                  )}
                  {resultado.entrega?.enviado && resultado.entrega.enlace && (
                    <Button
                      variant="outlined"
                      startIcon={<WhatsAppIcon />}
                      component="a"
                      href={resultado.entrega.enlace}
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      Abrir WhatsApp
                    </Button>
                  )}
                  {resultado.entrega?.mensaje && (
                    <Typography variant="body2" color="text.secondary">
                      {resultado.entrega.mensaje}
                    </Typography>
                  )}
                </Stack>
              )}
            </SectionCard>
          </Stack>
        </Grid>
      </Grid>

      {empresaId == null && !cargandoEmpresa && (
        <Alert severity="info">No hay una empresa asociada para emitir facturas.</Alert>
      )}
      {error && <Alert severity="error">{error}</Alert>}
    </Box>
  )
}
