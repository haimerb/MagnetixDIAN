import { useState } from 'react'
import { Link as RouterLink, useSearchParams } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Link from '@mui/material/Link'
import Paper from '@mui/material/Paper'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import Logo from '../components/ui/Logo'
import { aceptarInvitacion } from '../api/client'

export default function ActivarCuentaPage() {
  const [params] = useSearchParams()
  const token = params.get('token')
  const [password, setPassword] = useState('')
  const [confirmacion, setConfirmacion] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [listo, setListo] = useState(false)
  const [cargando, setCargando] = useState(false)

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    if (!token) {
      setError('Falta el token de la invitación.')
      return
    }
    if (password !== confirmacion) {
      setError('Las contraseñas no coinciden.')
      return
    }
    setCargando(true)
    try {
      await aceptarInvitacion(token, password)
      setListo(true)
    } catch (e: unknown) {
      const detalle = (e as { response?: { data?: { message?: string } } }).response?.data?.message
      setError(detalle ?? 'No se pudo activar la cuenta.')
    } finally {
      setCargando(false)
    }
  }

  return (
    <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center', p: 3, bgcolor: 'background.default' }}>
      <Paper
        elevation={0}
        sx={{ maxWidth: 440, width: '100%', p: { xs: 3, md: 4 }, border: '1px solid', borderColor: 'divider' }}
      >
        <Box sx={{ mb: 3 }}>
          <Logo size={34} />
        </Box>
        <Typography variant="h5" component="h1">
          Active su cuenta
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Defina la contraseña con la que ingresará a MagnetixDIAN.
        </Typography>

        {listo ? (
          <Alert severity="success">
            Cuenta activada. <Link component={RouterLink} to="/login">Inicie sesión</Link>.
          </Alert>
        ) : (
          <Box component="form" noValidate onSubmit={onSubmit} sx={{ display: 'grid', gap: 2 }}>
            <TextField
              label="Contraseña"
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={cargando}
              required
              autoFocus
            />
            <TextField
              label="Confirmar contraseña"
              type="password"
              autoComplete="new-password"
              value={confirmacion}
              onChange={(e) => setConfirmacion(e.target.value)}
              disabled={cargando}
              required
            />
            {error && <Alert severity="error">{error}</Alert>}
            <Button
              type="submit"
              variant="contained"
              size="large"
              disabled={cargando || !token || password.length < 8}
              loading={cargando}
            >
              Activar cuenta
            </Button>
          </Box>
        )}
      </Paper>
    </Box>
  )
}
