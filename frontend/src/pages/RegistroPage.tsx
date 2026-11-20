import { useState } from 'react'
import { Link as RouterLink, useNavigate } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Checkbox from '@mui/material/Checkbox'
import FormControlLabel from '@mui/material/FormControlLabel'
import Link from '@mui/material/Link'
import Paper from '@mui/material/Paper'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import Logo from '../components/ui/Logo'
import { registrarEmpresa } from '../api/client'

export default function RegistroPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    nit: '',
    razonSocial: '',
    nombre: '',
    email: '',
    username: '',
    password: '',
    website: '',
  })
  const [aceptaTerminos, setAceptaTerminos] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [cargando, setCargando] = useState(false)

  const actualizar = (campo: keyof typeof form) => (evento: React.ChangeEvent<HTMLInputElement>) => {
    setForm((actual) => ({ ...actual, [campo]: evento.target.value }))
  }

  const onSubmit = async (evento: React.FormEvent) => {
    evento.preventDefault()
    setError(null)
    setCargando(true)
    try {
      await registrarEmpresa({ ...form, aceptaTerminos })
      navigate('/login', { state: { registro: true, username: form.username } })
    } catch (err) {
      const respuesta = err as { response?: { data?: { message?: string } } }
      setError(respuesta.response?.data?.message ?? 'No se pudo crear la cuenta. Revise los datos e intente de nuevo.')
    } finally {
      setCargando(false)
    }
  }

  return (
    <Box
      component="main"
      sx={{
        minHeight: '100vh',
        display: 'grid',
        placeItems: 'center',
        p: 3,
        bgcolor: 'background.default',
      }}
    >
      <Paper
        elevation={0}
        sx={{
          width: '100%',
          maxWidth: 620,
          p: { xs: 3, sm: 4 },
          border: '1px solid',
          borderColor: 'divider',
        }}
      >
        <Box sx={{ mb: 3 }}>
          <Logo size={34} />
        </Box>
        <Typography variant="h4" component="h1" gutterBottom>
          Crear cuenta
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Registre su empresa y comience con el plan gratuito: 50 documentos y 1 usuario al mes.
        </Typography>

        <Box component="form" noValidate onSubmit={onSubmit} sx={{ display: 'grid', gap: 2 }}>
          <TextField
            label="NIT"
            value={form.nit}
            onChange={actualizar('nit')}
            autoComplete="off"
            required
            fullWidth
          />
          <TextField
            label="Razón social"
            value={form.razonSocial}
            onChange={actualizar('razonSocial')}
            autoComplete="organization"
            required
            fullWidth
          />
          <TextField
            label="Nombre completo"
            value={form.nombre}
            onChange={actualizar('nombre')}
            autoComplete="name"
            required
            fullWidth
          />
          <TextField
            label="Correo electrónico"
            type="email"
            value={form.email}
            onChange={actualizar('email')}
            autoComplete="email"
            required
            fullWidth
          />
          <TextField
            label="Usuario"
            value={form.username}
            onChange={actualizar('username')}
            autoComplete="username"
            required
            fullWidth
          />
          <TextField
            label="Contraseña"
            type="password"
            value={form.password}
            onChange={actualizar('password')}
            autoComplete="new-password"
            helperText="Mínimo 8 caracteres, con letras y números."
            required
            fullWidth
          />
          <TextField
            label="Sitio web"
            value={form.website}
            onChange={actualizar('website')}
            autoComplete="off"
            tabIndex={-1}
            aria-hidden
            sx={{ display: 'none' }}
          />
          <FormControlLabel
            control={
              <Checkbox checked={aceptaTerminos} onChange={(evento) => setAceptaTerminos(evento.target.checked)} />
            }
            label="Acepto los términos y condiciones"
          />
          <Typography variant="caption" color="text.secondary">
            Consulte los{' '}
            <Link component={RouterLink} to="/terminos">
              términos y condiciones
            </Link>{' '}
            y la{' '}
            <Link component={RouterLink} to="/privacidad">
              política de privacidad
            </Link>
            .
          </Typography>
          {error && <Alert severity="error">{error}</Alert>}
          <Button type="submit" variant="contained" size="large" disabled={cargando} loading={cargando}>
            Crear cuenta gratis
          </Button>
        </Box>

        <Typography variant="body2" color="text.secondary" sx={{ mt: 3, textAlign: 'center' }}>
          ¿Ya tiene cuenta?{' '}
          <Link component={RouterLink} to="/login">
            Ingresar
          </Link>
        </Typography>
      </Paper>
    </Box>
  )
}
