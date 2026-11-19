import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Tab from '@mui/material/Tab'
import Tabs from '@mui/material/Tabs'
import HistoryOutlinedIcon from '@mui/icons-material/HistoryOutlined'
import SyncOutlinedIcon from '@mui/icons-material/SyncOutlined'
import AuditoriaPanel from '../components/admin/AuditoriaPanel'
import CalendarioPanel from '../components/admin/CalendarioPanel'
import EmpresasPanel from '../components/admin/EmpresasPanel'
import EnviosPanel from '../components/admin/EnviosPanel'
import UsuariosPanel from '../components/admin/UsuariosPanel'
import PageHeader from '../components/ui/PageHeader'

type TabIdx = 'empresas' | 'usuarios' | 'calendario' | 'envios' | 'auditoria'

export default function AdminPage() {
  const [tab, setTab] = useState<TabIdx>('empresas')
  const [error, setError] = useState<string | null>(null)

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="Configuración"
        title="Administración"
        subtitle="Gestione empresas, usuarios y el calendario tributario usado para los recordatorios."
      />

      {error && <Alert severity="error" onClose={() => setError(null)}>{error}</Alert>}

      <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="scrollable" allowScrollButtonsMobile aria-label="Secciones de administración">
        <Tab label="Empresas" value="empresas" />
        <Tab label="Usuarios" value="usuarios" />
        <Tab label="Calendario DIAN" value="calendario" />
        <Tab label="Reintentos" value="envios" icon={<SyncOutlinedIcon />} iconPosition="start" />
        <Tab label="Auditoría" value="auditoria" icon={<HistoryOutlinedIcon />} iconPosition="start" />
      </Tabs>

      {tab === 'empresas' && <EmpresasPanel onError={setError} />}
      {tab === 'usuarios' && <UsuariosPanel onError={setError} />}
      {tab === 'calendario' && <CalendarioPanel onError={setError} />}
      {tab === 'envios' && <EnviosPanel />}
      {tab === 'auditoria' && <AuditoriaPanel />}
    </Box>
  )
}
