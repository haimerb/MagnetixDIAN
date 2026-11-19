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
import { restablecerPassword, solicitarRecuperacion } from '../api/client'

export default function RecuperarPasswordPage() {
  const [params] = useSearchParams()
  const token = params.get('token')
  const [identificador, setIdentificador] = useState('')
  const [password, setPassword] = useState('')
  const [confirmacion, setConfirmacion] = useState('')
  const [mensaje, setMensaje] = useState<string | null>(null)
  const [enlace, setEnlace] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [listo, setListo] = useState(false)
  const [cargando, setCargando] = useState(false)

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setMensaje(null)
    if (token && password !== confirmacion) {
      setError('Las contraseñas no coinciden.')
      return
    }
    setCargando(true)
    try {
      if (token) {
        await restablecerPassword(token, password)
        setListo(true)
      } else {
        const data = await solicitarRecuperacion(identificador)
        setMensaje(data.mensaje)
        setEnlace(data.enlace)
      }
    } catch (e: unknown) {
      const detalle = (e as { response?: { data?: { message?: string } } }).response?.data?.message
      setError(detalle ?? 'No se pudo completar la solicitud.')
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
          {token ? 'Nueva contraseña' : 'Recuperar acceso'}
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          {token
            ? 'Defina una contraseña de al menos 8 caracteres con letras y números.'
            : 'Indique su usuario o correo y le enviaremos un enlace de recuperación.'}
        </Typography>

        {listo ? (
          <Alert severity="success">
            Contraseña actualizada. <Link component={RouterLink} to="/login">Inicie sesión</Link> con su nueva clave.
          </Alert>
        ) : (
          <Box component="form" noValidate onSubmit={onSubmit} sx={{ display: 'grid', gap: 2 }}>
            {token ? (
              <>
                <TextField
                  label="Nueva contraseña"
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
              </>
            ) : (
              <TextField
                label="Usuario o correo"
                value={identificador}
                onChange={(e) => setIdentificador(e.target.value)}
                disabled={cargando}
                required
                autoFocus
              />
            )}
            {mensaje && <Alert severity="info">{mensaje}</Alert>}
            {enlace && (
              <Alert severity="warning">
                SMTP deshabilitado en este entorno. Use el enlace:{' '}
                <Link href={enlace}>restablecer contraseña</Link>
              </Alert>
            )}
            {error && <Alert severity="error">{error}</Alert>}
            <Button
              type="submit"
              variant="contained"
              size="large"
              disabled={cargando || (token ? password.length < 8 : !identificador)}
              loading={cargando}
            >
              {token ? 'Guardar contraseña' : 'Enviar enlace'}
            </Button>
          </Box>
        )}

        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 3, textAlign: 'center' }}>
          <Link component={RouterLink} to="/login">Volver al ingreso</Link>
        </Typography>
      </Paper>
    </Box>
  )
}
