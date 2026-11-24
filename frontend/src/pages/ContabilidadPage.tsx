import { useState } from 'react'
import { MenuItem } from '@mui/material'
import AsientosTab from '../features/contabilidad/AsientosTab'
import CatalogosTab from '../features/contabilidad/CatalogosTab'
import ImpuestosTab from '../features/contabilidad/ImpuestosTab'
import LibrosTab from '../features/contabilidad/LibrosTab'
import SectionCard from '../components/ui/SectionCard'

export default function ContabilidadPage() {
  const [tab, setTab] = useState<'puc' | 'periodos' | 'asientos' | 'libros' | 'balances' | 'impuestos'>('puc')
  const [empresaId, setEmpresaId] = useState<number | null>(null)
  const [cargandoEmpresa, setCargandoEmpresa] = useState(true)

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
      {tab === 'periodos' && <CatalogosTab titulo="Periodos contables" descripcion="Control de apertura y cierre de periodos" />}
      {tab === 'asientos' && <AsientosTab empresaId={empresaId} cargandoEmpresa={cargandoEmpresa} />}
      {tab === 'libros' && <LibrosTab empresaId={empresaId} cargandoEmpresa={cargandoEmpresa} />}
      {tab === 'balances' && <LibrosTab empresaId={empresaId} cargandoEmpresa={cargandoEmpresa} />}
      {tab === 'impuestos' && <LibrosTab empresaId={empresaId} cargandoEmpresa={cargandoEmpresa} />}
    </SectionCard>
  )
}
