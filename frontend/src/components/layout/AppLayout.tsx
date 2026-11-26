import { useEffect, useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import AppBar from '@mui/material/AppBar'
import Avatar from '@mui/material/Avatar'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Collapse from '@mui/material/Collapse'
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
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import LightModeOutlinedIcon from '@mui/icons-material/LightModeOutlined'
import LockResetIcon from '@mui/icons-material/LockReset'
import LogoutIcon from '@mui/icons-material/Logout'
import MenuIcon from '@mui/icons-material/Menu'
import { alpha } from '@mui/material/styles'
import Logo from '../ui/Logo'
import { useAuth } from '../../features/auth/useAuth'
import { useThemeMode } from '../../theme/useThemeMode'

const NAV_ITEMS = [
  { to: '/app', label: 'Inicio' },
  { to: '/app/facturacion', label: 'Facturación' },
  { to: '/app/contabilidad/asentar-facturacion', label: 'Contabilidad' },
  { to: '/app/erp', label: 'ERP' },
  { to: '/app/empresa', label: 'Empresa' },
]

const ERP_ITEMS = [
  { to: '/app/erp/ventas', label: 'Ventas' },
  { to: '/app/erp/compras', label: 'Compras' },
  { to: '/app/erp/inventario', label: 'Inventario' },
  { to: '/app/erp/tesoreria', label: 'Tesorería' },
  { to: '/app/erp/exogena', label: 'Exógena' },
  { to: '/app/erp/integraciones', label: 'Integraciones' },
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
  const { mode, alternar, branding } = useThemeMode()
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null)
  const [erpAnchor, setErpAnchor] = useState<HTMLElement | null>(null)
  const [erpAbierto, setErpAbierto] = useState(false)

  useEffect(() => {
    setErpAnchor(null)
    setErpAbierto(false)
  }, [location.pathname])

  const items = NAV_ITEMS
  const isActive = (to: string) => {
    if (to === '/app') return location.pathname === '/app'
    if (to === '/app/contabilidad/asentar-facturacion') {
      return location.pathname.startsWith('/app/contabilidad')
    }
    return location.pathname.startsWith(to)
  }

  const navegar = (to: string) => {
    navigate(to)
    setErpAnchor(null)
    setDrawerOpen(false)
    setErpAbierto(false)
  }

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
              <Logo size={32} branding={branding} />
            </Box>

            <Box
              component="nav"
              aria-label="Navegación principal"
              sx={{ display: { xs: 'none', md: 'flex' }, gap: 0.5, ml: 1 }}
            >
              {items.map((item) => {
                const active = isActive(item.to)
                if (item.to === '/app/erp') {
                  return (
                    <Button
                      key={item.to}
                      disableRipple
                      onClick={(event) => {
                        const objetivo = event.currentTarget
                        setErpAnchor((ancla) => (ancla ? null : objetivo))
                      }}
                      aria-haspopup="menu"
                      aria-controls="erp-menu"
                      aria-expanded={Boolean(erpAnchor)}
                      sx={{
                        px: 1.5,
                        position: 'relative',
                        display: 'inline-flex',
                        gap: 0.5,
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
                      <ExpandMoreIcon
                        fontSize="small"
                        sx={{
                          transform: erpAnchor ? 'rotate(180deg)' : 'none',
                          transition: 'transform 150ms ease',
                        }}
                      />
                    </Button>
                  )
                }
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
        id="erp-menu"
        anchorEl={erpAnchor}
        open={Boolean(erpAnchor)}
        onClose={() => setErpAnchor(null)}
        aria-label="Submenú ERP"
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
        transformOrigin={{ vertical: 'top', horizontal: 'left' }}
      >
        {ERP_ITEMS.map((item) => (
          <MenuItem
            key={item.to}
            selected={isActive(item.to)}
            aria-current={isActive(item.to) ? 'page' : undefined}
            onClick={() => navegar(item.to)}
          >
            {item.label}
          </MenuItem>
        ))}
      </Menu>

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
            <Logo size={30} branding={branding} />
            <IconButton onClick={() => setDrawerOpen(false)} aria-label="Cerrar menú">
              <CloseIcon />
            </IconButton>
          </Box>
          <Divider />
          <List disablePadding aria-label="Navegación principal">
            {items.map((item) => {
              if (item.to === '/app/erp') {
                return (
                  <Box key={item.to}>
                    <ListItemButton
                      selected={isActive(item.to)}
                      aria-expanded={erpAbierto}
                      aria-controls="erp-submenu"
                      onClick={() => setErpAbierto((abierto) => !abierto)}
                      sx={{ borderRadius: 2, mb: 0.5 }}
                    >
                      <ListItemText primary={item.label} />
                      <ExpandMoreIcon
                        fontSize="small"
                        sx={{
                          transform: erpAbierto ? 'rotate(180deg)' : 'none',
                          transition: 'transform 150ms ease',
                        }}
                      />
                    </ListItemButton>
                    <Collapse in={erpAbierto}>
                      <List id="erp-submenu" disablePadding aria-label="Submenú ERP" sx={{ pl: 2 }}>
                        {ERP_ITEMS.map((erpItem) => (
                          <ListItemButton
                            key={erpItem.to}
                            selected={isActive(erpItem.to)}
                            aria-current={isActive(erpItem.to) ? 'page' : undefined}
                            onClick={() => navegar(erpItem.to)}
                            sx={{ borderRadius: 2, mb: 0.5 }}
                          >
                            <ListItemText primary={erpItem.label} />
                          </ListItemButton>
                        ))}
                      </List>
                    </Collapse>
                  </Box>
                )
              }
              return (
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
              )
            })}
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
              MagnetixDIAN · ERP contable y facturación electrónica · Exógena DIAN
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
