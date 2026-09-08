import { useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
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
import TableRow from '@mui/material/TableRow'
import Tabs from '@mui/material/Tabs'
import Tab from '@mui/material/Tab'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import DeleteIcon from '@mui/icons-material/Delete'
import { api, type EmpresaAdmin, type UsuarioAdmin, type CalendarioAdmin } from '../api/client'

const ROLES = ['ROLE_ADMIN', 'ROLE_CONTADOR', 'ROLE_CLIENTE']
const TIPOS_REPORTE = ['GRAN_CONTRIBUYENTE', 'PERSONA_JURIDICA', 'PERSONA_NATURAL']

type TabIdx = 'empresas' | 'usuarios' | 'calendario'

export default function AdminPage() {
  const [tab, setTab] = useState<TabIdx>('empresas')
  const [empresas, setEmpresas] = useState<EmpresaAdmin[]>([])
  const [usuarios, setUsuarios] = useState<UsuarioAdmin[]>([])
  const [calendario, setCalendario] = useState<CalendarioAdmin[]>([])
  const [anioCalendario, setAnioCalendario] = useState(2025)
  const [error, setError] = useState<string | null>(null)
  const [dialog, setDialog] = useState<'empresa' | 'usuario' | 'calendario' | null>(null)

  const [formEmpresa, setFormEmpresa] = useState({ nit: '', razonSocial: '', email: '', ciudad: '', regimen: 'ORDINARIO', tipoDocumento: 'NIT' })
  const [formUsuario, setFormUsuario] = useState({ username: '', password: '', email: '', nombre: '', empresaId: '', roles: 'ROLE_CONTADOR' })
  const [formCalendario, setFormCalendario] = useState({ anioPresentacion: 2026, tipoReporte: 'PERSONA_JURIDICA', rangoNitIni: '', rangoNitFin: '', fechaLimite: '' })

  const cargarEmpresas = () => api.get<EmpresaAdmin[]>('/admin/empresas').then(r => setEmpresas(r.data)).catch(() => undefined)
  const cargarUsuarios = () => api.get<UsuarioAdmin[]>('/admin/usuarios').then(r => setUsuarios(r.data)).catch(() => undefined)
  const cargarCalendario = () => api.get<CalendarioAdmin[]>(`/admin/calendario?anioGravable=${anioCalendario}`).then(r => setCalendario(r.data)).catch(() => undefined)

  useEffect(() => { cargarEmpresas(); cargarUsuarios(); cargarCalendario() }, []) // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => { if (tab === 'calendario') cargarCalendario() }, [tab, anioCalendario]) // eslint-disable-line react-hooks/exhaustive-deps

  const crearEmpresa = async () => {
    try {
      await api.post('/admin/empresas', formEmpresa)
      setDialog(null)
      cargarEmpresas()
    } catch (e: unknown) {
      setError((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? 'Error al crear empresa')
    }
  }

  const eliminarEmpresa = async (id: number) => {
    if (!window.confirm('¿Eliminar esta empresa?')) return
    try {
      await api.delete(`/admin/empresas/${id}`)
      cargarEmpresas()
    } catch (e: unknown) {
      setError((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? 'No se pudo eliminar la empresa')
    }
  }

  const crearUsuario = async () => {
    try {
      await api.post('/admin/usuarios', {
        ...formUsuario,
        roles: [formUsuario.roles],
        empresaId: formUsuario.empresaId ? Number(formUsuario.empresaId) : null,
        enabled: true
      })
      setDialog(null)
      cargarUsuarios()
    } catch (e: unknown) {
      setError((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? 'Error al crear usuario')
    }
  }

  const toggleEstado = async (id: number, actual: boolean) => {
    await api.put(`/admin/usuarios/${id}/estado`, { enabled: !actual })
    cargarUsuarios()
  }

  const crearCalendario = async () => {
    try {
      await api.post('/admin/calendario', {
        anioGravable: anioCalendario,
        anioPresentacion: Number(formCalendario.anioPresentacion),
        tipoReporte: formCalendario.tipoReporte,
        rangoNitIni: formCalendario.rangoNitIni || null,
        rangoNitFin: formCalendario.rangoNitFin || null,
        fechaLimite: formCalendario.fechaLimite
      })
      setDialog(null)
      cargarCalendario()
    } catch (e: unknown) {
      setError((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? 'Error al crear fecha')
    }
  }

  const eliminarCalendario = async (id: number) => {
    if (!window.confirm('¿Eliminar esta fecha del calendario?')) return
    await api.delete(`/admin/calendario/${id}`)
    cargarCalendario()
  }

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <Typography variant="h4" component="h1">Administración</Typography>
      {error && <Alert severity="error" onClose={() => setError(null)}>{error}</Alert>}

      <Tabs value={tab} onChange={(_, v) => setTab(v)}>
        <Tab label="Empresas" value="empresas" />
        <Tab label="Usuarios" value="usuarios" />
        <Tab label="Calendario DIAN" value="calendario" />
      </Tabs>

      {tab === 'empresas' && (
        <Box sx={{ display: 'grid', gap: 2 }}>
          <Box><Button variant="contained" onClick={() => { setDialog('empresa'); setFormEmpresa({ nit: '', razonSocial: '', email: '', ciudad: '', regimen: 'ORDINARIO', tipoDocumento: 'NIT' }) }}>Nueva empresa</Button></Box>
          <TableContainer>
            <Table size="small" aria-label="Empresas">
              <TableHead>
                <TableRow>
                  <TableCell>ID</TableCell><TableCell>NIT</TableCell><TableCell>Razón social</TableCell><TableCell>Régimen</TableCell><TableCell>Email</TableCell><TableCell>Ciudad</TableCell><TableCell align="right">Acciones</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {empresas.map(e => (
                  <TableRow key={e.id}>
                    <TableCell>{e.id}</TableCell>
                    <TableCell>{e.nit}</TableCell>
                    <TableCell>{e.razonSocial}</TableCell>
                    <TableCell>{e.regimen}</TableCell>
                    <TableCell>{e.email ?? '—'}</TableCell>
                    <TableCell>{e.ciudad ?? '—'}</TableCell>
                    <TableCell align="right">
                      <IconButton size="small" color="error" onClick={() => eliminarEmpresa(e.id)} aria-label="Eliminar"><DeleteIcon fontSize="small" /></IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Box>
      )}

      {tab === 'usuarios' && (
        <Box sx={{ display: 'grid', gap: 2 }}>
          <Box><Button variant="contained" onClick={() => { setDialog('usuario'); setFormUsuario({ username: '', password: '', email: '', nombre: '', empresaId: '', roles: 'ROLE_CONTADOR' }) }}>Nuevo usuario</Button></Box>
          <TableContainer>
            <Table size="small" aria-label="Usuarios">
              <TableHead>
                <TableRow>
                  <TableCell>ID</TableCell><TableCell>Usuario</TableCell><TableCell>Email</TableCell><TableCell>Nombre</TableCell><TableCell>Roles</TableCell><TableCell>Estado</TableCell><TableCell align="right">Acciones</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {usuarios.map(u => (
                  <TableRow key={u.id}>
                    <TableCell>{u.id}</TableCell>
                    <TableCell>{u.username}</TableCell>
                    <TableCell>{u.email}</TableCell>
                    <TableCell>{u.nombre}</TableCell>
                    <TableCell>{u.roles.map(r => <Chip key={r} size="small" label={r.replace('ROLE_', '')} sx={{ mr: 0.5 }} />)}</TableCell>
                    <TableCell>
                      <Chip size="small" color={u.enabled ? 'success' : 'default'} label={u.enabled ? 'Activo' : 'Inactivo'}
                            onClick={() => toggleEstado(u.id, u.enabled)} clickable sx={{ cursor: 'pointer' }} />
                    </TableCell>
                    <TableCell align="right">—</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Box>
      )}

      {tab === 'calendario' && (
        <Box sx={{ display: 'grid', gap: 2 }}>
          <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
            <TextField label="Año gravable" type="number" size="small" value={anioCalendario} onChange={e => setAnioCalendario(Number(e.target.value))} />
            <Button variant="contained" onClick={() => { setDialog('calendario'); setFormCalendario({ anioPresentacion: anioCalendario + 1, tipoReporte: 'PERSONA_JURIDICA', rangoNitIni: '', rangoNitFin: '', fechaLimite: '' }) }}>Nueva fecha</Button>
          </Box>
          <TableContainer>
            <Table size="small" aria-label="Calendario DIAN">
              <TableHead>
                <TableRow>
                  <TableCell>Tipo reporte</TableCell><TableCell>Rango NIT</TableCell><TableCell>Fecha límite</TableCell><TableCell>Año presentación</TableCell><TableCell align="right">Acciones</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {calendario.map(c => (
                  <TableRow key={c.id}>
                    <TableCell>{c.tipoReporte.replace('_', ' ')}</TableCell>
                    <TableCell>{c.rangoNitIni ?? '*'} – {c.rangoNitFin ?? '*'}</TableCell>
                    <TableCell>{c.fechaLimite}</TableCell>
                    <TableCell>{c.anioPresentacion}</TableCell>
                    <TableCell align="right">
                      <IconButton size="small" color="error" onClick={() => eliminarCalendario(c.id)} aria-label="Eliminar"><DeleteIcon fontSize="small" /></IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Box>
      )}

      <Dialog open={dialog === 'empresa'} onClose={() => setDialog(null)} maxWidth="sm" fullWidth>
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
          <Button onClick={() => setDialog(null)}>Cancelar</Button>
          <Button variant="contained" disabled={!formEmpresa.nit || !formEmpresa.razonSocial} onClick={crearEmpresa}>Crear</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={dialog === 'usuario'} onClose={() => setDialog(null)} maxWidth="sm" fullWidth>
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
          <Button onClick={() => setDialog(null)}>Cancelar</Button>
          <Button variant="contained" disabled={!formUsuario.username || formUsuario.password.length < 6} onClick={crearUsuario}>Crear</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={dialog === 'calendario'} onClose={() => setDialog(null)} maxWidth="sm" fullWidth>
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
          <Button onClick={() => setDialog(null)}>Cancelar</Button>
          <Button variant="contained" disabled={!formCalendario.fechaLimite} onClick={crearCalendario}>Crear</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}