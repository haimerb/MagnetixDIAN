import Box from '@mui/material/Box'
import CircularProgress from '@mui/material/CircularProgress'
import { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './features/auth/AuthProvider'
import { useAuth } from './features/auth/useAuth'
import AppLayout from './components/layout/AppLayout'

const LoginPage = lazy(() => import('./pages/LoginPage'))
const DashboardPage = lazy(() => import('./pages/DashboardPage'))
const CargaPage = lazy(() => import('./pages/CargaPage'))
const ValidacionPage = lazy(() => import('./pages/ValidacionPage'))
const ReportePage = lazy(() => import('./pages/ReportePage'))
const GuiaMuiscaPage = lazy(() => import('./pages/GuiaMuiscaPage'))
const AdminPage = lazy(() => import('./pages/AdminPage'))
const FacturacionPage = lazy(() => import('./pages/FacturacionPage'))
const FacturaDetallePage = lazy(() => import('./pages/FacturaDetallePage'))
const ConfigFePage = lazy(() => import('./pages/ConfigFePage'))

function RequiereAuth({ children }: { children: React.ReactNode }) {
  const { sesion } = useAuth()
  if (!sesion) return <Navigate to="/login" replace />
  return <>{children}</>
}

function Cargando() {
  return (
    <Box sx={{ display: 'grid', placeItems: 'center', minHeight: '60vh' }}>
      <CircularProgress size={28} />
    </Box>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Suspense fallback={<Cargando />}>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route
              path="/"
              element={
                <RequiereAuth>
                  <AppLayout />
                </RequiereAuth>
              }
            >
              <Route index element={<DashboardPage />} />
              <Route path="carga" element={<CargaPage />} />
              <Route path="validacion/:medioId?" element={<ValidacionPage />} />
              <Route path="reporte/:medioId?" element={<ReportePage />} />
              <Route path="guia" element={<GuiaMuiscaPage />} />
              <Route path="facturacion" element={<FacturacionPage />} />
              <Route path="facturacion/config" element={<ConfigFePage />} />
              <Route path="facturacion/:id" element={<FacturaDetallePage />} />
              <Route path="admin" element={<AdminPage />} />
            </Route>
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Suspense>
      </BrowserRouter>
    </AuthProvider>
  )
}
