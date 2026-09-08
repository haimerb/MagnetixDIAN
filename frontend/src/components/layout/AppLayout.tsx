import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import AppBar from '@mui/material/AppBar'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Container from '@mui/material/Container'
import Toolbar from '@mui/material/Toolbar'
import Typography from '@mui/material/Typography'
import { useAuth } from '../../features/auth/useAuth'

const NAV_ITEMS = [
  { to: '/', label: 'Panel' },
  { to: '/carga', label: 'Carga de datos' },
  { to: '/validacion', label: 'Validación' },
  { to: '/reporte', label: 'Reportes' },
  { to: '/guia', label: 'Guía MUISCA' },
  { to: '/admin', label: 'Administración', admin: true as const },
]

export default function AppLayout() {
  const location = useLocation()
  const navigate = useNavigate()
  const { sesion, logout } = useAuth()

  const onLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <AppBar position="static" color="primary" elevation={0}>
        <Toolbar sx={{ gap: { xs: 1, md: 2 } }}>
          <Typography
            variant="h6"
            component={Box}
            sx={{ flexGrow: 1, fontFamily: '"Fraunces", serif', fontWeight: 600 }}
          >
            MagnetixDIAN
          </Typography>
          <Box component="nav" aria-label="Navegación principal" sx={{ display: { xs: 'none', md: 'flex' }, gap: 1 }}>
            {NAV_ITEMS
              .filter(item => !item.admin || sesion?.roles?.includes('ROLE_ADMIN'))
              .map((item) => (
                <Button
                  key={item.to}
                  color="inherit"
                  onClick={() => navigate(item.to)}
                  sx={{
                    opacity: location.pathname === item.to ? 1 : 0.75,
                    textDecoration: location.pathname === item.to ? 'underline' : 'none',
                  }}
                >
                  {item.label}
                </Button>
              ))}
          </Box>
          <Typography variant="body2" color="inherit" sx={{ display: { xs: 'none', sm: 'block' } }}>
            {sesion?.username}
          </Typography>
          <Button color="inherit" onClick={onLogout}>
            Salir
          </Button>
        </Toolbar>
      </AppBar>

      <Box component="main" sx={{ flexGrow: 1, py: { xs: 3.5, md: 6 } }} id="main-content">
        <Container maxWidth="lg">
          <Outlet />
        </Container>
      </Box>

      <Box component="footer" sx={{ borderTop: '1px solid divider', py: 3 }}>
        <Container maxWidth="lg">
          <Typography variant="caption" color="text.secondary">
            MagnetixDIAN · Información exógena DIAN · Resolución Única 000227 de 2025
          </Typography>
        </Container>
      </Box>
    </Box>
  )
}