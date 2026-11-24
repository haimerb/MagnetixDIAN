import { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './features/auth/AuthProvider'
import { useAuth } from './features/auth/useAuth'
import AppLayout from './components/layout/AppLayout'
import GlobalProgress from './components/layout/GlobalProgress'
import LoadingState from './components/ui/LoadingState'

const LandingPage = lazy(() => import('./pages/LandingPage'))
const LoginPage = lazy(() => import('./pages/LoginPage'))
const RegistroPage = lazy(() => import('./pages/RegistroPage'))
const TerminosPage = lazy(() => import('./pages/TerminosPage'))
const PrivacidadPage = lazy(() => import('./pages/PrivacidadPage'))
const RecuperarPasswordPage = lazy(() => import('./pages/RecuperarPasswordPage'))
const ActivarCuentaPage = lazy(() => import('./pages/ActivarCuentaPage'))
const CambiarPasswordPage = lazy(() => import('./pages/CambiarPasswordPage'))
const DashboardPage = lazy(() => import('./pages/DashboardPage'))
const ValidacionPage = lazy(() => import('./pages/ValidacionPage'))
const ReportePage = lazy(() => import('./pages/ReportePage'))
const GuiaMuiscaPage = lazy(() => import('./pages/GuiaMuiscaPage'))
const AdminPage = lazy(() => import('./pages/AdminPage'))
const FacturacionPage = lazy(() => import('./pages/FacturacionPage'))
const FacturaDetallePage = lazy(() => import('./pages/FacturaDetallePage'))
const ConfigFePage = lazy(() => import('./pages/ConfigFePage'))
const SuscripcionPage = lazy(() => import('./pages/SuscripcionPage'))
const RadianPage = lazy(() => import('./pages/RadianPage'))
const ContabilidadPage = lazy(() => import('./pages/ContabilidadPage'))
const FacturaRapidaPage = lazy(() => import('./pages/FacturaRapidaPage'))
const CheckoutPage = lazy(() => import('./pages/erp/CheckoutPage'))
const VerificarEmailPage = lazy(() => import('./pages/erp/VerificarEmailPage'))
const HabilitacionDianWizard = lazy(() => import('./pages/erp/HabilitacionDianWizard'))
const FacturaEditorPage = lazy(() => import('./pages/erp/FacturaEditorPage'))
const NominaPage = lazy(() => import('./pages/erp/NominaPage'))
const DocumentoSoportePage = lazy(() => import('./pages/erp/DocumentoSoportePage'))
const ExogenaPage = lazy(() => import('./pages/erp/ExogenaPage'))
const ComprasPage = lazy(() => import('./pages/erp/ComprasPage'))
const InventarioPage = lazy(() => import('./pages/erp/InventarioPage'))
const TesoreriaPage = lazy(() => import('./pages/erp/TesoreriaPage'))
const IntegracionesPage = lazy(() => import('./pages/erp/IntegracionesPage'))

function RequiereAuth({ children }: { children: React.ReactNode }) {
  const { sesion } = useAuth()
  if (!sesion) return <Navigate to="/login" replace />
  return <>{children}</>
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <GlobalProgress />
        <Suspense fallback={<LoadingState label="Cargando página…" minHeight="60vh" />}>
          <Routes>
            <Route path="/" element={<LandingPage />} />
            <Route path="/registro" element={<RegistroPage />} />
            <Route path="/terminos" element={<TerminosPage />} />
            <Route path="/privacidad" element={<PrivacidadPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/recuperar" element={<RecuperarPasswordPage />} />
            <Route path="/restablecer" element={<RecuperarPasswordPage />} />
            <Route path="/activar" element={<ActivarCuentaPage />} />
            <Route path="/checkout" element={<CheckoutPage />} />
            <Route path="/verificar-email" element={<VerificarEmailPage />} />
            <Route
              path="/app"
              element={
                <RequiereAuth>
                  <AppLayout />
                </RequiereAuth>
              }
            >
              <Route index element={<DashboardPage />} />
              <Route path="carga" element={<Navigate to="/app/contabilidad/asentar-facturacion" replace />} />
              <Route path="importacion-avanzada" element={<Navigate to="/app/contabilidad/asentar-facturacion" replace />} />
              <Route path="importacion-universal" element={<Navigate to="/app/contabilidad/asentar-facturacion" replace />} />
              <Route path="radian" element={<Navigate to="/app/contabilidad/asentar-facturacion" replace />} />
              <Route path="contabilidad/asentar-facturacion" element={<RadianPage />} />
              <Route path="contabilidad" element={<ContabilidadPage />} />
              <Route path="migracion-contable" element={<Navigate to="/app/contabilidad/asentar-facturacion" replace />} />
              <Route path="factura-rapida" element={<FacturaRapidaPage />} />
              <Route path="habilitacion-dian" element={<HabilitacionDianWizard />} />
              <Route path="exogena" element={<Navigate to="/app/erp/exogena" replace />} />
              <Route path="erp/nomina" element={<NominaPage />} />
              <Route path="erp/documentos-soporte" element={<DocumentoSoportePage />} />
              <Route path="erp/exogena" element={<ExogenaPage />} />
              <Route path="erp/compras" element={<ComprasPage />} />
              <Route path="erp/inventario" element={<InventarioPage />} />
              <Route path="erp/tesoreria" element={<TesoreriaPage />} />
              <Route path="erp/integraciones" element={<IntegracionesPage />} />
              <Route path="branding" element={<Navigate to="/app/empresa" replace />} />
              <Route path="validacion/:medioId?" element={<ValidacionPage />} />
              <Route path="reporte/:medioId?" element={<ReportePage />} />
              <Route path="guia" element={<GuiaMuiscaPage />} />
              <Route path="facturacion" element={<FacturacionPage />} />
              <Route path="facturacion/config" element={<ConfigFePage />} />
              <Route path="facturacion/nueva" element={<FacturaEditorPage />} />
              <Route path="facturacion/:id" element={<FacturaDetallePage />} />
              <Route path="suscripcion" element={<SuscripcionPage />} />
              <Route path="integraciones" element={<Navigate to="/app/empresa" replace />} />
              <Route path="admin" element={<Navigate to="/app/empresa" replace />} />
              <Route path="empresa" element={<AdminPage />} />
              <Route path="cuenta" element={<CambiarPasswordPage />} />
            </Route>
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Suspense>
      </BrowserRouter>
    </AuthProvider>
  )
}
