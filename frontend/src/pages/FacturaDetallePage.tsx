import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Grid from '@mui/material/Grid2'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import DownloadIcon from '@mui/icons-material/Download'
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf'
import { api, type FacturaDetalle } from '../api/client'
import { useEmpresaActiva } from '../features/auth/useEmpresaActiva'
import EstadoChip from '../components/ui/EstadoChip'
import SectionCard from '../components/ui/SectionCard'

const EVENTOS = [
  { codigo: '030', label: 'Acuse de recibo' },
  { codigo: '031', label: 'Reclamo' },
  { codigo: '032', label: 'Recibo del bien/servicio' },
  { codigo: '033', label: 'Aceptación expresa' },
  { codigo: '034', label: 'Aceptación tácita' },
]

async function descargar(url: string, nombre: string) {
  const token = localStorage.getItem('magnetixdian_token')
  const res = await fetch(url, { headers: token ? { Authorization: `Bearer ${token}` } : undefined })
  if (!res.ok) return
  const blob = await res.blob()
  const objectUrl = URL.createObjectURL(blob)
  const a = window.document.createElement('a')
  a.href = objectUrl
  a.download = nombre
  a.click()
  URL.revokeObjectURL(objectUrl)
}

export default function FacturaDetallePage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { empresaId } = useEmpresaActiva()
  const [factura, setFactura] = useState<FacturaDetalle | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [tipoEvento, setTipoEvento] = useState('030')
  const [registrando, setRegistrando] = useState(false)

  const cargar = useCallback(() => {
    if (id && empresaId != null) {
      api
        .get<FacturaDetalle>(`/facturacion/${empresaId}/${id}`)
        .then((res) => setFactura(res.data))
        .catch(() => setError('No se pudo cargar el documento.'))
    }
  }, [id, empresaId])

  useEffect(cargar, [cargar])

  const registrarEvento = async () => {
    if (id == null || empresaId == null) return
    setRegistrando(true)
    setError(null)
    try {
      await api.post(`/facturacion/${empresaId}/${id}/eventos`, { tipoEvento })
      cargar()
    } catch {
      setError('No se pudo registrar el evento.')
    } finally {
      setRegistrando(false)
    }
  }

  const money = (v: number) =>
    v?.toLocaleString('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

  if (error) return <Alert severity="error">{error}</Alert>
  if (!factura) return <Typography color="text.secondary">Cargando…</Typography>

  const cufe = factura.cufe ?? factura.cude ?? ''

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <Stack
        direction={{ xs: 'column', md: 'row' }}
        spacing={2}
        sx={{ alignItems: { md: 'center' }, justifyContent: 'space-between' }}
      >
        <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
          <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/facturacion')}>
            Volver
          </Button>
          <Typography variant="h5" component="h1">
            {factura.numeroCompleto}
          </Typography>
          <EstadoChip estado={factura.estado} />
        </Stack>
        <Stack direction="row" spacing={1}>
          <Button
            variant="outlined"
            startIcon={<PictureAsPdfIcon />}
            onClick={() =>
              descargar(`/api/facturacion/${empresaId}/${factura.id}/pdf`, `factura-${factura.numeroCompleto}.pdf`)
            }
          >
            PDF
          </Button>
          <Button
            variant="outlined"
            startIcon={<DownloadIcon />}
            onClick={() =>
              descargar(`/api/facturacion/${empresaId}/${factura.id}/xml`, `factura-${factura.numeroCompleto}.xml`)
            }
          >
            XML
          </Button>
        </Stack>
      </Stack>

      <SectionCard title="Datos del documento">
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, md: 4 }}>
            <Typography variant="overline" color="text.secondary">
              Adquirente
            </Typography>
            <Typography variant="body1">{factura.adquirente.nombre}</Typography>
            <Typography variant="body2" color="text.secondary">
              {factura.adquirente.tipoDocumento} {factura.adquirente.numeroDocumento}
            </Typography>
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <Typography variant="overline" color="text.secondary">
              Emisión
            </Typography>
            <Typography variant="body1">
              {factura.fechaEmision} {factura.horaEmision}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Ambiente {factura.ambiente === 1 ? 'producción' : 'habilitación'}
            </Typography>
          </Grid>
          <Grid size={{ xs: 12 }}>
            <Typography variant="overline" color="text.secondary">
              {factura.cufe ? 'CUFE' : 'CUDE'}
            </Typography>
            <Typography variant="caption" sx={{ display: 'block', wordBreak: 'break-all' }}>
              {cufe}
            </Typography>
          </Grid>
          {factura.qrUrl && (
            <Grid size={{ xs: 12 }}>
              <Typography variant="overline" color="text.secondary">
                Validación DIAN
              </Typography>
              <Typography variant="caption" sx={{ display: 'block', wordBreak: 'break-all' }}>
                {factura.qrUrl}
              </Typography>
            </Grid>
          )}
        </Grid>
      </SectionCard>

      <SectionCard title="Detalle" padded={false}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>#</TableCell>
              <TableCell>Descripción</TableCell>
              <TableCell align="right">Cant.</TableCell>
              <TableCell align="right">V. unitario</TableCell>
              <TableCell align="right">IVA</TableCell>
              <TableCell align="right">Total</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {factura.lineas.map((l, idx) => (
              <TableRow key={idx}>
                <TableCell>{idx + 1}</TableCell>
                <TableCell>{l.descripcion}</TableCell>
                <TableCell align="right">{l.cantidad}</TableCell>
                <TableCell align="right">{money(l.valorUnitario)}</TableCell>
                <TableCell align="right">{l.ivaPct}%</TableCell>
                <TableCell align="right">
                  {money(l.cantidad * l.valorUnitario + (l.cantidad * l.valorUnitario * l.ivaPct) / 100)}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </SectionCard>

      <SectionCard title="Eventos DIAN" padded={false}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Código</TableCell>
              <TableCell>Evento</TableCell>
              <TableCell>Estado</TableCell>
              <TableCell>Fecha</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {factura.eventos.length === 0 ? (
              <TableRow>
                <TableCell colSpan={4}>
                  <Typography variant="body2" color="text.secondary">
                    Sin eventos registrados.
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              factura.eventos.map((ev) => (
                <TableRow key={ev.id}>
                  <TableCell>{ev.tipoEvento}</TableCell>
                  <TableCell>{ev.descripcion}</TableCell>
                  <TableCell>
                    <Chip size="small" label={ev.estado} />
                  </TableCell>
                  <TableCell>{new Date(ev.fecha).toLocaleString('es-CO')}</TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
        <Stack direction="row" spacing={2} sx={{ p: 2, alignItems: 'center' }}>
          <TextField
            select
            size="small"
            label="Registrar evento"
            value={tipoEvento}
            onChange={(e) => setTipoEvento(e.target.value)}
            sx={{ minWidth: 240 }}
          >
            {EVENTOS.map((ev) => (
              <MenuItem key={ev.codigo} value={ev.codigo}>
                {ev.codigo} · {ev.label}
              </MenuItem>
            ))}
          </TextField>
          <Button variant="outlined" onClick={registrarEvento} disabled={registrando}>
            {registrando ? 'Registrando…' : 'Registrar'}
          </Button>
        </Stack>
      </SectionCard>

      <SectionCard>
        <Stack spacing={1} sx={{ alignItems: 'flex-end' }}>
          <Typography variant="body2">Subtotal: {money(factura.subtotal)}</Typography>
          <Typography variant="body2">Impuestos: {money(factura.totalImpuestos)}</Typography>
          <Typography variant="h6">Total a pagar: {money(factura.totalPagar)}</Typography>
          {factura.respuestaDian && (
            <Chip label={`Respuesta: ${factura.respuestaDian.slice(0, 80)}`} variant="outlined" />
          )}
        </Stack>
      </SectionCard>
    </Box>
  )
}
