import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid2'
import MenuItem from '@mui/material/MenuItem'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import {
  listarExistenciasErp,
  listarInventarioErp,
  registrarMovimientoInventarioErp,
} from '../../api/erp/erp'
import type { ExistenciaInventarioErp, MovimientoInventarioErp } from '../../api/erp/erp'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import { useEmpresaActiva } from '../../features/auth/useEmpresaActiva'
import { money } from '../../features/facturacion/calculos'
import TablaDatos from '../../features/erp/TablaDatos'
import type { ColumnaDatos } from '../../features/erp/TablaDatos'
import { useRecursoEmpresa } from '../../features/erp/useRecursoEmpresa'

const hoy = () => new Date().toISOString().slice(0, 10)

const columnas: ColumnaDatos<MovimientoInventarioErp>[] = [
  {
    id: 'producto',
    label: 'Producto',
    render: (fila) => <Typography variant="body2">{fila.descripcion || fila.productoId}</Typography>,
  },
  { id: 'bodega', label: 'Bodega', render: (fila) => <Typography variant="body2">{fila.bodega}</Typography> },
  { id: 'tipo', label: 'Tipo', render: (fila) => <Typography variant="body2">{fila.tipo}</Typography> },
  { id: 'cantidad', label: 'Cantidad', align: 'right', render: (fila) => <Typography variant="body2">{fila.cantidad}</Typography> },
  { id: 'saldo', label: 'Saldo', align: 'right', render: (fila) => <Typography variant="body2">{fila.existenciaNueva}</Typography> },
  { id: 'fecha', label: 'Fecha', render: (fila) => <Typography variant="body2">{fila.fecha}</Typography> },
]

const columnasExistencias: ColumnaDatos<ExistenciaInventarioErp>[] = [
  { id: 'producto', label: 'Producto', render: (fila) => <Typography variant="body2">{fila.productoId}</Typography> },
  { id: 'bodega', label: 'Bodega', render: (fila) => <Typography variant="body2">{fila.bodega}</Typography> },
  { id: 'existencia', label: 'Existencia', align: 'right', render: (fila) => <Typography variant="body2">{fila.existencia}</Typography> },
  { id: 'costo', label: 'Costo promedio', align: 'right', render: (fila) => <Typography variant="body2">{money(fila.costoPromedio)}</Typography> },
]

export default function InventarioPage() {
  const { empresaId } = useEmpresaActiva()
  const movimientos = useRecursoEmpresa(empresaId, listarInventarioErp)
  const existencias = useRecursoEmpresa(empresaId, listarExistenciasErp)

  const [tipo, setTipo] = useState<'ENTRADA' | 'SALIDA'>('ENTRADA')
  const [bodega, setBodega] = useState('Principal')
  const [productoId, setProductoId] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [cantidad, setCantidad] = useState('1')
  const [costoUnitario, setCostoUnitario] = useState('1000000')
  const [fecha, setFecha] = useState(hoy)
  const [referenciaTipo, setReferenciaTipo] = useState('')
  const [referenciaId, setReferenciaId] = useState('')
  const [registrando, setRegistrando] = useState(false)
  const [resultado, setResultado] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const registrar = async () => {
    if (empresaId == null) return
    const cantidadValor = Number(cantidad)
    const costoValor = costoUnitario.trim() ? Number(costoUnitario) : null
    if (!bodega.trim() || !productoId.trim()) {
      setError('Bodega y producto son obligatorios.')
      return
    }
    if (!Number.isFinite(cantidadValor) || cantidadValor <= 0) {
      setError('La cantidad debe ser mayor que cero.')
      return
    }
    if (tipo === 'ENTRADA' && (costoValor == null || !Number.isFinite(costoValor) || costoValor < 0)) {
      setError('La entrada requiere un costo unitario válido.')
      return
    }
    setRegistrando(true)
    setError(null)
    setResultado(null)
    try {
      const movimiento = await registrarMovimientoInventarioErp(empresaId, {
        tipo,
        bodega: bodega.trim(),
        productoId: productoId.trim(),
        descripcion: descripcion.trim() || null,
        cantidad: cantidadValor,
        costoUnitario: costoValor,
        fecha,
        referenciaTipo: referenciaTipo.trim() || null,
        referenciaId: referenciaId.trim() || null,
        observaciones: null,
      })
      setResultado(`Movimiento ${movimiento.tipo} registrado. Saldo: ${movimiento.existenciaNueva}.`)
      movimientos.recargar()
      existencias.recargar()
    } catch {
      setError('No se pudo registrar el movimiento.')
    } finally {
      setRegistrando(false)
    }
  }

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="ERP"
        title="Inventario"
        subtitle="Registre entradas y salidas de bodega y consulte los saldos por producto."
      />
      {resultado && <Alert severity="success">{resultado}</Alert>}
      {error && <Alert severity="error">{error}</Alert>}

      <SectionCard title="Nuevo movimiento">
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField
              select
              fullWidth
              label="Tipo"
              value={tipo}
              onChange={(e) => setTipo(e.target.value as 'ENTRADA' | 'SALIDA')}
            >
              <MenuItem value="ENTRADA">Entrada</MenuItem>
              <MenuItem value="SALIDA">Salida</MenuItem>
            </TextField>
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
            <TextField
              fullWidth
              type="number"
              label="Costo unitario"
              value={costoUnitario}
              disabled={tipo === 'SALIDA'}
              onChange={(e) => setCostoUnitario(e.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField fullWidth type="date" label="Fecha" value={fecha} slotProps={{ inputLabel: { shrink: true } }} onChange={(e) => setFecha(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField fullWidth label="Tipo de referencia" value={referenciaTipo} onChange={(e) => setReferenciaTipo(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField fullWidth label="Referencia" value={referenciaId} onChange={(e) => setReferenciaId(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <Button variant="contained" loading={registrando} disabled={empresaId == null} onClick={() => void registrar()}>
              Registrar movimiento
            </Button>
          </Grid>
        </Grid>
      </SectionCard>

      <SectionCard title="Existencias por bodega">
        <TablaDatos
          columnas={columnasExistencias}
          datos={existencias.datos}
          clave={(fila) => `${fila.bodega}-${fila.productoId}`}
          cargando={existencias.cargando}
          error={existencias.error}
          mensajeVacio="Las existencias aparecerán aquí cuando registre movimientos."
        />
      </SectionCard>

      <SectionCard title="Movimientos de inventario">
        <TablaDatos
          columnas={columnas}
          datos={movimientos.datos}
          clave={(fila) => fila.id}
          cargando={movimientos.cargando}
          error={movimientos.error}
          mensajeVacio="Los movimientos de inventario aparecerán aquí cuando existan."
        />
      </SectionCard>
    </Box>
  )
}
