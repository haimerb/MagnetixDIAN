import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import { listarComprasErp } from '../../api/erp/erp'
import type { CompraErp } from '../../api/erp/erp'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import EstadoChip from '../../components/ui/EstadoChip'
import { useEmpresaActiva } from '../../features/auth/useEmpresaActiva'
import { money } from '../../features/facturacion/calculos'
import TablaDatos from '../../features/erp/TablaDatos'
import { useRecursoEmpresa } from '../../features/erp/useRecursoEmpresa'

const columnas = [
  { id: 'consecutivo', label: 'Consecutivo', render: (fila: CompraErp) => <Typography variant="body2">{fila.consecutivo}</Typography> },
  { id: 'proveedor', label: 'Proveedor', render: (fila: CompraErp) => <Typography variant="body2">{fila.proveedor}</Typography> },
  { id: 'estado', label: 'Estado', render: (fila: CompraErp) => <EstadoChip estado={fila.estado} /> },
  { id: 'total', label: 'Total', align: 'right' as const, render: (fila: CompraErp) => <Typography variant="body2">{money(fila.total)}</Typography> },
  { id: 'fecha', label: 'Fecha', render: (fila: CompraErp) => <Typography variant="body2">{fila.fecha}</Typography> },
]

export default function ComprasPage() {
  const { empresaId, cargando: cargandoEmpresa } = useEmpresaActiva()
  const { datos, cargando, error } = useRecursoEmpresa(empresaId, listarComprasErp)

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="ERP"
        title="Compras"
        subtitle="Consulte las compras registradas en el ERP y su estado contable."
      />
      {cargandoEmpresa && <Alert severity="info">Consultando empresa activa…</Alert>}
      <SectionCard title="Compras registradas">
        <TablaDatos
          columnas={columnas}
          datos={datos}
          clave={(fila) => fila.id}
          cargando={cargando}
          error={error}
          mensajeVacio="Las compras del ERP aparecerán aquí cuando existan."
        />
      </SectionCard>
    </Box>
  )
}
