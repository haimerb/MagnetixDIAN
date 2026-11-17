import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './features/auth/AuthProvider'
import { useAuth } from './features/auth/useAuth'
import AppLayout from './components/layout/AppLayout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import CargaPage from './pages/CargaPage'
import ValidacionPage from './pages/ValidacionPage'
import ReportePage from './pages/ReportePage'
import GuiaMuiscaPage from './pages/GuiaMuiscaPage'
import AdminPage from './pages/AdminPage'
import FacturacionPage from './pages/FacturacionPage'
import FacturaDetallePage from './pages/FacturaDetallePage'
import ConfigFePage from './pages/ConfigFePage'

function RequiereAuth({ children }: { children: React.ReactNode }) {
  const { sesion } = useAuth()
  if (!sesion) return <Navigate to="/login" replace />
  return <>{children}</>
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
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
      </BrowserRouter>
    </AuthProvider>
  )
}