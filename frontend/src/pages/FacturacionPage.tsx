import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Snackbar from '@mui/material/Snackbar'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import AddIcon from '@mui/icons-material/Add'
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined'
import SettingsIcon from '@mui/icons-material/Settings'
import { api, type FacturaResumen, type ResultadoEmision } from '../api/client'
import { useEmpresaActiva } from '../features/auth/useEmpresaActiva'
import EmptyState from '../components/ui/EmptyState'
import EstadoChip from '../components/ui/EstadoChip'
import PageHeader from '../components/ui/PageHeader'
import SectionCard from '../components/ui/SectionCard'
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
      <PageHeader
        overline="Facturación electrónica"
        title="Documentos electrónicos"
        subtitle="Emita y consulte facturas, notas crédito y documentos soporte."
        actions={
          <>
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
          </>
        }
      />

      {empresaId == null && !cargandoEmpresa && (
        <Alert severity="info">No hay una empresa asociada para emitir documentos.</Alert>
      )}

      <SectionCard padded={false}>
        {facturas.length === 0 ? (
          <EmptyState
            icon={<ReceiptLongOutlinedIcon />}
            title="Sin documentos emitidos"
            description="Aún no hay documentos electrónicos. Cree el primero con «Nueva factura»."
          />
        ) : (
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
                    <TableCell sx={{ fontWeight: 600 }}>{f.numeroCompleto}</TableCell>
                    <TableCell>{f.tipoDocumento}</TableCell>
                    <TableCell>{f.fechaEmision}</TableCell>
                    <TableCell>{f.adquirenteNombre}</TableCell>
                    <TableCell align="right">{money(f.totalPagar)}</TableCell>
                    <TableCell>
                      <EstadoChip estado={f.estado} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </SectionCard>

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
