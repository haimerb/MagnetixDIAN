import { useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import CircularProgress from '@mui/material/CircularProgress'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import IconButton from '@mui/material/IconButton'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TablePagination from '@mui/material/TablePagination'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import Tooltip from '@mui/material/Tooltip'
import LinkOutlinedIcon from '@mui/icons-material/LinkOutlined'
import PeopleAltOutlinedIcon from '@mui/icons-material/PeopleAltOutlined'
import PersonAddAlt1OutlinedIcon from '@mui/icons-material/PersonAddAlt1Outlined'
import { api, generarEnlaceAcceso, type EnlaceAcceso, type Pagina, type UsuarioAdmin } from '../../api/client'
import InvitacionUsuarioDialog from './InvitacionUsuarioDialog'
import EmptyState from '../ui/EmptyState'
import SectionCard from '../ui/SectionCard'
import TableSkeleton from '../ui/TableSkeleton'

const ROLES = ['ROLE_ADMIN', 'ROLE_CONTADOR', 'ROLE_CLIENTE']
const FILAS_POR_PAGINA = [10, 25, 50]

const FORM_INICIAL = { username: '', password: '', email: '', nombre: '', empresaId: '', roles: 'ROLE_CONTADOR' }

export default function UsuariosPanel({ onError }: { onError: (mensaje: string) => void }) {
  const [usuarios, setUsuarios] = useState<UsuarioAdmin[]>([])
  const [pagina, setPagina] = useState(0)
  const [tamano, setTamano] = useState(FILAS_POR_PAGINA[0])
  const [total, setTotal] = useState(0)
  const [cargando, setCargando] = useState(true)
  const [dialogAbierto, setDialogAbierto] = useState(false)
  const [invitacionAbierta, setInvitacionAbierta] = useState(false)
  const [guardando, setGuardando] = useState(false)
  const [cambiandoEstado, setCambiandoEstado] = useState<number | null>(null)
  const [generandoEnlace, setGenerandoEnlace] = useState<number | null>(null)
  const [enlaceAcceso, setEnlaceAcceso] = useState<EnlaceAcceso | null>(null)
  const [copiado, setCopiado] = useState(false)
  const [form, setForm] = useState(FORM_INICIAL)

  const cargar = () => {
    setCargando(true)
    return api
      .get<Pagina<UsuarioAdmin>>('/admin/usuarios/paginado', { params: { page: pagina, size: tamano } })
      .then(r => { setUsuarios(r.data.contenido); setTotal(r.data.total) })
      .catch(() => onError('No se pudieron cargar los usuarios.'))
      .finally(() => setCargando(false))
  }

  useEffect(() => { cargar() }, [pagina, tamano]) // eslint-disable-line react-hooks/exhaustive-deps

  const crear = async () => {
    setGuardando(true)
    try {
      await api.post('/admin/usuarios', {
        ...form,
        roles: [form.roles],
        empresaId: form.empresaId ? Number(form.empresaId) : null,
        enabled: true,
      }, { skipGlobalError: true })
      setDialogAbierto(false)
      cargar()
    } catch (e: unknown) {
      onError((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? 'Error al crear usuario')
    } finally {
      setGuardando(false)
    }
  }

  const toggleEstado = async (id: number, actual: boolean) => {
    setCambiandoEstado(id)
    try {
      await api.put(`/admin/usuarios/${id}/estado`, { enabled: !actual })
      cargar()
    } finally {
      setCambiandoEstado(null)
    }
  }

  const abrirEnlaceAcceso = async (id: number) => {
    setGenerandoEnlace(id)
    try {
      setEnlaceAcceso(await generarEnlaceAcceso(id))
      setCopiado(false)
    } catch {
      onError('No se pudo generar el enlace de acceso.')
    } finally {
      setGenerandoEnlace(null)
    }
  }

  const copiarEnlace = async () => {
    if (!enlaceAcceso) return
    try {
      await navigator.clipboard.writeText(enlaceAcceso.enlace)
      setCopiado(true)
    } catch {
      onError('No se pudo copiar automáticamente; seleccione el enlace manualmente.')
    }
  }

  return (
    <>
      <SectionCard
        title="Usuarios"
        actions={
          <Stack direction="row" spacing={1}>
            <Button variant="outlined" startIcon={<PersonAddAlt1OutlinedIcon />} onClick={() => setInvitacionAbierta(true)}>
              Invitar
            </Button>
            <Button variant="contained" onClick={() => { setDialogAbierto(true); setForm(FORM_INICIAL) }}>
              Nuevo usuario
            </Button>
          </Stack>
        }
        padded={!cargando && usuarios.length === 0}
      >
        {cargando ? (
          <TableSkeleton filas={6} columnas={7} label="Cargando usuarios…" />
        ) : usuarios.length === 0 ? (
          <EmptyState icon={<PeopleAltOutlinedIcon />} title="Sin usuarios" description="Cree usuarios y asígneles un rol y empresa." />
        ) : (
          <TableContainer>
            <Table size="small" aria-label="Usuarios">
              <TableHead>
                <TableRow>
                  <TableCell>ID</TableCell><TableCell>Usuario</TableCell><TableCell>Email</TableCell><TableCell>Nombre</TableCell><TableCell>Roles</TableCell><TableCell>Estado</TableCell><TableCell align="right">Acciones</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {usuarios.map(u => (
                  <TableRow key={u.id} hover>
                    <TableCell>{u.id}</TableCell>
                    <TableCell>{u.username}</TableCell>
                    <TableCell>{u.email}</TableCell>
                    <TableCell>{u.nombre}</TableCell>
                    <TableCell>{u.roles.map(r => <Chip key={r} size="small" label={r.replace('ROLE_', '')} sx={{ mr: 0.5 }} />)}</TableCell>
                    <TableCell>
                      <Chip size="small" color={u.enabled ? 'success' : 'default'} label={u.enabled ? 'Activo' : 'Inactivo'}
                            onClick={() => toggleEstado(u.id, u.enabled)} clickable disabled={cambiandoEstado === u.id} sx={{ cursor: 'pointer' }} />
                    </TableCell>
                    <TableCell align="right">
                      <Tooltip title="Generar enlace de acceso">
                        <span>
                          <IconButton size="small" onClick={() => abrirEnlaceAcceso(u.id)} aria-label={`Enlace de acceso para ${u.username}`} disabled={generandoEnlace === u.id}>
                            {generandoEnlace === u.id ? <CircularProgress size={16} /> : <LinkOutlinedIcon fontSize="small" />}
                          </IconButton>
                        </span>
                      </Tooltip>
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
        <DialogTitle>Nuevo usuario</DialogTitle>
        <DialogContent sx={{ display: 'grid', gap: 2, pt: '16px !important' }}>
          <TextField label="Username" required value={form.username} onChange={e => setForm(p => ({ ...p, username: e.target.value }))} />
          <TextField label="Contraseña" type="password" required inputProps={{ minLength: 6 }} value={form.password} onChange={e => setForm(p => ({ ...p, password: e.target.value }))} />
          <TextField label="Email" type="email" value={form.email} onChange={e => setForm(p => ({ ...p, email: e.target.value }))} />
          <TextField label="Nombre completo" value={form.nombre} onChange={e => setForm(p => ({ ...p, nombre: e.target.value }))} />
          <TextField label="Rol" select value={form.roles} onChange={e => setForm(p => ({ ...p, roles: e.target.value }))}>
            {ROLES.map(r => <MenuItem key={r} value={r}>{r.replace('ROLE_', '')}</MenuItem>)}
          </TextField>
          <TextField label="Empresa (ID)" type="number" value={form.empresaId} onChange={e => setForm(p => ({ ...p, empresaId: e.target.value }))} helperText="Dejar vacío si no aplica" />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogAbierto(false)} disabled={guardando}>Cancelar</Button>
          <Button variant="contained" disabled={!form.username || form.password.length < 6 || guardando} loading={guardando} onClick={crear}>
            Crear
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={Boolean(enlaceAcceso)} onClose={() => setEnlaceAcceso(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Enlace de {enlaceAcceso?.tipo === 'ACTIVACION' ? 'activación' : 'recuperación'}</DialogTitle>
        <DialogContent sx={{ display: 'grid', gap: 2, pt: '16px !important' }}>
          <Alert severity="info">
            Comparta este enlace con <strong>{enlaceAcceso?.usuario}</strong> por un canal seguro. Vence en
            {enlaceAcceso?.tipo === 'ACTIVACION' ? ' 7 días' : ' 30 minutos'} y solo puede usarse una vez.
          </Alert>
          <TextField label="Enlace" value={enlaceAcceso?.enlace ?? ''} fullWidth multiline minRows={2} slotProps={{ input: { readOnly: true } }} onFocus={e => e.target.select()} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEnlaceAcceso(null)}>Cerrar</Button>
          <Button variant="contained" onClick={copiarEnlace}>{copiado ? 'Copiado' : 'Copiar enlace'}</Button>
        </DialogActions>
      </Dialog>

      <InvitacionUsuarioDialog abierto={invitacionAbierta} onCerrar={() => setInvitacionAbierta(false)} onInvitado={cargar} />
    </>
  )
}
