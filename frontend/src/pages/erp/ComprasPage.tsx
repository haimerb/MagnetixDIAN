import { useMemo, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid2'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import {
  crearCompraErp,
  listarComprasErp,
  listarRecepcionesErp,
  recibirCompraErp,
} from '../../api/erp/erp'
import type { CompraErp, RecepcionErp } from '../../api/erp/erp'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import EstadoChip from '../../components/ui/EstadoChip'
import { useEmpresaActiva } from '../../features/auth/useEmpresaActiva'
import { money } from '../../features/facturacion/calculos'
import TablaDatos from '../../features/erp/TablaDatos'
import type { ColumnaDatos } from '../../features/erp/TablaDatos'
import { useRecursoEmpresa } from '../../features/erp/useRecursoEmpresa'

const hoy = () => new Date().toISOString().slice(0, 10)

const columnasCompras: ColumnaDatos<CompraErp>[] = [
  { id: 'numero', label: 'Número', render: (fila) => <Typography variant="body2">{fila.numero}</Typography> },
  {
    id: 'proveedor',
    label: 'Proveedor',
    render: (fila) => <Typography variant="body2">{fila.proveedorNombre || fila.proveedorId || '—'}</Typography>,
  },
  { id: 'estado', label: 'Estado', render: (fila) => <EstadoChip estado={fila.estado} /> },
  { id: 'total', label: 'Total', align: 'right', render: (fila) => <Typography variant="body2">{money(fila.total)}</Typography> },
  { id: 'fecha', label: 'Fecha', render: (fila) => <Typography variant="body2">{fila.fecha}</Typography> },
]

const columnasRecepciones: ColumnaDatos<RecepcionErp>[] = [
  { id: 'numero', label: 'Recepción', render: (fila) => <Typography variant="body2">{fila.numero}</Typography> },
  { id: 'compra', label: 'Compra', render: (fila) => <Typography variant="body2">#{fila.compraId}</Typography> },
  { id: 'bodega', label: 'Bodega', render: (fila) => <Typography variant="body2">{fila.bodega}</Typography> },
  { id: 'estado', label: 'Estado', render: (fila) => <EstadoChip estado={fila.estado} /> },
  { id: 'total', label: 'Total', align: 'right', render: (fila) => <Typography variant="body2">{money(fila.total)}</Typography> },
  { id: 'fecha', label: 'Fecha', render: (fila) => <Typography variant="body2">{fila.fecha}</Typography> },
]

export default function ComprasPage() {
  const { empresaId, cargando: cargandoEmpresa } = useEmpresaActiva()
  const compras = useRecursoEmpresa(empresaId, listarComprasErp)
  const recepciones = useRecursoEmpresa(empresaId, listarRecepcionesErp)

  const [numero, setNumero] = useState('')
  const [proveedorId, setProveedorId] = useState('')
  const [proveedorNombre, setProveedorNombre] = useState('')
  const [fecha, setFecha] = useState(hoy)
  const [subtotal, setSubtotal] = useState('1000000')
  const [impuestos, setImpuestos] = useState('190000')
  const [compraId, setCompraId] = useState('')
  const [numeroRecepcion, setNumeroRecepcion] = useState('')
  const [bodega, setBodega] = useState('Principal')
  const [productoId, setProductoId] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [cantidad, setCantidad] = useState('1')
  const [costoUnitario, setCostoUnitario] = useState('1000000')
  const [operacion, setOperacion] = useState<'crear' | 'recibir' | null>(null)
  const [resultado, setResultado] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const total = useMemo(() => (Number(subtotal) || 0) + (Number(impuestos) || 0), [subtotal, impuestos])
  const compraSeleccionada = compraId || (compras.datos[0] ? String(compras.datos[0].id) : '')

  const crear = async () => {
    if (empresaId == null) return
    const base = Number(subtotal)
    const iva = Number(impuestos)
    if (!numero.trim() || !proveedorNombre.trim()) {
      setError('Número y proveedor son obligatorios.')
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
      const orden = await crearCompraErp(empresaId, {
        numero: numero.trim(),
        proveedorId: proveedorId.trim() || null,
        proveedorNombre: proveedorNombre.trim(),
        fecha,
        subtotal: base,
        impuestos: iva,
        total,
        observaciones: null,
      })
      setResultado(`Orden de compra ${orden.numero} creada.`)
      compras.recargar()
    } catch {
      setError('No se pudo crear la orden de compra.')
    } finally {
      setOperacion(null)
    }
  }

  const recibir = async () => {
    if (empresaId == null || !compraSeleccionada) return
    const cantidadValor = Number(cantidad)
    const costoValor = Number(costoUnitario)
    if (!numeroRecepcion.trim() || !bodega.trim() || !productoId.trim()) {
      setError('Número, bodega y producto son obligatorios.')
      return
    }
    if (!Number.isFinite(cantidadValor) || cantidadValor <= 0 || !Number.isFinite(costoValor) || costoValor < 0) {
      setError('Cantidad y costo unitario deben ser valores válidos.')
      return
    }
    setOperacion('recibir')
    setError(null)
    setResultado(null)
    try {
      const recepcion = await recibirCompraErp(empresaId, Number(compraSeleccionada), {
        numero: numeroRecepcion.trim(),
        fecha: hoy(),
        bodega: bodega.trim(),
        observaciones: null,
        lineas: [{
          productoId: productoId.trim(),
          descripcion: descripcion.trim() || null,
          cantidad: cantidadValor,
          costoUnitario: costoValor,
        }],
      })
      setResultado(`Recepción ${recepcion.numero} registrada.`)
      recepciones.recargar()
      compras.recargar()
    } catch {
      setError('No se pudo registrar la recepción.')
    } finally {
      setOperacion(null)
    }
  }

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="ERP"
        title="Compras"
        subtitle="Cree órdenes de compra, registre recepciones y consulte su estado contable."
      />
      {cargandoEmpresa && <Alert severity="info">Consultando empresa activa…</Alert>}
      {resultado && <Alert severity="success">{resultado}</Alert>}
      {error && <Alert severity="error">{error}</Alert>}

      <SectionCard title="Nueva orden de compra">
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField fullWidth label="Número" value={numero} onChange={(e) => setNumero(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField fullWidth label="Documento proveedor" value={proveedorId} onChange={(e) => setProveedorId(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField fullWidth label="Proveedor" value={proveedorNombre} onChange={(e) => setProveedorNombre(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField fullWidth type="date" label="Fecha" value={fecha} slotProps={{ inputLabel: { shrink: true } }} onChange={(e) => setFecha(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField fullWidth type="number" label="Subtotal" value={subtotal} onChange={(e) => setSubtotal(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField fullWidth type="number" label="Impuestos" value={impuestos} onChange={(e) => setImpuestos(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <Stack direction="row" spacing={2} alignItems="center">
              <Button variant="contained" loading={operacion === 'crear'} disabled={empresaId == null} onClick={() => void crear()}>
                Crear orden
              </Button>
              <Typography variant="body2" color="text.secondary">Total: {money(total)}</Typography>
            </Stack>
          </Grid>
        </Grid>
      </SectionCard>

      <SectionCard title="Órdenes registradas">
        <TablaDatos
          columnas={columnasCompras}
          datos={compras.datos}
          clave={(fila) => fila.id}
          cargando={compras.cargando}
          error={compras.error}
          mensajeVacio="Las órdenes creadas desde el ERP aparecerán aquí."
        />
      </SectionCard>

      <SectionCard title="Nueva recepción" subtitle="La recepción genera el movimiento de inventario correspondiente.">
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField
              select
              fullWidth
              label="Orden de compra"
              value={compraSeleccionada}
              onChange={(e) => setCompraId(e.target.value)}
            >
              {compras.datos.map((fila) => (
                <MenuItem key={fila.id} value={String(fila.id)}>{fila.numero}</MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField fullWidth label="Número de recepción" value={numeroRecepcion} onChange={(e) => setNumeroRecepcion(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField fullWidth label="Bodega" value={bodega} onChange={(e) => setBodega(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField fullWidth label="Producto" value={productoId} onChange={(e) => setProductoId(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField fullWidth label="Descripción" value={descripcion} onChange={(e) => setDescripcion(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 2 }}>
            <TextField fullWidth type="number" label="Cantidad" value={cantidad} onChange={(e) => setCantidad(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 2 }}>
            <TextField fullWidth type="number" label="Costo unitario" value={costoUnitario} onChange={(e) => setCostoUnitario(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <Button
              variant="contained"
              loading={operacion === 'recibir'}
              disabled={empresaId == null || !compraSeleccionada}
              onClick={() => void recibir()}
            >
              Registrar recepción
            </Button>
          </Grid>
        </Grid>
      </SectionCard>

      <SectionCard title="Recepciones registradas">
        <TablaDatos
          columnas={columnasRecepciones}
          datos={recepciones.datos}
          clave={(fila) => fila.id}
          cargando={recepciones.cargando}
          error={recepciones.error}
          mensajeVacio="Las recepciones aparecerán aquí cuando existan."
        />
      </SectionCard>
    </Box>
  )
}
