import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Paper from '@mui/material/Paper'
import Snackbar from '@mui/material/Snackbar'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Typography from '@mui/material/Typography'
import AddIcon from '@mui/icons-material/Add'
import SettingsIcon from '@mui/icons-material/Settings'
import { api, type FacturaResumen, type ResultadoEmision } from '../api/client'
import { useEmpresaActiva } from '../features/auth/useEmpresaActiva'
import EstadoChip from '../components/ui/EstadoChip'
import FacturaEditor from '../components/facturacion/FacturaEditor'

export default function FacturacionPage() {
  const navigate = useNavigate()
  const { empresaId, cargando: cargandoEmpresa } = useEmpresaActiva()
  const [facturas, setFacturas] = useState<FacturaResumen[]>([])
  const [editorAbierto, setEditorAbierto] = useState(false)
  const [aviso, setAviso] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const cargar = useCallback(() => {
    if (empresaId == null) return
    api
      .get<FacturaResumen[]>(`/facturacion/${empresaId}`)
      .then((res) => setFacturas(res.data))
      .catch(() => setError('No se pudieron cargar los documentos.'))
  }, [empresaId])

  useEffect(() => {
    cargar()
  }, [cargar])

  const onEmitida = (r: ResultadoEmision) => {
    setEditorAbierto(false)
    setAviso(`Documento ${r.numeroCompleto} emitido (${r.estado}).`)
    cargar()
  }

  const money = (v: number) =>
    v?.toLocaleString('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, alignItems: 'center', justifyContent: 'space-between' }}>
        <Typography variant="h4" component="h1">Facturación electrónica</Typography>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Button
            variant="outlined"
            startIcon={<SettingsIcon />}
            onClick={() => navigate('/facturacion/config')}
            disabled={cargandoEmpresa}
          >
            Configuración DIAN
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setEditorAbierto(true)}
            disabled={empresaId == null}
          >
            Nueva factura
          </Button>
        </Box>
      </Box>

      {empresaId == null && !cargandoEmpresa && (
        <Alert severity="info">No hay una empresa asociada para emitir documentos.</Alert>
      )}

      <Paper variant="outlined">
        <TableContainer>
          <Table size="small" aria-label="Documentos electrónicos">
            <TableHead>
              <TableRow>
                <TableCell>Número</TableCell>
                <TableCell>Tipo</TableCell>
                <TableCell>Fecha</TableCell>
                <TableCell>Adquirente</TableCell>
                <TableCell align="right">Total</TableCell>
                <TableCell>Estado</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {facturas.map((f) => (
                <TableRow
                  key={f.id}
                  hover
                  sx={{ cursor: 'pointer' }}
                  onClick={() => navigate(`/facturacion/${f.id}`)}
                >
                  <TableCell>{f.numeroCompleto}</TableCell>
                  <TableCell>{f.tipoDocumento}</TableCell>
                  <TableCell>{f.fechaEmision}</TableCell>
                  <TableCell>{f.adquirenteNombre}</TableCell>
                  <TableCell align="right">{money(f.totalPagar)}</TableCell>
                  <TableCell><EstadoChip estado={f.estado} /></TableCell>
                </TableRow>
              ))}
              {facturas.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6}>
                    <Typography variant="body2" color="text.secondary">
                      Aún no hay documentos electrónicos emitidos.
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      {error && <Alert severity="error">{error}</Alert>}

      {empresaId != null && (
        <FacturaEditor
          empresaId={empresaId}
          abierto={editorAbierto}
          onCerrar={() => setEditorAbierto(false)}
          onEmitida={onEmitida}
        />
      )}

      <Snackbar
        open={aviso != null}
        autoHideDuration={4000}
        onClose={() => setAviso(null)}
        message={aviso ?? ''}
      />
    </Box>
  )
}
