import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import Link from '@mui/material/Link'
import MenuItem from '@mui/material/MenuItem'
import TextField from '@mui/material/TextField'
import { api, type InvitacionResponse } from '../../api/client'

const ROLES = ['ROLE_ADMIN', 'ROLE_CONTADOR', 'ROLE_CLIENTE']

type Props = {
  abierto: boolean
  onCerrar: () => void
  onInvitado: () => void
}

export default function InvitacionUsuarioDialog({ abierto, onCerrar, onInvitado }: Props) {
  const [form, setForm] = useState({ username: '', email: '', nombre: '', rol: 'ROLE_CONTADOR', empresaId: '' })
  const [enlace, setEnlace] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)

  const cerrar = () => {
    if (guardando) return
    setForm({ username: '', email: '', nombre: '', rol: 'ROLE_CONTADOR', empresaId: '' })
    setEnlace(null)
    setError(null)
    onCerrar()
  }

  const invitar = async () => {
    setGuardando(true)
    setError(null)
    try {
      const { data } = await api.post<InvitacionResponse>('/admin/usuarios/invitar', {
        username: form.username,
        email: form.email,
        nombre: form.nombre,
        roles: [form.rol],
        empresaId: form.empresaId ? Number(form.empresaId) : null,
      }, { skipGlobalError: true })
      onInvitado()
      if (data.enlace) {
        setEnlace(data.enlace)
      } else {
        cerrar()
      }
    } catch (e: unknown) {
      const detalle = (e as { response?: { data?: { message?: string } } }).response?.data?.message
      setError(detalle ?? 'No se pudo enviar la invitación.')
    } finally {
      setGuardando(false)
    }
  }

  return (
    <Dialog open={abierto} onClose={cerrar} maxWidth="sm" fullWidth>
      <DialogTitle>Invitar usuario</DialogTitle>
      <DialogContent sx={{ display: 'grid', gap: 2, pt: '16px !important' }}>
        <Alert severity="info">
          El invitado recibirá un correo para definir su contraseña. No necesita clave inicial.
        </Alert>
        <TextField label="Usuario" required value={form.username}
          onChange={(e) => setForm((p) => ({ ...p, username: e.target.value }))} />
        <TextField label="Correo" type="email" required value={form.email}
          onChange={(e) => setForm((p) => ({ ...p, email: e.target.value }))} />
        <TextField label="Nombre completo" required value={form.nombre}
          onChange={(e) => setForm((p) => ({ ...p, nombre: e.target.value }))} />
        <TextField label="Rol" select value={form.rol}
          onChange={(e) => setForm((p) => ({ ...p, rol: e.target.value }))}>
          {ROLES.map((r) => <MenuItem key={r} value={r}>{r.replace('ROLE_', '')}</MenuItem>)}
        </TextField>
        <TextField label="Empresa (ID)" type="number" value={form.empresaId}
          onChange={(e) => setForm((p) => ({ ...p, empresaId: e.target.value }))}
          helperText="Dejar vacío si no aplica" />
        {enlace && (
          <Alert severity="warning">
            SMTP deshabilitado en este entorno. Comparta este enlace:{' '}
            <Link href={enlace} target="_blank" rel="noopener">activar cuenta</Link>
          </Alert>
        )}
        {error && <Alert severity="error">{error}</Alert>}
      </DialogContent>
      <DialogActions>
        <Button onClick={cerrar} disabled={guardando}>Cerrar</Button>
        <Button
          variant="contained"
          onClick={invitar}
          disabled={guardando || !form.username || !form.email || !form.nombre}
          loading={guardando}
        >
          Enviar invitación
        </Button>
      </DialogActions>
    </Dialog>
  )
}
