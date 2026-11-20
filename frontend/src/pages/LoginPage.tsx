import { useState } from 'react'
import { Link as RouterLink, useLocation, useNavigate } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Link from '@mui/material/Link'
import Paper from '@mui/material/Paper'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import Logo from '../components/ui/Logo'
import { useAuth } from '../features/auth/useAuth'

const BENEFICIOS = [
  'Validación con reglas DIAN: NIT, UVT y catálogo de conceptos.',
  'Generación del XML formato 1001 listo para presentar.',
  'Facturación electrónica multi-tenant integrada.',
  'Guía paso a paso para la presentación en MUISCA.',
]

export default function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { login } = useAuth()
  const estado = location.state as { registro?: boolean; username?: string } | null
  const [username, setUsername] = useState(estado?.username ?? '')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [cargando, setCargando] = useState(false)

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setCargando(true)
    try {
      await login(username, password)
      navigate('/app')
    } catch {
      setError('Credenciales inválidas. Verifique usuario y contraseña.')
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
        gridTemplateColumns: { xs: '1fr', md: '1.05fr 1fr' },
        bgcolor: 'background.default',
      }}
    >
      <Box
        sx={{
          display: { xs: 'none', md: 'flex' },
          flexDirection: 'column',
          justifyContent: 'space-between',
          p: 6,
          color: '#fff',
          background: (t) =>
            `linear-gradient(150deg, ${t.palette.primary.dark}, ${t.palette.primary.main} 58%, ${t.palette.primary.light})`,
        }}
      >
        <Logo size={40} variant="onDark" />
        <Box sx={{ maxWidth: 460 }}>
          <Typography variant="h3" sx={{ color: '#fff', mb: 2 }}>
            Información exógena DIAN sin fricción
          </Typography>
          <Typography variant="body1" sx={{ color: 'rgba(255,255,255,0.82)' }}>
            Cargue, valide y presente sus medios magnéticos con confianza.
          </Typography>
          <Box component="ul" sx={{ mt: 4, display: 'grid', gap: 1.5, pl: 0, listStyle: 'none' }}>
            {BENEFICIOS.map((b) => (
              <Box
                component="li"
                key={b}
                sx={{ display: 'flex', gap: 1.5, alignItems: 'flex-start' }}
              >
                <Box
                  sx={{
                    width: 8,
                    height: 8,
                    mt: 0.75,
                    borderRadius: '50%',
                    bgcolor: 'secondary.main',
                    flexShrink: 0,
                  }}
                />
                <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.92)' }}>
                  {b}
                </Typography>
              </Box>
            ))}
          </Box>
        </Box>
        <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.6)' }}>
          Resolución Única 000227 de 2025
        </Typography>
      </Box>

      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', p: { xs: 3, md: 6 } }}>
        <Paper
          elevation={0}
          sx={{
            maxWidth: 420,
            width: '100%',
            p: { xs: 3, md: 4 },
            border: '1px solid',
            borderColor: (t) => t.palette.divider,
          }}
        >
          <Box sx={{ display: { xs: 'block', md: 'none' }, mb: 3 }}>
            <Logo size={34} />
          </Box>
          <Typography variant="h5" component="h1">
            Ingresar
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Use sus credenciales habilitadas.
          </Typography>

          {estado?.registro && (
            <Alert severity="success" sx={{ mb: 2 }}>
              Cuenta creada correctamente. Ingrese para continuar.
            </Alert>
          )}

          <Box component="form" noValidate onSubmit={onSubmit} sx={{ display: 'grid', gap: 2 }}>
            <TextField
              label="Usuario"
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              disabled={cargando}
              autoFocus
              required
            />
            <TextField
              label="Contraseña"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={cargando}
              required
            />
            {error && <Alert severity="error">{error}</Alert>}
            <Button type="submit" variant="contained" size="large" disabled={cargando} loading={cargando}>
              Ingresar
            </Button>
            <Link component={RouterLink} to="/recuperar" variant="body2" sx={{ justifySelf: 'center' }}>
              ¿Olvidó su contraseña?
            </Link>
          </Box>

          <Typography variant="body2" color="text.secondary" sx={{ mt: 3, textAlign: 'center' }}>
            ¿No tiene cuenta?{' '}
            <Link component={RouterLink} to="/registro">
              Crear cuenta gratis
            </Link>
          </Typography>
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1, textAlign: 'center' }}>
            <Link component={RouterLink} to="/terminos">
              Términos
            </Link>{' '}
            ·{' '}
            <Link component={RouterLink} to="/privacidad">
              Privacidad
            </Link>
          </Typography>
          {import.meta.env.DEV && (
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 2, textAlign: 'center' }}>
              Usuario demo: admin / admin123
            </Typography>
          )}
        </Paper>
      </Box>
    </Box>
  )
}
