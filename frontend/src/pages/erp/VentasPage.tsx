import { useMemo, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid2'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import {
  aprobarPedidoErp,
  crearOrdenVentaErp,
  crearPedidoErp,
  emitirFactura,
  listarPedidosErp,
} from '../../api/erp/erp'
import type { FacturaRequest, PedidoErp, ResultadoEmision } from '../../api/client'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import EstadoChip from '../../components/ui/EstadoChip'
import { useEmpresaActiva } from '../../features/auth/useEmpresaActiva'
import { money } from '../../features/facturacion/calculos'
import TablaDatos from '../../features/erp/TablaDatos'
import type { ColumnaDatos } from '../../features/erp/TablaDatos'
import { useRecursoEmpresa } from '../../features/erp/useRecursoEmpresa'

type Operacion = 'crear' | 'aprobar' | 'emitir'

export default function VentasPage() {
  const { empresaId, cargando: cargandoEmpresa } = useEmpresaActiva()
  const pedidos = useRecursoEmpresa(empresaId, listarPedidosErp)
  const [numero, setNumero] = useState('')
  const [clienteId, setClienteId] = useState('')
  const [clienteNombre, setClienteNombre] = useState('')
  const [fecha, setFecha] = useState(() => new Date().toISOString().slice(0, 10))
  const [subtotal, setSubtotal] = useState('1000000')
  const [impuestos, setImpuestos] = useState('190000')
  const [operacion, setOperacion] = useState<Operacion | null>(null)
  const [resultado, setResultado] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const total = useMemo(
    () => (Number(subtotal) || 0) + (Number(impuestos) || 0),
    [subtotal, impuestos],
  )

  const crear = async () => {
    if (empresaId == null) return
    const base = Number(subtotal)
    const iva = Number(impuestos)
    if (!numero.trim() || !clienteId.trim() || !clienteNombre.trim()) {
      setError('Número, documento y cliente son obligatorios.')
      return
    }
    if (!Number.isFinite(base) || base <= 0 || !Number.isFinite(iva) || iva < 0) {
      setError('Subtotal e impuestos deben ser valores válidos.')
      return
    }
    setOperacion('crear')
    setError(null)
    setResultado(null)
    try {
      const orden = await crearOrdenVentaErp(empresaId, {
        numero: numero.trim(),
        clienteId: clienteId.trim(),
        clienteNombre: clienteNombre.trim(),
        fecha,
        subtotal: base,
        impuestos: iva,
        total,
        moneda: 'COP',
        observaciones: 'Pedido creado desde la UI de Ventas.',
      })
      const pedido = await crearPedidoErp(empresaId, {
        numero: numero.trim(),
        ordenVentaId: orden.id,
        clienteId: clienteId.trim(),
        clienteNombre: clienteNombre.trim(),
        fecha,
        subtotal: base,
        impuestos: iva,
        total,
        observaciones: null,
      })
      setResultado(`Pedido ${pedido.consecutivo} creado.`)
      pedidos.recargar()
    } catch {
      setError('No se pudo crear el pedido.')
    } finally {
      setOperacion(null)
    }
  }

  const aprobar = async (pedido: PedidoErp) => {
    if (empresaId == null) return
    setOperacion('aprobar')
    setError(null)
    setResultado(null)
    try {
      const actualizado = await aprobarPedidoErp(empresaId, pedido.id)
      setResultado(`Pedido ${actualizado.consecutivo} aprobado.`)
      pedidos.recargar()
    } catch {
      setError('No se pudo aprobar el pedido.')
    } finally {
      setOperacion(null)
    }
  }

  const emitir = async (pedido: PedidoErp) => {
    if (empresaId == null) return
    const ivaPct = pedido.subtotal > 0 ? (pedido.impuestos / pedido.subtotal) * 100 : 0
    const solicitud: FacturaRequest = {
      tipoDocumento: '01',
      adquirente: {
        tipoDocumento: '31',
        numeroDocumento: pedido.clienteId ?? '',
        digitoVerificacion: null,
        nombre: pedido.cliente,
        email: null,
        direccion: null,
        municipio: null,
        regimen: null,
        responsabilidadFiscal: null,
      },
      lineas: [{
        codigoProducto: null,
        descripcion: `Venta desde pedido ${pedido.consecutivo}`,
        cantidad: 1,
        unidadMedida: '94',
        valorUnitario: pedido.subtotal,
        descuento: 0,
        ivaPct,
        incPct: 0,
        codigoUnspsc: null,
      }],
      observaciones: `Pedido ${pedido.consecutivo}`,
      formaPago: '1',
      medioPago: '10',
      tipoOperacion: '10',
      fechaVencimiento: null,
      cufeReferencia: null,
      numeroReferencia: null,
      pedidoId: pedido.id,
    }
    setOperacion('emitir')
    setError(null)
    setResultado(null)
    try {
      const emision: ResultadoEmision = await emitirFactura(empresaId, solicitud)
      if (!emision.exitoso) {
        setError(emision.mensaje ?? 'La DIAN rechazó la factura.')
        return
      }
      setResultado(`Factura ${emision.numeroCompleto} vinculada al pedido ${pedido.consecutivo}.`)
      pedidos.recargar()
    } catch {
      setError('No se pudo emitir la factura del pedido.')
    } finally {
      setOperacion(null)
    }
  }

  const columnas: ColumnaDatos<PedidoErp>[] = [
    { id: 'consecutivo', label: 'Pedido', render: (fila) => <Typography variant="body2">{fila.consecutivo}</Typography> },
    { id: 'cliente', label: 'Cliente', render: (fila) => <Typography variant="body2">{fila.cliente}</Typography> },
    { id: 'estado', label: 'Estado', render: (fila) => <EstadoChip estado={fila.estado} /> },
    { id: 'total', label: 'Total', align: 'right', render: (fila) => <Typography variant="body2">{money(fila.total)}</Typography> },
    {
      id: 'factura',
      label: 'Factura DIAN',
      render: (fila) => <Typography variant="body2">{fila.facturaDianId ?? 'Sin factura'}</Typography>,
    },
    { id: 'fecha', label: 'Fecha', render: (fila) => <Typography variant="body2">{fila.fecha}</Typography> },
    {
      id: 'acciones',
      label: 'Acciones',
      render: (fila) => (
        <Stack direction="row" spacing={1}>
          <Button
            size="small"
            variant="outlined"
            loading={operacion === 'aprobar'}
            disabled={fila.estado !== 'BORRADOR' || operacion !== null}
            onClick={() => void aprobar(fila)}
          >
            Aprobar pedido
          </Button>
          <Button
            size="small"
            variant="contained"
            loading={operacion === 'emitir'}
            disabled={fila.estado !== 'APROBADO' || operacion !== null}
            onClick={() => void emitir(fila)}
          >
            Emitir factura
          </Button>
        </Stack>
      ),
    },
  ]

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="ERP"
        title="Ventas"
        subtitle="Cree pedidos, apruébelos y emita la factura DIAN vinculada al pedido."
      />
      {cargandoEmpresa && <Alert severity="info">Consultando empresa activa…</Alert>}
      {resultado && <Alert severity="success">{resultado}</Alert>}
      {error && <Alert severity="error">{error}</Alert>}

      <SectionCard title="Nuevo pedido" subtitle="El total se calcula automáticamente a partir del subtotal y los impuestos.">
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 6 }}>
            <TextField fullWidth label="Número" value={numero} onChange={(e) => setNumero(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
            <TextField
              fullWidth
              label="Documento cliente"
              value={clienteId}
              onChange={(e) => setClienteId(e.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
            <TextField fullWidth label="Cliente" value={clienteNombre} onChange={(e) => setClienteNombre(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
            <TextField fullWidth label="Fecha" type="date" value={fecha} onChange={(e) => setFecha(e.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField fullWidth label="Subtotal" type="number" value={subtotal} onChange={(e) => setSubtotal(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField fullWidth label="Impuestos" type="number" value={impuestos} onChange={(e) => setImpuestos(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField fullWidth label="Total" value={money(total)} slotProps={{ htmlInput: { readOnly: true } }} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <Button variant="contained" loading={operacion === 'crear'} disabled={empresaId == null} onClick={() => void crear()}>
              Crear pedido
            </Button>
          </Grid>
        </Grid>
      </SectionCard>

      <SectionCard title="Pedidos y facturación" subtitle="La emisión respeta la habilitación DIAN configurada para la empresa.">
        <TablaDatos
          columnas={columnas}
          datos={pedidos.datos}
          clave={(fila) => fila.id}
          cargando={pedidos.cargando}
          error={pedidos.error}
          mensajeVacio="Los pedidos creados desde el ERP aparecerán aquí."
        />
      </SectionCard>
    </Box>
  )
}
