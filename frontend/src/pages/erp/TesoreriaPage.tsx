import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import { listarTesoreriaErp } from '../../api/erp/erp'
import type { MovimientoTesoreriaErp } from '../../api/erp/erp'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import { useEmpresaActiva } from '../../features/auth/useEmpresaActiva'
import { money } from '../../features/facturacion/calculos'
import TablaDatos from '../../features/erp/TablaDatos'
import { useRecursoEmpresa } from '../../features/erp/useRecursoEmpresa'

const columnas = [
  { id: 'cuenta', label: 'Cuenta', render: (fila: MovimientoTesoreriaErp) => <Typography variant="body2">{fila.cuenta}</Typography> },
  { id: 'descripcion', label: 'Descripción', render: (fila: MovimientoTesoreriaErp) => <Typography variant="body2">{fila.descripcion}</Typography> },
  { id: 'tipo', label: 'Tipo', render: (fila: MovimientoTesoreriaErp) => <Typography variant="body2">{fila.tipo}</Typography> },
  { id: 'valor', label: 'Valor', align: 'right' as const, render: (fila: MovimientoTesoreriaErp) => <Typography variant="body2">{money(fila.valor)}</Typography> },
  { id: 'saldo', label: 'Saldo', align: 'right' as const, render: (fila: MovimientoTesoreriaErp) => <Typography variant="body2">{money(fila.saldo)}</Typography> },
  { id: 'fecha', label: 'Fecha', render: (fila: MovimientoTesoreriaErp) => <Typography variant="body2">{fila.fecha}</Typography> },
]

export default function TesoreriaPage() {
  const { empresaId } = useEmpresaActiva()
  const { datos, cargando, error } = useRecursoEmpresa(empresaId, listarTesoreriaErp)

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="ERP"
        title="Tesorería"
        subtitle="Ingresos, egresos y saldos por cuenta."
      />
      <SectionCard title="Movimientos de tesorería">
        <TablaDatos
          columnas={columnas}
          datos={datos}
          clave={(fila) => fila.id}
          cargando={cargando}
          error={error}
          mensajeVacio="Los movimientos de tesorería aparecerán aquí cuando existan."
        />
      </SectionCard>
    </Box>
  )
}
