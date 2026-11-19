import { useEffect, useState } from 'react'
import Button from '@mui/material/Button'
import CircularProgress from '@mui/material/CircularProgress'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import IconButton from '@mui/material/IconButton'
import MenuItem from '@mui/material/MenuItem'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TablePagination from '@mui/material/TablePagination'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import DeleteIcon from '@mui/icons-material/Delete'
import BusinessOutlinedIcon from '@mui/icons-material/BusinessOutlined'
import { api, type EmpresaAdmin, type Pagina } from '../../api/client'
import ConfirmDialog from '../ui/ConfirmDialog'
import EmptyState from '../ui/EmptyState'
import SectionCard from '../ui/SectionCard'
import TableSkeleton from '../ui/TableSkeleton'

const FILAS_POR_PAGINA = [10, 25, 50]

const FORM_INICIAL = { nit: '', razonSocial: '', email: '', ciudad: '', regimen: 'ORDINARIO', tipoDocumento: 'NIT' }

export default function EmpresasPanel({ onError }: { onError: (mensaje: string) => void }) {
  const [empresas, setEmpresas] = useState<EmpresaAdmin[]>([])
  const [pagina, setPagina] = useState(0)
  const [tamano, setTamano] = useState(FILAS_POR_PAGINA[0])
  const [total, setTotal] = useState(0)
  const [cargando, setCargando] = useState(true)
  const [dialogAbierto, setDialogAbierto] = useState(false)
  const [guardando, setGuardando] = useState(false)
  const [eliminando, setEliminando] = useState<number | null>(null)
  const [confirmacion, setConfirmacion] = useState<number | null>(null)
  const [form, setForm] = useState(FORM_INICIAL)

  const cargar = () => {
    setCargando(true)
    return api
      .get<Pagina<EmpresaAdmin>>('/admin/empresas/paginado', { params: { page: pagina, size: tamano } })
      .then(r => { setEmpresas(r.data.contenido); setTotal(r.data.total) })
      .catch(() => onError('No se pudieron cargar las empresas.'))
      .finally(() => setCargando(false))
  }

  useEffect(() => { cargar() }, [pagina, tamano]) // eslint-disable-line react-hooks/exhaustive-deps

  const crear = async () => {
    setGuardando(true)
    try {
      await api.post('/admin/empresas', form, { skipGlobalError: true })
      setDialogAbierto(false)
      cargar()
    } catch (e: unknown) {
      onError((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? 'Error al crear empresa')
    } finally {
      setGuardando(false)
    }
  }

  const confirmarEliminar = async () => {
    if (confirmacion === null) return
    setEliminando(confirmacion)
    try {
      await api.delete(`/admin/empresas/${confirmacion}`, { skipGlobalError: true })
      cargar()
      setConfirmacion(null)
    } catch (e: unknown) {
      onError((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? 'No se pudo eliminar la empresa')
    } finally {
      setEliminando(null)
    }
  }

  return (
    <>
      <SectionCard
        title="Empresas"
        actions={
          <Button variant="contained" onClick={() => { setDialogAbierto(true); setForm(FORM_INICIAL) }}>
            Nueva empresa
          </Button>
        }
        padded={!cargando && empresas.length === 0}
      >
        {cargando ? (
          <TableSkeleton filas={6} columnas={7} label="Cargando empresas…" />
        ) : empresas.length === 0 ? (
          <EmptyState icon={<BusinessOutlinedIcon />} title="Sin empresas registradas" description="Cree la primera empresa para empezar a operar." />
        ) : (
          <TableContainer>
            <Table size="small" aria-label="Empresas">
              <TableHead>
                <TableRow>
                  <TableCell>ID</TableCell><TableCell>NIT</TableCell><TableCell>Razón social</TableCell><TableCell>Régimen</TableCell><TableCell>Email</TableCell><TableCell>Ciudad</TableCell><TableCell align="right">Acciones</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {empresas.map(e => (
                  <TableRow key={e.id} hover>
                    <TableCell>{e.id}</TableCell>
                    <TableCell>{e.nit}</TableCell>
                    <TableCell>{e.razonSocial}</TableCell>
                    <TableCell>{e.regimen}</TableCell>
                    <TableCell>{e.email ?? '—'}</TableCell>
                    <TableCell>{e.ciudad ?? '—'}</TableCell>
                    <TableCell align="right">
                      <IconButton size="small" color="error" onClick={() => setConfirmacion(e.id)} aria-label="Eliminar" disabled={eliminando === e.id}>
                        {eliminando === e.id ? <CircularProgress size={16} /> : <DeleteIcon fontSize="small" />}
                      </IconButton>
                    </TableCell>
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
            onRowsPerPageChange={e => { setTamano(parseInt(e.target.value, 10)); setPagina(0) }}
            rowsPerPageOptions={FILAS_POR_PAGINA}
            labelRowsPerPage="Filas por página"
            labelDisplayedRows={({ from, to, count }) => `${from}–${to} de ${count}`}
          />
        )}
      </SectionCard>

      <Dialog open={dialogAbierto} onClose={() => { if (!guardando) setDialogAbierto(false) }} maxWidth="sm" fullWidth>
        <DialogTitle>Nueva empresa</DialogTitle>
        <DialogContent sx={{ display: 'grid', gap: 2, pt: '16px !important' }}>
          <TextField label="NIT" required value={form.nit} onChange={e => setForm(p => ({ ...p, nit: e.target.value }))} />
          <TextField label="Razón social" required value={form.razonSocial} onChange={e => setForm(p => ({ ...p, razonSocial: e.target.value }))} />
          <TextField label="Email" value={form.email} onChange={e => setForm(p => ({ ...p, email: e.target.value }))} />
          <TextField label="Ciudad" value={form.ciudad} onChange={e => setForm(p => ({ ...p, ciudad: e.target.value }))} />
          <TextField label="Régimen" select value={form.regimen} onChange={e => setForm(p => ({ ...p, regimen: e.target.value }))}>
            <MenuItem value="ORDINARIO">Ordinario</MenuItem>
            <MenuItem value="SIMPLE">Simplificado</MenuItem>
          </TextField>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogAbierto(false)} disabled={guardando}>Cancelar</Button>
          <Button variant="contained" disabled={!form.nit || !form.razonSocial || guardando} loading={guardando} onClick={crear}>
            Crear
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        abierto={confirmacion !== null}
        titulo="Eliminar empresa"
        mensaje="¿Confirma que desea eliminar esta empresa? Esta acción no se puede deshacer."
        cargando={confirmacion !== null && eliminando === confirmacion}
        onConfirmar={confirmarEliminar}
        onCancelar={() => setConfirmacion(null)}
      />
    </>
  )
}
