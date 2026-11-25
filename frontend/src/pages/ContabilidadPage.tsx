import Box from '@mui/material/Box'
import Tab from '@mui/material/Tab'
import Tabs from '@mui/material/Tabs'
import Typography from '@mui/material/Typography'
import { useSearchParams } from 'react-router-dom'
import AsientosTab from '../features/contabilidad/AsientosTab'
import BalancesTab from '../features/contabilidad/BalancesTab'
import CatalogosTab from '../features/contabilidad/CatalogosTab'
import ExogenaTab from '../features/contabilidad/ExogenaTab'
import ImpuestosTab from '../features/contabilidad/ImpuestosTab'
import LibrosTab from '../features/contabilidad/LibrosTab'
import PeriodosTab from '../features/contabilidad/PeriodosTab'
import { useEmpresaActiva } from '../features/auth/useEmpresaActiva'

const SECCIONES = [
  { value: 'asientos', label: 'Asientos', titulo: 'Asientos contables' },
  { value: 'puc', label: 'PUC', titulo: 'Plan único de cuentas' },
  { value: 'periodos', label: 'Periodos', titulo: 'Periodos contables' },
  { value: 'exogena', label: 'Exógena', titulo: 'Información exógena' },
  { value: 'libros', label: 'Libros', titulo: 'Libros contables' },
  { value: 'balances', label: 'Balances', titulo: 'Balances contables' },
  { value: 'impuestos', label: 'Impuestos', titulo: 'Impuestos' },
] as const

type Seccion = (typeof SECCIONES)[number]['value']

const VALORES = new Set<string>(SECCIONES.map((seccion) => seccion.value))

export default function ContabilidadPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const { empresaId, cargando: cargandoEmpresa } = useEmpresaActiva()
  const valorSolicitado = searchParams.get('tab') ?? 'asientos'
  const tab: Seccion = VALORES.has(valorSolicitado) ? (valorSolicitado as Seccion) : 'asientos'
  const titulo = SECCIONES.find((seccion) => seccion.value === tab)?.titulo ?? 'Asientos contables'

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <Typography component="h1" variant="h5">
        {titulo}
      </Typography>
      <Tabs
        value={tab}
        onChange={(_, valor) => setSearchParams({ tab: String(valor) })}
        variant="scrollable"
        allowScrollButtonsMobile
        aria-label="Secciones de contabilidad"
      >
        {SECCIONES.map((seccion) => (
          <Tab key={seccion.value} value={seccion.value} label={seccion.label} />
        ))}
      </Tabs>
      {tab === 'asientos' && <AsientosTab empresaId={empresaId} cargandoEmpresa={cargandoEmpresa} />}
      {tab === 'puc' && (
        <CatalogosTab titulo="Plan único de cuentas" descripcion="Catálogo sin consulta pública" />
      )}
      {tab === 'periodos' && <PeriodosTab empresaId={empresaId} cargandoEmpresa={cargandoEmpresa} />}
      {tab === 'exogena' && <ExogenaTab empresaId={empresaId} cargandoEmpresa={cargandoEmpresa} />}
      {tab === 'libros' && <LibrosTab empresaId={empresaId} cargandoEmpresa={cargandoEmpresa} />}
      {tab === 'balances' && <BalancesTab empresaId={empresaId} cargandoEmpresa={cargandoEmpresa} />}
      {tab === 'impuestos' && <ImpuestosTab empresaId={empresaId} cargandoEmpresa={cargandoEmpresa} />}
    </Box>
  )
}
