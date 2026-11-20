import { useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import AppBar from '@mui/material/AppBar'
import Avatar from '@mui/material/Avatar'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Container from '@mui/material/Container'
import Divider from '@mui/material/Divider'
import Drawer from '@mui/material/Drawer'
import IconButton from '@mui/material/IconButton'
import List from '@mui/material/List'
import ListItemButton from '@mui/material/ListItemButton'
import ListItemText from '@mui/material/ListItemText'
import Menu from '@mui/material/Menu'
import MenuItem from '@mui/material/MenuItem'
import Toolbar from '@mui/material/Toolbar'
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import CloseIcon from '@mui/icons-material/Close'
import DarkModeOutlinedIcon from '@mui/icons-material/DarkModeOutlined'
import LightModeOutlinedIcon from '@mui/icons-material/LightModeOutlined'
import LockResetIcon from '@mui/icons-material/LockReset'
import LogoutIcon from '@mui/icons-material/Logout'
import MenuIcon from '@mui/icons-material/Menu'
import { alpha } from '@mui/material/styles'
import Logo from '../ui/Logo'
import { useAuth } from '../../features/auth/useAuth'
import { useThemeMode } from '../../theme/useThemeMode'

const NAV_ITEMS = [
  { to: '/app', label: 'Panel' },
  { to: '/app/carga', label: 'Carga de datos' },
  { to: '/app/validacion', label: 'Validación' },
  { to: '/app/reporte', label: 'Reportes' },
  { to: '/app/facturacion', label: 'Facturación' },
  { to: '/app/suscripcion', label: 'Suscripción' },
  { to: '/app/integraciones', label: 'Integraciones' },
  { to: '/app/guia', label: 'Guía MUISCA' },
  { to: '/app/admin', label: 'Administración', admin: true as const },
]

const ROL_LABEL: Record<string, string> = {
  ROLE_ADMIN: 'Administrador',
  ROLE_CONTADOR: 'Contador',
  ROLE_CLIENTE: 'Cliente',
}

export default function AppLayout() {
  const location = useLocation()
  const navigate = useNavigate()
  const { sesion, logout } = useAuth()
  const { mode, alternar } = useThemeMode()
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null)

  const items = NAV_ITEMS.filter((item) => !item.admin || sesion?.roles?.includes('ROLE_ADMIN'))
  const isActive = (to: string) =>
    to === '/app' ? location.pathname === '/app' : location.pathname.startsWith(to)

  const iniciales = (sesion?.username ?? '?').slice(0, 2).toUpperCase()
  const roles = (sesion?.roles ?? []).map((r) => ROL_LABEL[r] ?? r).join(' · ')

  const onLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <Box
        component="a"
        href="#main-content"
        sx={{
          position: 'absolute',
          left: 8,
          top: -64,
          zIndex: (t) => t.zIndex.tooltip,
          bgcolor: 'primary.main',
          color: 'primary.contrastText',
          px: 2,
          py: 1,
          borderRadius: 1,
          transition: 'top 150ms ease',
          '&:focus': { top: 8 },
        }}
      >
        Saltar al contenido principal
      </Box>

      <AppBar
        position="sticky"
        elevation={0}
        sx={{
          bgcolor: (t) => alpha(t.palette.background.paper, 0.88),
          backdropFilter: 'blur(8px)',
          color: 'text.primary',
          borderBottom: '1px solid',
          borderColor: (t) => t.palette.divider,
        }}
      >
        <Container maxWidth="lg">
          <Toolbar disableGutters sx={{ gap: { xs: 1, md: 2 }, minHeight: { xs: 64 } }}>
            <Box
              role="button"
              tabIndex={0}
              aria-label="Ir al panel"
              onClick={() => navigate('/app')}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault()
                  navigate('/app')
                }
              }}
              sx={{
                cursor: 'pointer',
                borderRadius: 1,
                outlineOffset: 4,
                '&:focus-visible': { outline: '2px solid', outlineColor: 'primary.main' },
              }}
            >
              <Logo size={32} />
            </Box>

            <Box
              component="nav"
              aria-label="Navegación principal"
              sx={{ display: { xs: 'none', md: 'flex' }, gap: 0.5, ml: 1 }}
            >
              {items.map((item) => {
                const active = isActive(item.to)
                return (
                  <Button
                    key={item.to}
                    disableRipple
                    onClick={() => navigate(item.to)}
                    aria-current={active ? 'page' : undefined}
                    sx={{
                      px: 1.5,
                      position: 'relative',
                      color: active ? 'primary.main' : 'text.secondary',
                      fontWeight: active ? 700 : 500,
                      '&:hover': {
                        color: 'primary.main',
                        bgcolor: (t) => alpha(t.palette.primary.main, 0.06),
                      },
                      '&::after': active
                        ? {
                            content: '""',
                            position: 'absolute',
                            left: 12,
                            right: 12,
                            bottom: 4,
                            height: 2,
                            borderRadius: 1,
                            bgcolor: 'secondary.main',
                          }
                        : {},
                    }}
                  >
                    {item.label}
                  </Button>
                )
              })}
            </Box>

            <Box sx={{ flexGrow: 1 }} />

            <Tooltip title={mode === 'dark' ? 'Tema claro' : 'Tema oscuro'}>
              <IconButton
                onClick={alternar}
                aria-label={mode === 'dark' ? 'Activar tema claro' : 'Activar tema oscuro'}
                size="small"
              >
                {mode === 'dark' ? <LightModeOutlinedIcon /> : <DarkModeOutlinedIcon />}
              </IconButton>
            </Tooltip>

            <IconButton
              onClick={(e) => setMenuAnchor(e.currentTarget)}
              aria-label="Menú de usuario"
              size="small"
            >
              <Avatar
                sx={{
                  width: 34,
                  height: 34,
                  bgcolor: 'primary.main',
                  fontSize: 13,
                  fontWeight: 700,
                }}
              >
                {iniciales}
              </Avatar>
            </IconButton>

            <IconButton
              onClick={() => setDrawerOpen(true)}
              aria-label="Abrir menú de navegación"
              sx={{ display: { xs: 'inline-flex', md: 'none' } }}
            >
              <MenuIcon />
            </IconButton>
          </Toolbar>
        </Container>
      </AppBar>

      <Menu
        anchorEl={menuAnchor}
        open={Boolean(menuAnchor)}
        onClose={() => setMenuAnchor(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <Box sx={{ px: 2, py: 1, minWidth: 200 }}>
          <Typography variant="subtitle2">{sesion?.username}</Typography>
          <Typography variant="caption" color="text.secondary">
            {roles || 'Sin rol'}
          </Typography>
        </Box>
        <Divider />
        <MenuItem onClick={() => { setMenuAnchor(null); navigate('/app/cuenta') }}>
          <LockResetIcon fontSize="small" sx={{ mr: 1 }} />
          Cambiar contraseña
        </MenuItem>
        <MenuItem onClick={onLogout}>
          <LogoutIcon fontSize="small" sx={{ mr: 1 }} />
          Salir
        </MenuItem>
      </Menu>

      <Drawer anchor="right" open={drawerOpen} onClose={() => setDrawerOpen(false)}>
        <Box sx={{ width: 280, p: 2, display: 'flex', flexDirection: 'column', gap: 1.5 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <Logo size={30} />
            <IconButton onClick={() => setDrawerOpen(false)} aria-label="Cerrar menú">
              <CloseIcon />
            </IconButton>
          </Box>
          <Divider />
          <List disablePadding aria-label="Navegación principal">
            {items.map((item) => (
              <ListItemButton
                key={item.to}
                selected={isActive(item.to)}
                aria-current={isActive(item.to) ? 'page' : undefined}
                onClick={() => {
                  navigate(item.to)
                  setDrawerOpen(false)
                }}
                sx={{ borderRadius: 2, mb: 0.5 }}
              >
                <ListItemText primary={item.label} />
              </ListItemButton>
            ))}
          </List>
          <Divider />
          <Box sx={{ px: 1 }}>
            <Typography variant="subtitle2">{sesion?.username}</Typography>
            <Typography variant="caption" color="text.secondary">
              {roles || 'Sin rol'}
            </Typography>
          </Box>
          <Button
            color="inherit"
            startIcon={<LockResetIcon />}
            onClick={() => { setDrawerOpen(false); navigate('/app/cuenta') }}
            sx={{ justifyContent: 'flex-start' }}
          >
            Cambiar contraseña
          </Button>
          <Button color="inherit" startIcon={<LogoutIcon />} onClick={onLogout} sx={{ justifyContent: 'flex-start' }}>
            Salir
          </Button>
        </Box>
      </Drawer>

      <Box component="main" id="main-content" sx={{ flexGrow: 1, py: { xs: 3.5, md: 5 } }}>
        <Container maxWidth="lg">
          <Outlet />
        </Container>
      </Box>

      <Box component="footer" sx={{ borderTop: '1px solid', borderColor: (t) => t.palette.divider, py: 3 }}>
        <Container maxWidth="lg">
          <Box sx={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 1 }}>
            <Typography variant="caption" color="text.secondary">
              MagnetixDIAN · Información exógena DIAN · Resolución Única 000227 de 2025
            </Typography>
            <Box sx={{ display: 'flex', gap: 2 }}>
              <Typography component="a" href="/terminos" variant="caption" color="text.secondary">
                Términos
              </Typography>
              <Typography component="a" href="/privacidad" variant="caption" color="text.secondary">
                Privacidad
              </Typography>
            </Box>
          </Box>
        </Container>
      </Box>
    </Box>
  )
}
