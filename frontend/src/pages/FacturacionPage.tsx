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
import IconButton from '@mui/material/IconButton'
import Tooltip from '@mui/material/Tooltip'
import AddIcon from '@mui/icons-material/Add'
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined'
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined'
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined'
import SettingsIcon from '@mui/icons-material/Settings'
import ContentCopyOutlinedIcon from '@mui/icons-material/ContentCopyOutlined'
import {
  api,
  type FacturaDetalle,
  type FacturaRequest,
  type FacturaResumen,
  type Pagina,
  type ResultadoEmision,
} from '../api/client'
import { descargarArchivo } from '../api/descargas'
import { useEmpresaActiva } from '../features/auth/useEmpresaActiva'
import { useNotificaciones } from '../features/notificaciones/contexto'
import EmptyState from '../components/ui/EmptyState'
import EstadoChip from '../components/ui/EstadoChip'
import PageHeader from '../components/ui/PageHeader'
import SectionCard from '../components/ui/SectionCard'
import TableSkeleton from '../components/ui/TableSkeleton'
import FacturaEditor from '../components/facturacion/FacturaEditor'
import ImportarFacturasDialog from '../components/facturacion/ImportarFacturasDialog'

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
  const [duplicarDesde, setDuplicarDesde] = useState<FacturaRequest | null>(null)
  const [importadorAbierto, setImportadorAbierto] = useState(false)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const cargar = useCallback(() => {
    if (empresaId == null) {
      setCargando(false)
      return
    }
    setCargando(true)
    setError(null)
    api
      .get<Pagina<FacturaResumen>>(`/facturacion/${empresaId}/paginado`, {
        params: { page: pagina, size: tamano },
      })
      .then((res) => {
        setFacturas(res.data.contenido)
        setTotal(res.data.total)
      })
      .catch(() => setError('No se pudieron cargar los documentos electrónicos.'))
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

  const nuevaFactura = () => {
    setDuplicarDesde(null)
    setEditorAbierto(true)
  }

  const duplicar = async (f: FacturaResumen) => {
    if (empresaId == null) return
    setError(null)
    try {
      const { data } = await api.get<FacturaDetalle>(`/facturacion/${empresaId}/${f.id}`)
      setDuplicarDesde({
        tipoDocumento: data.tipoDocumento,
        adquirente: data.adquirente,
        lineas: data.lineas,
        observaciones: data.observaciones,
        formaPago: data.formaPago,
        medioPago: data.medioPago,
        tipoOperacion: '10',
        fechaVencimiento: null,
        cufeReferencia: null,
        numeroReferencia: null,
      })
      setEditorAbierto(true)
    } catch {
      setError('No se pudo cargar el documento para duplicar.')
    }
  }

  const exportar = async () => {
    if (empresaId == null) return
    setError(null)
    try {
      await descargarArchivo(`/api/facturacion/${empresaId}/export`, `facturas-${empresaId}.csv`)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo exportar el archivo.')
    }
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
              variant="outlined"
              startIcon={<DownloadOutlinedIcon />}
              onClick={exportar}
              disabled={empresaId == null || cargando}
            >
              Exportar CSV
            </Button>
            <Button
              variant="outlined"
              startIcon={<UploadFileOutlinedIcon />}
              onClick={() => setImportadorAbierto(true)}
              disabled={empresaId == null}
            >
              Importar
            </Button>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={nuevaFactura}
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

      {error && (
        <Alert
          severity="error"
          action={
            <Button color="inherit" size="small" onClick={() => setRecarga((n) => n + 1)}>
              Reintentar
            </Button>
          }
        >
          {error}
        </Alert>
      )}

      {(cargando || (error == null && empresaId != null)) && (
        <SectionCard padded={false}>
          {cargando ? (
            <TableSkeleton filas={5} columnas={7} label="Cargando documentos…" />
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
                    <TableCell align="right">Acciones</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {facturas.map((f) => (
                    <TableRow
                      key={f.id}
                      hover
                      tabIndex={0}
                      aria-label={`Abrir documento ${f.numeroCompleto}`}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault()
                          navigate(`/facturacion/${f.id}`)
                        }
                      }}
                      sx={{
                        cursor: 'pointer',
                        '&:focus-visible': { outline: '2px solid', outlineColor: 'primary.main', outlineOffset: -2 },
                      }}
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
                      <TableCell align="right">
                        {f.tipoDocumento === '01' && (
                          <Tooltip title="Duplicar como nueva factura">
                            <IconButton
                              size="small"
                              aria-label={`Duplicar ${f.numeroCompleto}`}
                              onClick={(e) => {
                                e.stopPropagation()
                                duplicar(f)
                              }}
                            >
                              <ContentCopyOutlinedIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}
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
      )}

      {empresaId != null && (
        <FacturaEditor
          empresaId={empresaId}
          abierto={editorAbierto}
          inicial={duplicarDesde}
          onCerrar={() => {
            setEditorAbierto(false)
            setDuplicarDesde(null)
          }}
          onEmitida={onEmitida}
        />
      )}

      {empresaId != null && (
        <ImportarFacturasDialog
          empresaId={empresaId}
          abierto={importadorAbierto}
          onCerrar={() => setImportadorAbierto(false)}
          onImportado={() => {
            notificar('Importación completada.', 'success')
            setPagina(0)
            setRecarga((n) => n + 1)
          }}
        />
      )}
    </Box>
  )
}
