import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
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
import Typography from '@mui/material/Typography'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import DownloadIcon from '@mui/icons-material/Download'
import EmailOutlinedIcon from '@mui/icons-material/EmailOutlined'
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf'
import { api, reenviarFactura, type FacturaDetalle } from '../api/client'
import { descargarArchivo } from '../api/descargas'
import { useEmpresaActiva } from '../features/auth/useEmpresaActiva'
import { money, totalesLinea } from '../features/facturacion/calculos'
import EstadoChip from '../components/ui/EstadoChip'
import LoadingState from '../components/ui/LoadingState'
import SectionCard from '../components/ui/SectionCard'

const EVENTOS = [
  { codigo: '030', label: 'Acuse de recibo' },
  { codigo: '031', label: 'Reclamo' },
  { codigo: '032', label: 'Recibo del bien/servicio' },
  { codigo: '033', label: 'Aceptación expresa' },
  { codigo: '034', label: 'Aceptación tácita' },
]

export default function FacturaDetallePage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { empresaId } = useEmpresaActiva()
  const [factura, setFactura] = useState<FacturaDetalle | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [tipoEvento, setTipoEvento] = useState('030')
  const [registrando, setRegistrando] = useState(false)
  const [descargando, setDescargando] = useState<'pdf' | 'xml' | null>(null)
  const [cargando, setCargando] = useState(true)
  const [reenvioAbierto, setReenvioAbierto] = useState(false)
  const [destinatario, setDestinatario] = useState('')
  const [reenviando, setReenviando] = useState(false)
  const [aviso, setAviso] = useState<string | null>(null)

  const cargar = useCallback(() => {
    if (id == null || empresaId == null) {
      setCargando(false)
      return
    }
    setCargando(true)
    api
      .get<FacturaDetalle>(`/facturacion/${empresaId}/${id}`)
      .then((res) => setFactura(res.data))
      .catch(() => setError('No se pudo cargar el documento.'))
      .finally(() => setCargando(false))
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

  if (!factura) {
    if (error) return <Alert severity="error">{error}</Alert>
    if (cargando) return <LoadingState label="Cargando documento…" minHeight="40vh" />
    return <Alert severity="info">No se encontró el documento.</Alert>
  }

  const onDescargar = async (tipo: 'pdf' | 'xml') => {
    setDescargando(tipo)
    setError(null)
    try {
      await descargarArchivo(
        `/api/facturacion/${empresaId}/${factura.id}/${tipo}`,
        `factura-${factura.numeroCompleto}.${tipo}`,
      )
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo descargar el archivo.')
    } finally {
      setDescargando(null)
    }
  }

  const cufe = factura.cufe ?? factura.cude ?? ''

  const abrirReenvio = () => {
    setDestinatario(factura.adquirente.email ?? '')
    setAviso(null)
    setError(null)
    setReenvioAbierto(true)
  }

  const onReenviar = async () => {
    if (empresaId == null) return
    setReenviando(true)
    setError(null)
    try {
      const resultado = await reenviarFactura(empresaId, factura.id, destinatario)
      setAviso(`Factura reenviada a ${resultado.destinatario}.`)
      setReenvioAbierto(false)
    } catch (e: unknown) {
      const detalle = (e as { response?: { data?: { message?: string } } }).response?.data?.message
      setError(detalle ?? 'No se pudo reenviar la factura.')
    } finally {
      setReenviando(false)
    }
  }

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
          <Button variant="outlined" startIcon={<EmailOutlinedIcon />} onClick={abrirReenvio}>
            Reenviar
          </Button>
          <Button
            variant="outlined"
            startIcon={<PictureAsPdfIcon />}
            onClick={() => onDescargar('pdf')}
            disabled={descargando !== null}
            loading={descargando === 'pdf'}
          >
            PDF
          </Button>
          <Button
            variant="outlined"
            startIcon={<DownloadIcon />}
            onClick={() => onDescargar('xml')}
            disabled={descargando !== null}
            loading={descargando === 'xml'}
          >
            XML
          </Button>
        </Stack>
      </Stack>

      {error && <Alert severity="error">{error}</Alert>}
      {aviso && <Alert severity="success" onClose={() => setAviso(null)}>{aviso}</Alert>}

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
        <TableContainer>
          <Table size="small" sx={{ minWidth: 600 }}>
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
                    {money(totalesLinea(l).total)}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </SectionCard>

      <SectionCard title="Eventos DIAN" padded={false}>
        <TableContainer>
          <Table size="small" sx={{ minWidth: 520 }}>
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
        </TableContainer>
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
          <Button variant="outlined" onClick={registrarEvento} disabled={registrando} loading={registrando}>
            Registrar
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

      <Dialog open={reenvioAbierto} onClose={() => { if (!reenviando) setReenvioAbierto(false) }} fullWidth maxWidth="xs">
        <DialogTitle>Reenviar factura por correo</DialogTitle>
        <DialogContent sx={{ display: 'grid', gap: 2, pt: '16px !important' }}>
          <Typography variant="body2" color="text.secondary">
            Se enviarán el XML firmado y el PDF de {factura.numeroCompleto} al correo indicado.
          </Typography>
          <TextField
            label="Correo electrónico"
            type="email"
            value={destinatario}
            onChange={(e) => setDestinatario(e.target.value)}
            fullWidth
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setReenvioAbierto(false)} disabled={reenviando}>Cancelar</Button>
          <Button variant="contained" onClick={onReenviar} disabled={!destinatario.trim()} loading={reenviando}>
            Enviar
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
