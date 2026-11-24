import { useState } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Typography from '@mui/material/Typography'
import {
  listarIntegraciones,
  listarPedidosErp,
  migrarContabilidad,
} from '../../api/client'
import type { IntegracionConfig, PedidoErp, ProveedorIntegracion } from '../../api/client'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import EstadoChip from '../../components/ui/EstadoChip'
import { useEmpresaActiva } from '../../features/auth/useEmpresaActiva'
import { money } from '../../features/facturacion/calculos'
import TablaDatos from '../../features/erp/TablaDatos'
import { useRecursoEmpresa } from '../../features/erp/useRecursoEmpresa'
import { useNotificaciones } from '../../features/notificaciones/contexto'

const columnasIntegracion = [
  {
    id: 'proveedor',
    label: 'Proveedor',
    render: (fila: IntegracionConfig) => <Typography variant="body2">{fila.proveedor}</Typography>,
  },
  {
    id: 'estado',
    label: 'Estado',
    render: (fila: IntegracionConfig) => <EstadoChip estado={fila.estado} />,
  },
  {
    id: 'capacidades',
    label: 'Capacidades',
    render: (fila: IntegracionConfig) => (
      <Typography variant="body2" color="text.secondary">{fila.capacidades.join(', ') || 'Sin capacidades'}</Typography>
    ),
  },
  {
    id: 'sincronizacion',
    label: 'Última sincronización',
    render: (fila: IntegracionConfig) => (
      <Typography variant="body2">{fila.ultimaSincronizacion ?? 'Nunca'}</Typography>
    ),
  },
]

const columnasPedidos = [
  { id: 'consecutivo', label: 'Pedido', render: (fila: PedidoErp) => <Typography variant="body2">{fila.consecutivo}</Typography> },
  { id: 'cliente', label: 'Cliente', render: (fila: PedidoErp) => <Typography variant="body2">{fila.cliente}</Typography> },
  { id: 'estado', label: 'Estado', render: (fila: PedidoErp) => <EstadoChip estado={fila.estado} /> },
  { id: 'total', label: 'Total', align: 'right' as const, render: (fila: PedidoErp) => <Typography variant="body2">{money(fila.total)}</Typography> },
  { id: 'fecha', label: 'Fecha', render: (fila: PedidoErp) => <Typography variant="body2">{fila.fecha}</Typography> },
]

export default function IntegracionesPage() {
  const { empresaId } = useEmpresaActiva()
  const { notificar } = useNotificaciones()
  const integraciones = useRecursoEmpresa(empresaId, listarIntegraciones)
  const pedidos = useRecursoEmpresa(empresaId, listarPedidosErp)
  const [migrando, setMigrando] = useState<ProveedorIntegracion | null>(null)
  const [mensaje, setMensaje] = useState<string | null>(null)

  const migrar = async (proveedor: ProveedorIntegracion) => {
    if (empresaId == null) return
    setMigrando(proveedor)
    setMensaje(null)
    try {
      await migrarContabilidad(empresaId, proveedor)
      setMensaje(`Migración contable iniciada para ${proveedor}.`)
      integraciones.recargar()
      notificar(`Migración contable iniciada para ${proveedor}.`, 'success')
    } catch {
      setMensaje(`No se pudo iniciar la migración contable de ${proveedor}.`)
    } finally {
      setMigrando(null)
    }
  }

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="ERP"
        title="Integraciones"
        subtitle="Configure proveedores, migre contabilidad y convierta pedidos en facturas."
        actions={
          <Button component={RouterLink} to="/app/facturacion/nueva" variant="contained">
            Nueva factura desde pedido
          </Button>
        }
      />
      {mensaje && <Alert severity="info">{mensaje}</Alert>}

      <SectionCard title="Proveedores conectados">
        <TablaDatos
          columnas={columnasIntegracion}
          datos={integraciones.datos}
          clave={(fila) => fila.slug}
          cargando={integraciones.cargando}
          error={integraciones.error}
          mensajeVacio="No hay integraciones configuradas para la empresa."
        />
        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mt: 2 }}>
          {integraciones.datos.map((integracion) => (
            <Button
              key={integracion.slug}
              size="small"
              variant="outlined"
              loading={migrando === integracion.proveedor}
              disabled={!integracion.configurada || migrando != null}
              onClick={() => void migrar(integracion.proveedor)}
            >
              Migrar contabilidad · {integracion.proveedor}
            </Button>
          ))}
        </Box>
      </SectionCard>

      <SectionCard title="Pedidos ERP">
        <TablaDatos
          columnas={columnasPedidos}
          datos={pedidos.datos}
          clave={(fila) => fila.id}
          cargando={pedidos.cargando}
          error={pedidos.error}
          mensajeVacio="Los pedidos del ERP aparecerán aquí cuando existan."
        />
      </SectionCard>
    </Box>
  )
}
