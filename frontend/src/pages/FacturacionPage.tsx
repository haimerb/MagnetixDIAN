import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TablePagination from '@mui/material/TablePagination'
import TableRow from '@mui/material/TableRow'
import AddIcon from '@mui/icons-material/Add'
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined'
import SettingsIcon from '@mui/icons-material/Settings'
import { api, type FacturaResumen, type Pagina, type ResultadoEmision } from '../api/client'
import { useEmpresaActiva } from '../features/auth/useEmpresaActiva'
import { useNotificaciones } from '../features/notificaciones/contexto'
import EmptyState from '../components/ui/EmptyState'
import EstadoChip from '../components/ui/EstadoChip'
import PageHeader from '../components/ui/PageHeader'
import SectionCard from '../components/ui/SectionCard'
import TableSkeleton from '../components/ui/TableSkeleton'
import FacturaEditor from '../components/facturacion/FacturaEditor'

const FILAS_POR_PAGINA = [10, 25, 50]

export default function FacturacionPage() {
  const navigate = useNavigate()
  const { notificar } = useNotificaciones()
  const { empresaId, cargando: cargandoEmpresa } = useEmpresaActiva()
  const [facturas, setFacturas] = useState<FacturaResumen[]>([])
  const [total, setTotal] = useState(0)
  const [pagina, setPagina] = useState(0)
  const [tamano, setTamano] = useState(FILAS_POR_PAGINA[0])
  const [recarga, setRecarga] = useState(0)
  const [editorAbierto, setEditorAbierto] = useState(false)
  const [cargando, setCargando] = useState(true)

  const cargar = useCallback(() => {
    if (empresaId == null) {
      setCargando(false)
      return
    }
    setCargando(true)
    api
      .get<Pagina<FacturaResumen>>(`/facturacion/${empresaId}/paginado`, {
        params: { page: pagina, size: tamano },
      })
      .then((res) => {
        setFacturas(res.data.contenido)
        setTotal(res.data.total)
      })
      .catch(() => undefined)
      .finally(() => setCargando(false))
  }, [empresaId, pagina, tamano])

  useEffect(() => {
    cargar()
  }, [cargar, recarga])

  const onEmitida = (r: ResultadoEmision) => {
    setEditorAbierto(false)
    notificar(`Documento ${r.numeroCompleto} emitido (${r.estado}).`, 'success')
    setPagina(0)
    setRecarga((n) => n + 1)
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
        {cargando ? (
          <TableSkeleton filas={5} columnas={6} label="Cargando documentos…" />
        ) : facturas.length === 0 ? (
          <EmptyState
            icon={<ReceiptLongOutlinedIcon />}
            title="Sin documentos emitidos"
            description="Aún no hay documentos electrónicos. Cree el primero con «Nueva factura»."
          />
        ) : (
          <>
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
            <TablePagination
              component="div"
              count={total}
              page={pagina}
              onPageChange={(_, nueva) => setPagina(nueva)}
              rowsPerPage={tamano}
              onRowsPerPageChange={(e) => {
                setTamano(parseInt(e.target.value, 10))
                setPagina(0)
              }}
              rowsPerPageOptions={FILAS_POR_PAGINA}
              labelRowsPerPage="Filas por página"
              labelDisplayedRows={({ from, to, count }) => `${from}–${to} de ${count}`}
            />
          </>
        )}
      </SectionCard>

      {empresaId != null && (
        <FacturaEditor
          empresaId={empresaId}
          abierto={editorAbierto}
          onCerrar={() => setEditorAbierto(false)}
          onEmitida={onEmitida}
        />
      )}
    </Box>
  )
}
