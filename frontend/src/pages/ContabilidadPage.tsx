import { useCallback, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Tab from '@mui/material/Tab'
import Tabs from '@mui/material/Tabs'
import AccountTreeOutlinedIcon from '@mui/icons-material/AccountTreeOutlined'
import BalanceOutlinedIcon from '@mui/icons-material/BalanceOutlined'
import CalculateOutlinedIcon from '@mui/icons-material/CalculateOutlined'
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined'
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined'
import FactCheckOutlinedIcon from '@mui/icons-material/FactCheckOutlined'
import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined'
import PlayArrowOutlinedIcon from '@mui/icons-material/PlayArrowOutlined'
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined'
import { Link as RouterLink, useSearchParams } from 'react-router-dom'
import { conciliarContabilidad, type FiltrosConciliacion, type ResultadoConciliacion } from '../api/client'
import PageHeader from '../components/ui/PageHeader'
import { useEmpresaActiva } from '../features/auth/useEmpresaActiva'
import { useNotificaciones } from '../features/notificaciones/contexto'
import AsientosTab from '../features/contabilidad/AsientosTab'
import BalancesTab from '../features/contabilidad/BalancesTab'
import CatalogosTab from '../features/contabilidad/CatalogosTab'
import ConciliacionTab from '../features/contabilidad/ConciliacionTab'
import ExogenaTab from '../features/contabilidad/ExogenaTab'
import ImpuestosTab from '../features/contabilidad/ImpuestosTab'
import LibrosTab from '../features/contabilidad/LibrosTab'

const TABS = [
  { id: 'puc', label: 'PUC', titulo: 'Plan único de cuentas', icon: <AccountTreeOutlinedIcon /> },
  { id: 'periodos', label: 'Periodos', titulo: 'Periodos contables', icon: <CalendarMonthOutlinedIcon /> },
  { id: 'asientos', label: 'Asientos', titulo: 'Asientos contables', icon: <ReceiptLongOutlinedIcon /> },
  { id: 'libros', label: 'Libros', titulo: 'Libros contables', icon: <MenuBookOutlinedIcon /> },
  { id: 'balances', label: 'Balances', titulo: 'Balances y estados', icon: <BalanceOutlinedIcon /> },
  { id: 'impuestos', label: 'Impuestos', titulo: 'Impuestos', icon: <CalculateOutlinedIcon /> },
  { id: 'exogena', label: 'Exógena', titulo: 'Información exógena', icon: <DescriptionOutlinedIcon /> },
  { id: 'conciliacion', label: 'Conciliación', titulo: 'Conciliación detallada', icon: <FactCheckOutlinedIcon /> },
] as const

type TabId = (typeof TABS)[number]['id']

function esTabId(valor: string | null): valor is TabId {
  return valor != null && TABS.some((tab) => tab.id === valor)
}

export default function ContabilidadPage() {
  const { empresaId, cargando: cargandoEmpresa } = useEmpresaActiva()
  const { notificar } = useNotificaciones()
  const [searchParams, setSearchParams] = useSearchParams()
  const valorTab = searchParams.get('tab')
  const tab: TabId = esTabId(valorTab) ? valorTab : 'asientos'
  const [conciliacion, setConciliacion] = useState<ResultadoConciliacion | null>(null)
  const [conciliando, setConciliando] = useState(false)
  const [errorConciliacion, setErrorConciliacion] = useState<string | null>(null)

  const cambiarTab = useCallback(
    (siguiente: TabId) => {
      setSearchParams({ tab: siguiente }, { replace: true })
    },
    [setSearchParams],
  )

  const onConciliar = useCallback(
    async (filtros: FiltrosConciliacion = {}) => {
      if (empresaId == null) return
      cambiarTab('conciliacion')
      setConciliando(true)
      setErrorConciliacion(null)
      try {
        setConciliacion(await conciliarContabilidad(empresaId, filtros))
        notificar('Conciliación contable completada.', 'success')
      } catch {
        setErrorConciliacion('No se pudo completar la conciliación contable.')
      } finally {
        setConciliando(false)
      }
    },
    [cambiarTab, empresaId, notificar],
  )

  const titulo = TABS.find((item) => item.id === tab)?.titulo ?? 'Asientos contables'

  const contenido = () => {
    switch (tab) {
      case 'puc':
        return (
          <CatalogosTab
            titulo="Plan único de cuentas"
            descripcion="Catálogo contable base de la empresa"
            icono={<AccountTreeOutlinedIcon />}
            mensaje="El plan de cuentas se crea automáticamente al contabilizar. Aún no hay consulta HTTP expuesta."
          />
        )
      case 'periodos':
        return (
          <CatalogosTab
            titulo="Periodos contables"
            descripcion="Control de apertura y cierre de periodos"
            icono={<CalendarMonthOutlinedIcon />}
            mensaje="El cierre de periodos existe en el backend, pero todavía no hay un listado consultable."
          />
        )
      case 'asientos':
        return <AsientosTab empresaId={empresaId} cargandoEmpresa={cargandoEmpresa} />
      case 'libros':
        return <LibrosTab empresaId={empresaId} cargandoEmpresa={cargandoEmpresa} />
      case 'balances':
        return <BalancesTab empresaId={empresaId} cargandoEmpresa={cargandoEmpresa} />
      case 'impuestos':
        return <ImpuestosTab empresaId={empresaId} cargandoEmpresa={cargandoEmpresa} />
      case 'exogena':
        return <ExogenaTab empresaId={empresaId} cargandoEmpresa={cargandoEmpresa} />
      case 'conciliacion':
        return (
          <ConciliacionTab
            empresaId={empresaId}
            cargandoEmpresa={cargandoEmpresa}
            conciliacion={conciliacion}
            conciliando={conciliando}
            error={errorConciliacion}
            onConciliar={onConciliar}
          />
        )
    }
  }

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="Contabilidad"
        title={titulo}
        subtitle="Asiente la facturación de la DIAN, consulte libros y balances, calcule impuestos y genere exógena."
        actions={
          <>
            <Button
              component={RouterLink}
              to="/app/contabilidad/asentar-facturacion"
              variant="outlined"
              startIcon={<PlayArrowOutlinedIcon />}
            >
              Asentar facturación
            </Button>
            <Button
              variant="contained"
              startIcon={<FactCheckOutlinedIcon />}
              loading={conciliando}
              disabled={empresaId == null}
              onClick={() => onConciliar()}
            >
              Conciliar
            </Button>
          </>
        }
      />

      {empresaId == null && !cargandoEmpresa && (
        <Alert severity="info">No hay una empresa asociada para consultar la contabilidad.</Alert>
      )}

      <Tabs
        value={tab}
        onChange={(_, siguiente: TabId) => cambiarTab(siguiente)}
        variant="scrollable"
        scrollButtons="auto"
        aria-label="Secciones de contabilidad"
      >
        {TABS.map((item) => (
          <Tab key={item.id} value={item.id} label={item.label} icon={item.icon} iconPosition="start" />
        ))}
      </Tabs>

      {contenido()}
    </Box>
  )
}
