import { useEffect, useMemo, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import Divider from '@mui/material/Divider'
import Grid from '@mui/material/Grid2'
import IconButton from '@mui/material/IconButton'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import AddIcon from '@mui/icons-material/Add'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import { api, type ErrorValidacion, type FacturaRequest, type FacturaResumen, type LineaFacturaRequest, type ResultadoEmision } from '../../api/client'
import { money, totalesLinea } from '../../features/facturacion/calculos'

const TIPOS = [
  { codigo: '01', label: 'Factura de venta' },
  { codigo: '91', label: 'Nota crédito' },
  { codigo: '92', label: 'Nota débito' },
  { codigo: '05', label: 'Documento soporte' },
]

const TIPOS_DOC = [
  { codigo: '13', label: 'Cédula de ciudadanía' },
  { codigo: '31', label: 'NIT' },
  { codigo: '22', label: 'Cédula de extranjería' },
  { codigo: '41', label: 'Pasaporte' },
]

function lineaVacia(): LineaFacturaRequest {
  return {
    codigoProducto: '',
    descripcion: '',
    cantidad: 1,
    unidadMedida: '94',
    valorUnitario: 0,
    descuento: 0,
    ivaPct: 19,
    incPct: 0,
    codigoUnspsc: '',
  }
}

export default function FacturaEditor({
  empresaId,
  abierto,
  onCerrar,
  onEmitida,
}: {
  empresaId: number
  abierto: boolean
  onCerrar: () => void
  onEmitida: (r: ResultadoEmision) => void
}) {
  const [tipoDocumento, setTipoDocumento] = useState('01')
  const [adquirente, setAdquirente] = useState({
    tipoDocumento: '13',
    numeroDocumento: '',
    digitoVerificacion: '',
    nombre: '',
    email: '',
    direccion: '',
    municipio: '',
    regimen: 'ORDINARIO',
    responsabilidadFiscal: 'R-99-PN',
  })
  const [lineas, setLineas] = useState<LineaFacturaRequest[]>([lineaVacia()])
  const [observaciones, setObservaciones] = useState('')
  const [errores, setErrores] = useState<ErrorValidacion[]>([])
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [validando, setValidando] = useState(false)
  const [facturas, setFacturas] = useState<FacturaResumen[]>([])
  const [referenciaId, setReferenciaId] = useState<number | ''>('')
  const [cufeReferencia, setCufeReferencia] = useState<string | null>(null)
  const [numeroReferencia, setNumeroReferencia] = useState<string | null>(null)

  const esNota = tipoDocumento === '91' || tipoDocumento === '92'

  useEffect(() => {
    if (!abierto) return
    api
      .get<FacturaResumen[]>(`/facturacion/${empresaId}`)
      .then((res) => setFacturas(res.data.filter((f) => f.tipoDocumento === '01')))
      .catch(() => setFacturas([]))
  }, [abierto, empresaId])

  const seleccionarReferencia = (id: number | '') => {
    setReferenciaId(id)
    const ref = facturas.find((f) => f.id === id)
    setNumeroReferencia(ref?.numeroCompleto ?? null)
    setCufeReferencia(ref?.cufe ?? null)
    if (ref) {
      setAdquirente((a) => ({
        ...a,
        numeroDocumento: a.numeroDocumento || ref.adquirenteNumDoc,
        nombre: a.nombre || ref.adquirenteNombre,
      }))
    }
  }

  const totales = useMemo(() => {
    let subtotal = 0
    let impuestos = 0
    for (const l of lineas) {
      const t = totalesLinea(l)
      subtotal += t.base
      impuestos += t.iva + t.inc
    }
    return { subtotal, impuestos, total: subtotal + impuestos }
  }, [lineas])

  const actualizarLinea = (idx: number, patch: Partial<LineaFacturaRequest>) => {
    setLineas((prev) => prev.map((l, i) => (i === idx ? { ...l, ...patch } : l)))
  }

  const construirRequest = (): FacturaRequest => ({
    tipoDocumento,
    adquirente: {
      ...adquirente,
      digitoVerificacion: adquirente.digitoVerificacion === '' ? null : Number(adquirente.digitoVerificacion),
      email: adquirente.email || null,
      direccion: adquirente.direccion || null,
      municipio: adquirente.municipio || null,
    },
    lineas,
    observaciones: observaciones || null,
    formaPago: '1',
    medioPago: '10',
    tipoOperacion: '10',
    fechaVencimiento: null,
    cufeReferencia,
    numeroReferencia,
  })

  const onValidar = async () => {
    setValidando(true)
    setError(null)
    try {
      const { data } = await api.post<ErrorValidacion[]>(`/facturacion/${empresaId}/validar`, construirRequest())
      setErrores(data)
      if (data.length === 0) setErrores([])
    } catch {
      setError('No se pudo validar el documento.')
    } finally {
      setValidando(false)
    }
  }

  const onEmitir = async () => {
    setEnviando(true)
    setError(null)
    setErrores([])
    try {
      const { data } = await api.post<ResultadoEmision>(`/facturacion/${empresaId}/emitir`, construirRequest())
      if (data.exitoso) {
        onEmitida(data)
      } else {
        setErrores(data.errores)
        setError(data.mensaje ?? 'El documento no pudo emitirse.')
      }
    } catch {
      setError('No se pudo emitir el documento.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Dialog open={abierto} onClose={() => { if (!enviando) onCerrar() }} fullWidth maxWidth="lg">
      <DialogTitle>Nueva factura electrónica</DialogTitle>
      <DialogContent
        dividers
        sx={{ pointerEvents: enviando ? 'none' : undefined, opacity: enviando ? 0.7 : 1 }}
      >
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField
              select
              fullWidth
              label="Tipo de documento"
              value={tipoDocumento}
              onChange={(e) => {
                const nuevo = e.target.value
                setTipoDocumento(nuevo)
                if (nuevo !== '91' && nuevo !== '92') seleccionarReferencia('')
              }}
            >
              {TIPOS.map((t) => (
                <MenuItem key={t.codigo} value={t.codigo}>{t.label}</MenuItem>
              ))}
            </TextField>
          </Grid>
          {esNota && (
            <Grid size={{ xs: 12, md: 8 }}>
              <TextField
                select
                fullWidth
                label="Documento referenciado (factura original)"
                value={referenciaId}
                onChange={(e) => seleccionarReferencia(e.target.value === '' ? '' : Number(e.target.value))}
                helperText="La nota crédito/débito debe referenciar la factura que modifica"
              >
                {facturas.length === 0 && <MenuItem value="" disabled>No hay facturas emitidas</MenuItem>}
                {facturas.map((f) => (
                  <MenuItem key={f.id} value={f.id}>
                    {f.numeroCompleto} · {f.adquirenteNombre}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
          )}
          <Grid size={{ xs: 12 }}>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>Adquirente</Typography>
            <Grid container spacing={2}>
              <Grid size={{ xs: 12, md: 3 }}>
                <TextField
                  select
                  fullWidth
                  label="Tipo doc."
                  value={adquirente.tipoDocumento}
                  onChange={(e) => setAdquirente((a) => ({ ...a, tipoDocumento: e.target.value }))}
                >
                  {TIPOS_DOC.map((t) => (
                    <MenuItem key={t.codigo} value={t.codigo}>{t.label}</MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid size={{ xs: 12, md: 3 }}>
                <TextField
                  fullWidth
                  label="Número"
                  value={adquirente.numeroDocumento}
                  onChange={(e) => setAdquirente((a) => ({ ...a, numeroDocumento: e.target.value }))}
                />
              </Grid>
              <Grid size={{ xs: 12, md: 2 }}>
                <TextField
                  fullWidth
                  label="DV"
                  value={adquirente.digitoVerificacion}
                  onChange={(e) => setAdquirente((a) => ({ ...a, digitoVerificacion: e.target.value.replace(/\D/g, '') }))}
                />
              </Grid>
              <Grid size={{ xs: 12, md: 4 }}>
                <TextField
                  fullWidth
                  label="Nombre / razón social"
                  value={adquirente.nombre}
                  onChange={(e) => setAdquirente((a) => ({ ...a, nombre: e.target.value }))}
                />
              </Grid>
              <Grid size={{ xs: 12, md: 4 }}>
                <TextField
                  fullWidth
                  label="Email"
                  value={adquirente.email}
                  onChange={(e) => setAdquirente((a) => ({ ...a, email: e.target.value }))}
                />
              </Grid>
              <Grid size={{ xs: 12, md: 4 }}>
                <TextField
                  fullWidth
                  label="Dirección"
                  value={adquirente.direccion}
                  onChange={(e) => setAdquirente((a) => ({ ...a, direccion: e.target.value }))}
                />
              </Grid>
              <Grid size={{ xs: 12, md: 4 }}>
                <TextField
                  fullWidth
                  label="Municipio (código DANE)"
                  value={adquirente.municipio}
                  onChange={(e) => setAdquirente((a) => ({ ...a, municipio: e.target.value }))}
                />
              </Grid>
            </Grid>
          </Grid>

          <Grid size={{ xs: 12 }}>
            <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
              <Typography variant="subtitle2">Detalle</Typography>
              <Button size="small" startIcon={<AddIcon />} onClick={() => setLineas((p) => [...p, lineaVacia()])}>
                Agregar línea
              </Button>
            </Stack>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Descripción</TableCell>
                  <TableCell width={90}>Cant.</TableCell>
                  <TableCell width={130}>V. unitario</TableCell>
                  <TableCell width={90}>IVA %</TableCell>
                  <TableCell width={90}>INC %</TableCell>
                  <TableCell width={150}>UNSPSC</TableCell>
                  <TableCell width={120} align="right">Total</TableCell>
                  <TableCell width={48} />
                </TableRow>
              </TableHead>
              <TableBody>
                {lineas.map((l, idx) => (
                  <TableRow key={idx}>
                    <TableCell>
                      <TextField
                        size="small"
                        fullWidth
                        value={l.descripcion}
                        onChange={(e) => actualizarLinea(idx, { descripcion: e.target.value })}
                      />
                    </TableCell>
                    <TableCell>
                      <TextField
                        size="small"
                        type="number"
                        value={l.cantidad}
                        onChange={(e) => actualizarLinea(idx, { cantidad: Number(e.target.value) })}
                      />
                    </TableCell>
                    <TableCell>
                      <TextField
                        size="small"
                        type="number"
                        value={l.valorUnitario}
                        onChange={(e) => actualizarLinea(idx, { valorUnitario: Number(e.target.value) })}
                      />
                    </TableCell>
                    <TableCell>
                      <TextField
                        size="small"
                        type="number"
                        value={l.ivaPct}
                        onChange={(e) => actualizarLinea(idx, { ivaPct: Number(e.target.value) })}
                      />
                    </TableCell>
                    <TableCell>
                      <TextField
                        size="small"
                        type="number"
                        value={l.incPct}
                        onChange={(e) => actualizarLinea(idx, { incPct: Number(e.target.value) })}
                      />
                    </TableCell>
                    <TableCell>
                      <TextField
                        size="small"
                        value={l.codigoUnspsc ?? ''}
                        onChange={(e) => actualizarLinea(idx, { codigoUnspsc: e.target.value })}
                      />
                    </TableCell>
                    <TableCell align="right">{money(totalesLinea(l).total)}</TableCell>
                    <TableCell>
                      <IconButton
                        size="small"
                        aria-label="Eliminar línea"
                        disabled={lineas.length === 1}
                        onClick={() => setLineas((p) => p.filter((_, i) => i !== idx))}
                      >
                        <DeleteOutlineIcon fontSize="small" />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Grid>

          <Grid size={{ xs: 12 }}>
            <TextField
              fullWidth
              multiline
              minRows={2}
              label="Observaciones"
              value={observaciones}
              onChange={(e) => setObservaciones(e.target.value)}
            />
          </Grid>

          <Grid size={{ xs: 12 }}>
            <Divider sx={{ mb: 2 }} />
            <Stack
              direction="row"
              spacing={3}
              sx={{
                justifyContent: 'flex-end',
                alignItems: 'baseline',
                p: 2,
                borderRadius: 2,
                bgcolor: 'action.hover',
              }}
            >
              <Typography variant="body2" color="text.secondary">Subtotal: {money(totales.subtotal)}</Typography>
              <Typography variant="body2" color="text.secondary">Impuestos: {money(totales.impuestos)}</Typography>
              <Typography variant="h6">Total: {money(totales.total)}</Typography>
            </Stack>
          </Grid>

          {error && (
            <Grid size={{ xs: 12 }}>
              <Alert severity="error">{error}</Alert>
            </Grid>
          )}
          {errores.length > 0 && (
            <Grid size={{ xs: 12 }}>
              <Alert severity="warning">
                <Box component="ul" sx={{ m: 0, pl: 2 }}>
                  {errores.map((e, i) => (
                    <li key={i}>{e.mensaje}{e.campo ? ` (${e.campo})` : ''}</li>
                  ))}
                </Box>
              </Alert>
            </Grid>
          )}
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCerrar} disabled={enviando}>Cancelar</Button>
        <Button onClick={onValidar} disabled={enviando || validando} loading={validando}>Validar</Button>
        <Button variant="contained" onClick={onEmitir} disabled={enviando || validando} loading={enviando}>
          Emitir
        </Button>
      </DialogActions>
    </Dialog>
  )
}
