import { useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TablePagination from '@mui/material/TablePagination'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import HistoryOutlinedIcon from '@mui/icons-material/HistoryOutlined'
import { api, type EventoAuditoria, type Pagina } from '../../api/client'
import EmptyState from '../ui/EmptyState'
import SectionCard from '../ui/SectionCard'
import TableSkeleton from '../ui/TableSkeleton'

const FILAS = [10, 25, 50]

export default function AuditoriaPanel() {
  const [eventos, setEventos] = useState<EventoAuditoria[]>([])
  const [total, setTotal] = useState(0)
  const [pagina, setPagina] = useState(0)
  const [tamano, setTamano] = useState(FILAS[0])
  const [accion, setAccion] = useState('')
  const [usuario, setUsuario] = useState('')
  const [filtros, setFiltros] = useState({ accion: '', usuario: '' })
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let activo = true
    setCargando(true)
    api
      .get<Pagina<EventoAuditoria>>('/admin/auditoria', {
        params: {
          page: pagina,
          size: tamano,
          accion: filtros.accion || undefined,
          usuario: filtros.usuario || undefined,
        },
      })
      .then((r) => {
        if (!activo) return
        setEventos(r.data.contenido)
        setTotal(r.data.total)
      })
      .catch(() => activo && setError('No se pudo cargar la auditoría.'))
      .finally(() => activo && setCargando(false))
    return () => {
      activo = false
    }
  }, [pagina, tamano, filtros])

  const buscar = () => {
    setPagina(0)
    setFiltros({ accion: accion.trim(), usuario: usuario.trim() })
  }

  return (
    <SectionCard title="Auditoría" padded={!cargando && eventos.length === 0}>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ p: 2, alignItems: { sm: 'center' } }}>
        <TextField label="Acción" size="small" value={accion} onChange={(e) => setAccion(e.target.value)} />
        <TextField label="Usuario" size="small" value={usuario} onChange={(e) => setUsuario(e.target.value)} />
        <Button variant="outlined" onClick={buscar}>Filtrar</Button>
      </Stack>
      {error && <Alert severity="error" sx={{ m: 2 }} onClose={() => setError(null)}>{error}</Alert>}
      {cargando ? (
        <TableSkeleton filas={8} columnas={5} label="Cargando auditoría…" />
      ) : eventos.length === 0 && !error ? (
        <Box sx={{ p: 2 }}>
          <EmptyState
            icon={<HistoryOutlinedIcon />}
            title="Sin eventos registrados"
            description="Las acciones sensibles aparecerán aquí a medida que se realicen."
          />
        </Box>
      ) : (
        <TableContainer>
          <Table size="small" aria-label="Eventos de auditoría">
            <TableHead>
              <TableRow>
                <TableCell>Fecha</TableCell>
                <TableCell>Usuario</TableCell>
                <TableCell>Acción</TableCell>
                <TableCell>Entidad</TableCell>
                <TableCell>Detalle</TableCell>
                <TableCell>IP</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {eventos.map((e) => (
                <TableRow key={e.id} hover>
                  <TableCell>{new Date(e.createdAt).toLocaleString('es-CO')}</TableCell>
                  <TableCell>{e.usuario ?? '—'}</TableCell>
                  <TableCell>{e.accion}</TableCell>
                  <TableCell>{e.entidad ? `${e.entidad}${e.entidadId ? ` #${e.entidadId}` : ''}` : '—'}</TableCell>
                  <TableCell>{e.detalle ?? '—'}</TableCell>
                  <TableCell>{e.ip ?? '—'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
      {total > 0 && (
        <TablePagination
          component="div"
          count={total}
          page={pagina}
          onPageChange={(_, nueva) => setPagina(nueva)}
          rowsPerPage={tamano}
          onRowsPerPageChange={(e) => { setTamano(parseInt(e.target.value, 10)); setPagina(0) }}
          rowsPerPageOptions={FILAS}
          labelRowsPerPage="Filas por página"
          labelDisplayedRows={({ from, to, count }) => `${from}–${to} de ${count}`}
        />
      )}
    </SectionCard>
  )
}
