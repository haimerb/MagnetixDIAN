import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid2'
import MenuItem from '@mui/material/MenuItem'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import {
  crearCuentaBancariaErp,
  listarCobrosClienteErp,
  listarCuentasBancariasErp,
  listarPagosProveedorErp,
  listarTesoreriaErp,
  registrarCobroClienteErp,
  registrarPagoProveedorErp,
} from '../../api/erp/erp'
import type {
  CuentaBancariaErp,
  MovimientoTesoreriaErp,
  PagoProveedorErp,
  CobroClienteErp,
} from '../../api/erp/erp'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import EstadoChip from '../../components/ui/EstadoChip'
import { useEmpresaActiva } from '../../features/auth/useEmpresaActiva'
import { money } from '../../features/facturacion/calculos'
import TablaDatos from '../../features/erp/TablaDatos'
import type { ColumnaDatos } from '../../features/erp/TablaDatos'
import { useRecursoEmpresa } from '../../features/erp/useRecursoEmpresa'

const hoy = () => new Date().toISOString().slice(0, 10)

const columnasMovimientos: ColumnaDatos<MovimientoTesoreriaErp>[] = [
  { id: 'cuenta', label: 'Cuenta', render: (fila) => <Typography variant="body2">#{fila.cuentaBancariaId}</Typography> },
  { id: 'descripcion', label: 'Descripción', render: (fila) => <Typography variant="body2">{fila.descripcion || '—'}</Typography> },
  { id: 'tipo', label: 'Tipo', render: (fila) => <Typography variant="body2">{fila.tipo}</Typography> },
  { id: 'valor', label: 'Valor', align: 'right', render: (fila) => <Typography variant="body2">{money(fila.valor)}</Typography> },
  { id: 'saldo', label: 'Saldo', align: 'right', render: (fila) => <Typography variant="body2">{money(fila.saldoNuevo)}</Typography> },
  { id: 'fecha', label: 'Fecha', render: (fila) => <Typography variant="body2">{fila.fecha}</Typography> },
]

const columnasCuentas: ColumnaDatos<CuentaBancariaErp>[] = [
  { id: 'banco', label: 'Banco', render: (fila) => <Typography variant="body2">{fila.banco}</Typography> },
  { id: 'numero', label: 'Número', render: (fila) => <Typography variant="body2">{fila.numeroCuenta}</Typography> },
  { id: 'tipo', label: 'Tipo', render: (fila) => <Typography variant="body2">{fila.tipoCuenta}</Typography> },
  { id: 'moneda', label: 'Moneda', render: (fila) => <Typography variant="body2">{fila.moneda}</Typography> },
  { id: 'saldo', label: 'Saldo', align: 'right', render: (fila) => <Typography variant="body2">{money(fila.saldo)}</Typography> },
  { id: 'estado', label: 'Estado', render: (fila) => <EstadoChip estado={fila.estado} /> },
]

const columnasPagos: ColumnaDatos<PagoProveedorErp>[] = [
  { id: 'proveedor', label: 'Proveedor', render: (fila) => <Typography variant="body2">{fila.proveedorNombre || fila.proveedorId || '—'}</Typography> },
  { id: 'cuenta', label: 'Cuenta', render: (fila) => <Typography variant="body2">#{fila.cuentaBancariaId}</Typography> },
  { id: 'valor', label: 'Valor', align: 'right', render: (fila) => <Typography variant="body2">{money(fila.valor)}</Typography> },
  { id: 'estado', label: 'Estado', render: (fila) => <EstadoChip estado={fila.estado} /> },
  { id: 'fecha', label: 'Fecha', render: (fila) => <Typography variant="body2">{fila.fecha}</Typography> },
]

const columnasCobros: ColumnaDatos<CobroClienteErp>[] = [
  { id: 'cliente', label: 'Cliente', render: (fila) => <Typography variant="body2">{fila.clienteNombre || fila.clienteId || '—'}</Typography> },
  { id: 'cuenta', label: 'Cuenta', render: (fila) => <Typography variant="body2">#{fila.cuentaBancariaId}</Typography> },
  { id: 'valor', label: 'Valor', align: 'right', render: (fila) => <Typography variant="body2">{money(fila.valor)}</Typography> },
  { id: 'estado', label: 'Estado', render: (fila) => <EstadoChip estado={fila.estado} /> },
  { id: 'fecha', label: 'Fecha', render: (fila) => <Typography variant="body2">{fila.fecha}</Typography> },
]

export default function TesoreriaPage() {
  const { empresaId } = useEmpresaActiva()
  const movimientos = useRecursoEmpresa(empresaId, listarTesoreriaErp)
  const cuentas = useRecursoEmpresa(empresaId, listarCuentasBancariasErp)
  const pagos = useRecursoEmpresa(empresaId, listarPagosProveedorErp)
  const cobros = useRecursoEmpresa(empresaId, listarCobrosClienteErp)

  const [banco, setBanco] = useState('')
  const [numeroCuenta, setNumeroCuenta] = useState('')
  const [tipoCuenta, setTipoCuenta] = useState('CORRIENTE')
  const [moneda, setMoneda] = useState('COP')
  const [saldoInicial, setSaldoInicial] = useState('0')
  const [cuentaId, setCuentaId] = useState('')
  const [compraId, setCompraId] = useState('')
  const [proveedorId, setProveedorId] = useState('')
  const [proveedorNombre, setProveedorNombre] = useState('')
  const [valorPago, setValorPago] = useState('100000')
  const [medioPagoPago, setMedioPagoPago] = useState('TRANSFERENCIA')
  const [pedidoId, setPedidoId] = useState('')
  const [clienteId, setClienteId] = useState('')
  const [clienteNombre, setClienteNombre] = useState('')
  const [valorCobro, setValorCobro] = useState('100000')
  const [medioPagoCobro, setMedioPagoCobro] = useState('TRANSFERENCIA')
  const [referencia, setReferencia] = useState('')
  const [operacion, setOperacion] = useState<'cuenta' | 'pago' | 'cobro' | null>(null)
  const [resultado, setResultado] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const cuentaSeleccionada = cuentaId || (cuentas.datos[0] ? String(cuentas.datos[0].id) : '')
  const cuentaActiva = cuentas.datos.find((cuenta) => String(cuenta.id) === cuentaSeleccionada && cuenta.activa)

  const crearCuenta = async () => {
    if (empresaId == null) return
    const saldo = Number(saldoInicial)
    if (!banco.trim() || !numeroCuenta.trim()) {
      setError('Banco y número de cuenta son obligatorios.')
      return
    }
    if (!Number.isFinite(saldo) || saldo < 0) {
      setError('El saldo inicial debe ser un valor válido.')
      return
    }
    setOperacion('cuenta')
    setError(null)
    setResultado(null)
    try {
      const cuenta = await crearCuentaBancariaErp(empresaId, {
        banco: banco.trim(),
        numeroCuenta: numeroCuenta.trim(),
        tipoCuenta: tipoCuenta.trim() || 'CORRIENTE',
        moneda: moneda.trim() || 'COP',
        saldoInicial: saldo,
        activa: true,
      })
      setResultado(`Cuenta ${cuenta.numeroCuenta} creada.`)
      cuentas.recargar()
    } catch {
      setError('No se pudo crear la cuenta bancaria.')
    } finally {
      setOperacion(null)
    }
  }

  const registrarPago = async () => {
    if (empresaId == null || !cuentaActiva) return
    const valor = Number(valorPago)
    if (!Number.isFinite(valor) || valor <= 0) {
      setError('El valor del pago debe ser mayor que cero.')
      return
    }
    setOperacion('pago')
    setError(null)
    setResultado(null)
    try {
      await registrarPagoProveedorErp(empresaId, {
        compraId: compraId.trim() ? Number(compraId) : null,
        cuentaBancariaId: cuentaActiva.id,
        proveedorId: proveedorId.trim() || null,
        proveedorNombre: proveedorNombre.trim() || null,
        fecha: hoy(),
        valor,
        medioPago: medioPagoPago.trim() || null,
        referencia: referencia.trim() || null,
        observaciones: null,
      })
      setResultado(`Pago a proveedor registrado por ${money(valor)}.`)
      pagos.recargar()
      cuentas.recargar()
      movimientos.recargar()
    } catch {
      setError('No se pudo registrar el pago a proveedor.')
    } finally {
      setOperacion(null)
    }
  }

  const registrarCobro = async () => {
    if (empresaId == null || !cuentaActiva) return
    const valor = Number(valorCobro)
    if (!Number.isFinite(valor) || valor <= 0) {
      setError('El valor del cobro debe ser mayor que cero.')
      return
    }
    setOperacion('cobro')
    setError(null)
    setResultado(null)
    try {
      await registrarCobroClienteErp(empresaId, {
        pedidoId: pedidoId.trim() ? Number(pedidoId) : null,
        cuentaBancariaId: cuentaActiva.id,
        clienteId: clienteId.trim() || null,
        clienteNombre: clienteNombre.trim() || null,
        fecha: hoy(),
        valor,
        medioPago: medioPagoCobro.trim() || null,
        referencia: referencia.trim() || null,
        observaciones: null,
      })
      setResultado(`Cobro a cliente registrado por ${money(valor)}.`)
      cobros.recargar()
      cuentas.recargar()
      movimientos.recargar()
    } catch {
      setError('No se pudo registrar el cobro a cliente.')
    } finally {
      setOperacion(null)
    }
  }

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="ERP"
        title="Tesorería"
        subtitle="Administre cuentas bancarias, pagos a proveedores, cobros a clientes y saldos."
      />
      {resultado && <Alert severity="success">{resultado}</Alert>}
      {error && <Alert severity="error">{error}</Alert>}

      <SectionCard title="Nueva cuenta bancaria">
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField fullWidth label="Banco" value={banco} onChange={(e) => setBanco(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField fullWidth label="Número de cuenta" value={numeroCuenta} onChange={(e) => setNumeroCuenta(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 2 }}>
            <TextField fullWidth label="Tipo" value={tipoCuenta} onChange={(e) => setTipoCuenta(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 2 }}>
            <TextField fullWidth label="Moneda" value={moneda} onChange={(e) => setMoneda(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField fullWidth type="number" label="Saldo inicial" value={saldoInicial} onChange={(e) => setSaldoInicial(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <Button variant="contained" loading={operacion === 'cuenta'} disabled={empresaId == null} onClick={() => void crearCuenta()}>
              Crear cuenta
            </Button>
          </Grid>
        </Grid>
      </SectionCard>

      <SectionCard title="Cuentas bancarias">
        <TablaDatos
          columnas={columnasCuentas}
          datos={cuentas.datos}
          clave={(fila) => fila.id}
          cargando={cuentas.cargando}
          error={cuentas.error}
          mensajeVacio="Cree una cuenta bancaria para registrar pagos y cobros."
        />
      </SectionCard>

      <SectionCard title="Pago a proveedor">
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField
              select
              fullWidth
              label="Cuenta"
              value={cuentaSeleccionada}
              onChange={(e) => setCuentaId(e.target.value)}
            >
              {cuentas.datos.filter((cuenta) => cuenta.activa).map((cuenta) => (
                <MenuItem key={cuenta.id} value={String(cuenta.id)}>{cuenta.banco} · {cuenta.numeroCuenta}</MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid size={{ xs: 12, md: 2 }}>
            <TextField fullWidth type="number" label="Compra" value={compraId} onChange={(e) => setCompraId(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField fullWidth label="Documento proveedor" value={proveedorId} onChange={(e) => setProveedorId(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField fullWidth label="Proveedor" value={proveedorNombre} onChange={(e) => setProveedorNombre(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 2 }}>
            <TextField fullWidth type="number" label="Valor" value={valorPago} onChange={(e) => setValorPago(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField fullWidth label="Medio de pago" value={medioPagoPago} onChange={(e) => setMedioPagoPago(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField fullWidth label="Referencia" value={referencia} onChange={(e) => setReferencia(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <Button variant="contained" loading={operacion === 'pago'} disabled={!cuentaActiva} onClick={() => void registrarPago()}>
              Registrar pago
            </Button>
          </Grid>
        </Grid>
      </SectionCard>

      <SectionCard title="Cobro a cliente">
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField
              select
              fullWidth
              label="Cuenta"
              value={cuentaSeleccionada}
              onChange={(e) => setCuentaId(e.target.value)}
            >
              {cuentas.datos.filter((cuenta) => cuenta.activa).map((cuenta) => (
                <MenuItem key={cuenta.id} value={String(cuenta.id)}>{cuenta.banco} · {cuenta.numeroCuenta}</MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid size={{ xs: 12, md: 2 }}>
            <TextField fullWidth type="number" label="Pedido" value={pedidoId} onChange={(e) => setPedidoId(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField fullWidth label="Documento cliente" value={clienteId} onChange={(e) => setClienteId(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField fullWidth label="Cliente" value={clienteNombre} onChange={(e) => setClienteNombre(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 2 }}>
            <TextField fullWidth type="number" label="Valor" value={valorCobro} onChange={(e) => setValorCobro(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField fullWidth label="Medio de pago" value={medioPagoCobro} onChange={(e) => setMedioPagoCobro(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <Button variant="contained" loading={operacion === 'cobro'} disabled={!cuentaActiva} onClick={() => void registrarCobro()}>
              Registrar cobro
            </Button>
          </Grid>
        </Grid>
      </SectionCard>

      <SectionCard title="Movimientos de tesorería">
        <TablaDatos
          columnas={columnasMovimientos}
          datos={movimientos.datos}
          clave={(fila) => fila.id}
          cargando={movimientos.cargando}
          error={movimientos.error}
          mensajeVacio="Los movimientos aparecerán aquí cuando registre pagos o cobros."
        />
      </SectionCard>

      <SectionCard title="Pagos a proveedores">
        <TablaDatos
          columnas={columnasPagos}
          datos={pagos.datos}
          clave={(fila) => fila.id}
          cargando={pagos.cargando}
          error={pagos.error}
          mensajeVacio="Los pagos registrados aparecerán aquí."
        />
      </SectionCard>

      <SectionCard title="Cobros a clientes">
        <TablaDatos
          columnas={columnasCobros}
          datos={cobros.datos}
          clave={(fila) => fila.id}
          cargando={cobros.cargando}
          error={cobros.error}
          mensajeVacio="Los cobros registrados aparecerán aquí."
        />
      </SectionCard>
    </Box>
  )
}
