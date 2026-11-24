import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import { listarInventarioErp } from '../../api/erp/erp'
import type { MovimientoInventarioErp } from '../../api/erp/erp'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import { useEmpresaActiva } from '../../features/auth/useEmpresaActiva'
import TablaDatos from '../../features/erp/TablaDatos'
import { useRecursoEmpresa } from '../../features/erp/useRecursoEmpresa'

const columnas = [
  { id: 'producto', label: 'Producto', render: (fila: MovimientoInventarioErp) => <Typography variant="body2">{fila.producto}</Typography> },
  { id: 'bodega', label: 'Bodega', render: (fila: MovimientoInventarioErp) => <Typography variant="body2">{fila.bodega}</Typography> },
  { id: 'tipo', label: 'Tipo', render: (fila: MovimientoInventarioErp) => <Typography variant="body2">{fila.tipo}</Typography> },
  { id: 'cantidad', label: 'Cantidad', align: 'right' as const, render: (fila: MovimientoInventarioErp) => <Typography variant="body2">{fila.cantidad}</Typography> },
  { id: 'saldo', label: 'Saldo', align: 'right' as const, render: (fila: MovimientoInventarioErp) => <Typography variant="body2">{fila.saldo}</Typography> },
  { id: 'fecha', label: 'Fecha', render: (fila: MovimientoInventarioErp) => <Typography variant="body2">{fila.fecha}</Typography> },
]

export default function InventarioPage() {
  const { empresaId } = useEmpresaActiva()
  const { datos, cargando, error } = useRecursoEmpresa(empresaId, listarInventarioErp)

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="ERP"
        title="Inventario"
        subtitle="Movimientos de bodega y saldos por producto."
      />
      <SectionCard title="Movimientos de inventario">
        <TablaDatos
          columnas={columnas}
          datos={datos}
          clave={(fila) => fila.id}
          cargando={cargando}
          error={error}
          mensajeVacio="Los movimientos de inventario aparecerán aquí cuando existan."
        />
      </SectionCard>
    </Box>
  )
}
