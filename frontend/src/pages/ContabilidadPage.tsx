import { useState } from 'react'
import { MenuItem } from '@mui/material'
import AsientosTab from '../features/contabilidad/AsientosTab'
import BalancesTab from '../features/contabilidad/BalancesTab'
import CatalogosTab from '../features/contabilidad/CatalogosTab'
import ImpuestosTab from '../features/contabilidad/ImpuestosTab'
import LibrosTab from '../features/contabilidad/LibrosTab'
import PeriodosTab from '../features/contabilidad/PeriodosTab'
import SectionCard from '../components/ui/SectionCard'
import { useEmpresaActiva } from '../features/auth/useEmpresaActiva'

export default function ContabilidadPage() {
  const [tab, setTab] = useState<'puc' | 'periodos' | 'asientos' | 'libros' | 'balances' | 'impuestos'>('puc')
  const { empresaId, cargando: cargandoEmpresa } = useEmpresaActiva()

  const cambiarTab = (nuevoTab: 'puc' | 'periodos' | 'asientos' | 'libros' | 'balances' | 'impuestos') => setTab(nuevoTab)

  return (
    <SectionCard>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2>Contabilidad</h2>
        <MenuItem onClick={() => cambiarTab('asientos')}>Asientos</MenuItem>
        <MenuItem onClick={() => cambiarTab('puc')}>PUC</MenuItem>
        <MenuItem onClick={() => cambiarTab('periodos')}>Periodos</MenuItem>
        <MenuItem onClick={() => cambiarTab('libros')}>Libros</MenuItem>
        <MenuItem onClick={() => cambiarTab('balances')}>Balances</MenuItem>
        <MenuItem onClick={() => cambiarTab('impuestos')}>Impuestos</MenuItem>
      </div>
      {tab === 'puc' && <CatalogosTab titulo="Plan único de cuentas" descripcion="Catálogo contable base de la empresa" />}
      {tab === 'periodos' && <PeriodosTab empresaId={empresaId} cargandoEmpresa={cargandoEmpresa} />}
      {tab === 'asientos' && <AsientosTab empresaId={empresaId} cargandoEmpresa={cargandoEmpresa} />}
      {tab === 'libros' && <LibrosTab empresaId={empresaId} cargandoEmpresa={cargandoEmpresa} />}
      {tab === 'balances' && <BalancesTab empresaId={empresaId} cargandoEmpresa={cargandoEmpresa} />}
      {tab === 'impuestos' && <ImpuestosTab empresaId={empresaId} cargandoEmpresa={cargandoEmpresa} />}
    </SectionCard>
  )
}
