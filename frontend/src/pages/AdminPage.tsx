import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Tab from '@mui/material/Tab'
import Tabs from '@mui/material/Tabs'
import AuditoriaPanel from '../components/admin/AuditoriaPanel'
import CalendarioPanel from '../components/admin/CalendarioPanel'
import EmpresasPanel from '../components/admin/EmpresasPanel'
import EnviosPanel from '../components/admin/EnviosPanel'
import UsuariosPanel from '../components/admin/UsuariosPanel'
import PageHeader from '../components/ui/PageHeader'
import BrandingPage from './BrandingPage'
import IntegracionesPage from './IntegracionesPage'
import SuscripcionPage from './SuscripcionPage'

type TabIdx = 'usuarios' | 'identidad' | 'integraciones' | 'suscripcion' | 'administracion'
type AdminTabIdx = 'empresas' | 'calendario' | 'envios' | 'auditoria'

export default function AdminPage() {
  const [tab, setTab] = useState<TabIdx>('usuarios')
  const [adminTab, setAdminTab] = useState<AdminTabIdx>('empresas')
  const [error, setError] = useState<string | null>(null)

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="Empresa"
        title="Empresa"
        subtitle="Usuarios, identidad visual, integraciones, suscripción y administración de la organización."
      />

      {error && <Alert severity="error" onClose={() => setError(null)}>{error}</Alert>}

      <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="scrollable" allowScrollButtonsMobile aria-label="Secciones de empresa">
        <Tab label="Usuarios" value="usuarios" />
        <Tab label="Identidad visual" value="identidad" />
        <Tab label="Integraciones" value="integraciones" />
        <Tab label="Suscripción" value="suscripcion" />
        <Tab label="Administración" value="administracion" />
      </Tabs>

      {tab === 'usuarios' && <UsuariosPanel onError={setError} />}
      {tab === 'identidad' && <BrandingPage ocultarEncabezado />}
      {tab === 'integraciones' && <IntegracionesPage ocultarEncabezado />}
      {tab === 'suscripcion' && <SuscripcionPage ocultarEncabezado />}
      {tab === 'administracion' && (
        <Box sx={{ display: 'grid', gap: 2 }}>
          <Tabs value={adminTab} onChange={(_, v) => setAdminTab(v)} variant="scrollable" allowScrollButtonsMobile aria-label="Secciones de administración">
            <Tab label="Empresas" value="empresas" />
            <Tab label="Calendario DIAN" value="calendario" />
            <Tab label="Reintentos" value="envios" />
            <Tab label="Auditoría" value="auditoria" />
          </Tabs>
          {adminTab === 'empresas' && <EmpresasPanel onError={setError} />}
          {adminTab === 'calendario' && <CalendarioPanel onError={setError} />}
          {adminTab === 'envios' && <EnviosPanel />}
          {adminTab === 'auditoria' && <AuditoriaPanel />}
        </Box>
      )}
    </Box>
  )
}
