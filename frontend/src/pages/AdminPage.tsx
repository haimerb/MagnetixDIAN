import { useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
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
import Tab from '@mui/material/Tab'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TablePagination from '@mui/material/TablePagination'
import TableRow from '@mui/material/TableRow'
import Tabs from '@mui/material/Tabs'
import TextField from '@mui/material/TextField'
import DeleteIcon from '@mui/icons-material/Delete'
import BusinessOutlinedIcon from '@mui/icons-material/BusinessOutlined'
import EventOutlinedIcon from '@mui/icons-material/EventOutlined'
import PeopleAltOutlinedIcon from '@mui/icons-material/PeopleAltOutlined'
import { api, type EmpresaAdmin, type Pagina, type UsuarioAdmin, type CalendarioAdmin } from '../api/client'
import ConfirmDialog from '../components/ui/ConfirmDialog'
import EmptyState from '../components/ui/EmptyState'
import PageHeader from '../components/ui/PageHeader'
import SectionCard from '../components/ui/SectionCard'
import TableSkeleton from '../components/ui/TableSkeleton'

const ROLES = ['ROLE_ADMIN', 'ROLE_CONTADOR', 'ROLE_CLIENTE']
const TIPOS_REPORTE = ['GRAN_CONTRIBUYENTE', 'PERSONA_JURIDICA', 'PERSONA_NATURAL']
const FILAS_POR_PAGINA = [10, 25, 50]

type TabIdx = 'empresas' | 'usuarios' | 'calendario'

export default function AdminPage() {
  const [tab, setTab] = useState<TabIdx>('empresas')
  const [empresas, setEmpresas] = useState<EmpresaAdmin[]>([])
  const [usuarios, setUsuarios] = useState<UsuarioAdmin[]>([])
  const [calendario, setCalendario] = useState<CalendarioAdmin[]>([])
  const [anioCalendario, setAnioCalendario] = useState(2025)
  const [error, setError] = useState<string | null>(null)
  const [dialog, setDialog] = useState<'empresa' | 'usuario' | 'calendario' | null>(null)
  const [paginaEmpresas, setPaginaEmpresas] = useState(0)
  const [tamanoEmpresas, setTamanoEmpresas] = useState(FILAS_POR_PAGINA[0])
  const [totalEmpresas, setTotalEmpresas] = useState(0)
  const [paginaUsuarios, setPaginaUsuarios] = useState(0)
  const [tamanoUsuarios, setTamanoUsuarios] = useState(FILAS_POR_PAGINA[0])
  const [totalUsuarios, setTotalUsuarios] = useState(0)
  const [cargandoEmpresas, setCargandoEmpresas] = useState(true)
  const [cargandoUsuarios, setCargandoUsuarios] = useState(true)
  const [cargandoCalendario, setCargandoCalendario] = useState(false)
  const [guardando, setGuardando] = useState<'empresa' | 'usuario' | 'calendario' | null>(null)
  const [eliminando, setEliminando] = useState<number | null>(null)
  const [confirmacion, setConfirmacion] = useState<{ tipo: 'empresa' | 'calendario'; id: number } | null>(null)
  const [cambiandoEstado, setCambiandoEstado] = useState<number | null>(null)

  const [formEmpresa, setFormEmpresa] = useState({ nit: '', razonSocial: '', email: '', ciudad: '', regimen: 'ORDINARIO', tipoDocumento: 'NIT' })
  const [formUsuario, setFormUsuario] = useState({ username: '', password: '', email: '', nombre: '', empresaId: '', roles: 'ROLE_CONTADOR' })
  const [formCalendario, setFormCalendario] = useState({ anioPresentacion: 2026, tipoReporte: 'PERSONA_JURIDICA', rangoNitIni: '', rangoNitFin: '', fechaLimite: '' })

  const cargarEmpresas = () => {
    setCargandoEmpresas(true)
    return api
      .get<Pagina<EmpresaAdmin>>('/admin/empresas/paginado', { params: { page: paginaEmpresas, size: tamanoEmpresas } })
      .then(r => { setEmpresas(r.data.contenido); setTotalEmpresas(r.data.total) })
      .catch(() => undefined)
      .finally(() => setCargandoEmpresas(false))
  }
  const cargarUsuarios = () => {
    setCargandoUsuarios(true)
    return api
      .get<Pagina<UsuarioAdmin>>('/admin/usuarios/paginado', { params: { page: paginaUsuarios, size: tamanoUsuarios } })
      .then(r => { setUsuarios(r.data.contenido); setTotalUsuarios(r.data.total) })
      .catch(() => undefined)
      .finally(() => setCargandoUsuarios(false))
  }
  const cargarCalendario = () => {
    setCargandoCalendario(true)
    return api
      .get<CalendarioAdmin[]>(`/admin/calendario?anioGravable=${anioCalendario}`)
      .then(r => setCalendario(r.data))
      .catch(() => undefined)
      .finally(() => setCargandoCalendario(false))
  }

  useEffect(() => { cargarEmpresas() }, [paginaEmpresas, tamanoEmpresas]) // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => { cargarUsuarios() }, [paginaUsuarios, tamanoUsuarios]) // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => { if (tab === 'calendario') cargarCalendario() }, [tab, anioCalendario]) // eslint-disable-line react-hooks/exhaustive-deps

  const crearEmpresa = async () => {
    setGuardando('empresa')
    try {
      await api.post('/admin/empresas', formEmpresa, { skipGlobalError: true })
      setDialog(null)
      cargarEmpresas()
    } catch (e: unknown) {
      setError((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? 'Error al crear empresa')
    } finally {
      setGuardando(null)
    }
  }

  const solicitarEliminar = (tipo: 'empresa' | 'calendario', id: number) => {
    setConfirmacion({ tipo, id })
  }

  const confirmarEliminar = async () => {
    if (!confirmacion) return
    const { tipo, id } = confirmacion
    setEliminando(id)
    try {
      if (tipo === 'empresa') {
        await api.delete(`/admin/empresas/${id}`, { skipGlobalError: true })
        cargarEmpresas()
      } else {
        await api.delete(`/admin/calendario/${id}`, { skipGlobalError: true })
        cargarCalendario()
      }
      setConfirmacion(null)
    } catch (e: unknown) {
      const porDefecto = tipo === 'empresa' ? 'No se pudo eliminar la empresa' : 'No se pudo eliminar la fecha'
      setError((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? porDefecto)
    } finally {
      setEliminando(null)
    }
  }

  const crearUsuario = async () => {
    setGuardando('usuario')
    try {
      await api.post('/admin/usuarios', {
        ...formUsuario,
        roles: [formUsuario.roles],
        empresaId: formUsuario.empresaId ? Number(formUsuario.empresaId) : null,
        enabled: true
      }, { skipGlobalError: true })
      setDialog(null)
      cargarUsuarios()
    } catch (e: unknown) {
      setError((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? 'Error al crear usuario')
    } finally {
      setGuardando(null)
    }
  }

  const toggleEstado = async (id: number, actual: boolean) => {
    setCambiandoEstado(id)
    try {
      await api.put(`/admin/usuarios/${id}/estado`, { enabled: !actual })
      cargarUsuarios()
    } finally {
      setCambiandoEstado(null)
    }
  }

  const crearCalendario = async () => {
    setGuardando('calendario')
    try {
      await api.post('/admin/calendario', {
        anioGravable: anioCalendario,
        anioPresentacion: Number(formCalendario.anioPresentacion),
        tipoReporte: formCalendario.tipoReporte,
        rangoNitIni: formCalendario.rangoNitIni || null,
        rangoNitFin: formCalendario.rangoNitFin || null,
        fechaLimite: formCalendario.fechaLimite
      }, { skipGlobalError: true })
      setDialog(null)
      cargarCalendario()
    } catch (e: unknown) {
      setError((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? 'Error al crear fecha')
    } finally {
      setGuardando(null)
    }
  }

  const eliminarCalendario = (id: number) => {
    solicitarEliminar('calendario', id)
  }

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="Configuración"
        title="Administración"
        subtitle="Gestione empresas, usuarios y el calendario tributario usado para los recordatorios."
      />

      {error && <Alert severity="error" onClose={() => setError(null)}>{error}</Alert>}

      <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="scrollable" allowScrollButtonsMobile>
        <Tab label="Empresas" value="empresas" />
        <Tab label="Usuarios" value="usuarios" />
        <Tab label="Calendario DIAN" value="calendario" />
      </Tabs>

      {tab === 'empresas' && (
        <SectionCard
          title="Empresas"
          actions={
            <Button variant="contained" onClick={() => { setDialog('empresa'); setFormEmpresa({ nit: '', razonSocial: '', email: '', ciudad: '', regimen: 'ORDINARIO', tipoDocumento: 'NIT' }) }}>
              Nueva empresa
            </Button>
          }
          padded={!cargandoEmpresas && empresas.length === 0}
        >
          {cargandoEmpresas ? (
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
                        <IconButton size="small" color="error" onClick={() => solicitarEliminar('empresa', e.id)} aria-label="Eliminar" disabled={eliminando === e.id}>
                          {eliminando === e.id ? <CircularProgress size={16} /> : <DeleteIcon fontSize="small" />}
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
          {totalEmpresas > 0 && (
            <TablePagination
              component="div"
              count={totalEmpresas}
              page={paginaEmpresas}
              onPageChange={(_, nueva) => setPaginaEmpresas(nueva)}
              rowsPerPage={tamanoEmpresas}
              onRowsPerPageChange={e => { setTamanoEmpresas(parseInt(e.target.value, 10)); setPaginaEmpresas(0) }}
              rowsPerPageOptions={FILAS_POR_PAGINA}
              labelRowsPerPage="Filas por página"
              labelDisplayedRows={({ from, to, count }) => `${from}–${to} de ${count}`}
            />
          )}
        </SectionCard>
      )}

      {tab === 'usuarios' && (
        <SectionCard
          title="Usuarios"
          actions={
            <Button variant="contained" onClick={() => { setDialog('usuario'); setFormUsuario({ username: '', password: '', email: '', nombre: '', empresaId: '', roles: 'ROLE_CONTADOR' }) }}>
              Nuevo usuario
            </Button>
          }
          padded={!cargandoUsuarios && usuarios.length === 0}
        >
          {cargandoUsuarios ? (
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
                      <TableCell align="right">—</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
          {totalUsuarios > 0 && (
            <TablePagination
              component="div"
              count={totalUsuarios}
              page={paginaUsuarios}
              onPageChange={(_, nueva) => setPaginaUsuarios(nueva)}
              rowsPerPage={tamanoUsuarios}
              onRowsPerPageChange={e => { setTamanoUsuarios(parseInt(e.target.value, 10)); setPaginaUsuarios(0) }}
              rowsPerPageOptions={FILAS_POR_PAGINA}
              labelRowsPerPage="Filas por página"
              labelDisplayedRows={({ from, to, count }) => `${from}–${to} de ${count}`}
            />
          )}
        </SectionCard>
      )}

      {tab === 'calendario' && (
        <SectionCard
          title="Calendario DIAN"
          actions={
            <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
              <TextField label="Año gravable" type="number" size="small" value={anioCalendario} onChange={e => setAnioCalendario(Number(e.target.value))} sx={{ width: 140 }} />
              <Button variant="contained" onClick={() => { setDialog('calendario'); setFormCalendario({ anioPresentacion: anioCalendario + 1, tipoReporte: 'PERSONA_JURIDICA', rangoNitIni: '', rangoNitFin: '', fechaLimite: '' }) }}>
                Nueva fecha
              </Button>
            </Stack>
          }
          padded={!cargandoCalendario && calendario.length === 0}
        >
          {cargandoCalendario ? (
            <TableSkeleton filas={5} columnas={5} label="Cargando calendario…" />
          ) : calendario.length === 0 ? (
            <EmptyState icon={<EventOutlinedIcon />} title="Sin fechas para este año" description="Agregue las fechas límite del calendario tributario." />
          ) : (
            <TableContainer>
              <Table size="small" aria-label="Calendario DIAN">
                <TableHead>
                  <TableRow>
                    <TableCell>Tipo reporte</TableCell><TableCell>Rango NIT</TableCell><TableCell>Fecha límite</TableCell><TableCell>Año presentación</TableCell><TableCell align="right">Acciones</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {calendario.map(c => (
                    <TableRow key={c.id} hover>
                      <TableCell>{c.tipoReporte.replace('_', ' ')}</TableCell>
                      <TableCell>{c.rangoNitIni ?? '*'} – {c.rangoNitFin ?? '*'}</TableCell>
                      <TableCell>{c.fechaLimite}</TableCell>
                      <TableCell>{c.anioPresentacion}</TableCell>
                      <TableCell align="right">
                        <IconButton size="small" color="error" onClick={() => eliminarCalendario(c.id)} aria-label="Eliminar" disabled={eliminando === c.id}>
                          {eliminando === c.id ? <CircularProgress size={16} /> : <DeleteIcon fontSize="small" />}
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </SectionCard>
      )}

      <Dialog open={dialog === 'empresa'} onClose={() => { if (!guardando) setDialog(null) }} maxWidth="sm" fullWidth>
        <DialogTitle>Nueva empresa</DialogTitle>
        <DialogContent sx={{ display: 'grid', gap: 2, pt: '16px !important' }}>
          <TextField label="NIT" required value={formEmpresa.nit} onChange={e => setFormEmpresa(p => ({ ...p, nit: e.target.value }))} />
          <TextField label="Razón social" required value={formEmpresa.razonSocial} onChange={e => setFormEmpresa(p => ({ ...p, razonSocial: e.target.value }))} />
          <TextField label="Email" value={formEmpresa.email} onChange={e => setFormEmpresa(p => ({ ...p, email: e.target.value }))} />
          <TextField label="Ciudad" value={formEmpresa.ciudad} onChange={e => setFormEmpresa(p => ({ ...p, ciudad: e.target.value }))} />
          <TextField label="Régimen" select value={formEmpresa.regimen} onChange={e => setFormEmpresa(p => ({ ...p, regimen: e.target.value }))}>
            <MenuItem value="ORDINARIO">Ordinario</MenuItem>
            <MenuItem value="SIMPLE">Simplificado</MenuItem>
          </TextField>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialog(null)} disabled={guardando !== null}>Cancelar</Button>
          <Button
            variant="contained"
            disabled={!formEmpresa.nit || !formEmpresa.razonSocial || guardando !== null}
            loading={guardando === 'empresa'}
            onClick={crearEmpresa}
          >
            Crear
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={dialog === 'usuario'} onClose={() => { if (!guardando) setDialog(null) }} maxWidth="sm" fullWidth>
        <DialogTitle>Nuevo usuario</DialogTitle>
        <DialogContent sx={{ display: 'grid', gap: 2, pt: '16px !important' }}>
          <TextField label="Username" required value={formUsuario.username} onChange={e => setFormUsuario(p => ({ ...p, username: e.target.value }))} />
          <TextField label="Contraseña" type="password" required inputProps={{ minLength: 6 }} value={formUsuario.password} onChange={e => setFormUsuario(p => ({ ...p, password: e.target.value }))} />
          <TextField label="Email" type="email" value={formUsuario.email} onChange={e => setFormUsuario(p => ({ ...p, email: e.target.value }))} />
          <TextField label="Nombre completo" value={formUsuario.nombre} onChange={e => setFormUsuario(p => ({ ...p, nombre: e.target.value }))} />
          <TextField label="Rol" select value={formUsuario.roles} onChange={e => setFormUsuario(p => ({ ...p, roles: e.target.value }))}>
            {ROLES.map(r => <MenuItem key={r} value={r}>{r.replace('ROLE_', '')}</MenuItem>)}
          </TextField>
          <TextField label="Empresa (ID)" type="number" value={formUsuario.empresaId} onChange={e => setFormUsuario(p => ({ ...p, empresaId: e.target.value }))} helperText="Dejar vacío si no aplica" />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialog(null)} disabled={guardando !== null}>Cancelar</Button>
          <Button
            variant="contained"
            disabled={!formUsuario.username || formUsuario.password.length < 6 || guardando !== null}
            loading={guardando === 'usuario'}
            onClick={crearUsuario}
          >
            Crear
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={dialog === 'calendario'} onClose={() => { if (!guardando) setDialog(null) }} maxWidth="sm" fullWidth>
        <DialogTitle>Nueva fecha DIAN</DialogTitle>
        <DialogContent sx={{ display: 'grid', gap: 2, pt: '16px !important' }}>
          <TextField label="Año presentación" type="number" required value={formCalendario.anioPresentacion} onChange={e => setFormCalendario(p => ({ ...p, anioPresentacion: Number(e.target.value) }))} />
          <TextField label="Tipo reporte" select required value={formCalendario.tipoReporte} onChange={e => setFormCalendario(p => ({ ...p, tipoReporte: e.target.value }))}>
            {TIPOS_REPORTE.map(t => <MenuItem key={t} value={t}>{t.replace(/_/g, ' ')}</MenuItem>)}
          </TextField>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <TextField label="Rango NIT inicio" value={formCalendario.rangoNitIni} onChange={e => setFormCalendario(p => ({ ...p, rangoNitIni: e.target.value }))} inputProps={{ maxLength: 2 }} sx={{ flex: 1 }} />
            <TextField label="Rango NIT fin" value={formCalendario.rangoNitFin} onChange={e => setFormCalendario(p => ({ ...p, rangoNitFin: e.target.value }))} inputProps={{ maxLength: 2 }} sx={{ flex: 1 }} />
          </Box>
          <TextField label="Fecha límite" type="date" required value={formCalendario.fechaLimite} onChange={e => setFormCalendario(p => ({ ...p, fechaLimite: e.target.value }))} InputLabelProps={{ shrink: true }} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialog(null)} disabled={guardando !== null}>Cancelar</Button>
          <Button
            variant="contained"
            disabled={!formCalendario.fechaLimite || guardando !== null}
            loading={guardando === 'calendario'}
            onClick={crearCalendario}
          >
            Crear
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        abierto={confirmacion !== null}
        titulo={confirmacion?.tipo === 'empresa' ? 'Eliminar empresa' : 'Eliminar fecha del calendario'}
        mensaje={
          confirmacion?.tipo === 'empresa'
            ? '¿Confirma que desea eliminar esta empresa? Esta acción no se puede deshacer.'
            : '¿Confirma que desea eliminar esta fecha del calendario? Esta acción no se puede deshacer.'
        }
        cargando={confirmacion !== null && eliminando === confirmacion.id}
        onConfirmar={confirmarEliminar}
        onCancelar={() => setConfirmacion(null)}
      />
    </Box>
  )
}
