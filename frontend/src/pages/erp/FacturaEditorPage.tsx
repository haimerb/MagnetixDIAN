import { useMemo, useRef, useState } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid2'
import IconButton from '@mui/material/IconButton'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import { api, buscarAsientoPorOrigen } from '../../api/client'
import type { AsientoContable, ErrorValidacion, FacturaRequest, ResultadoEmision } from '../../api/client'
import EmptyState from '../../components/ui/EmptyState'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import { useEmpresaActiva } from '../../features/auth/useEmpresaActiva'
import { money, totalesLinea } from '../../features/facturacion/calculos'
import { useHabilitacionDian } from '../../features/erp/useHabilitacionDian'
import { useNotificaciones } from '../../features/notificaciones/contexto'

type LineaEditor = {
  id: number
  descripcion: string
  cantidad: number
  valorUnitario: number
  ivaPct: number
}

function nuevaLinea(id: number): LineaEditor {
  return { id, descripcion: '', cantidad: 1, valorUnitario: 0, ivaPct: 19 }
}

export default function FacturaEditorPage() {
  const { empresaId, cargando: cargandoEmpresa } = useEmpresaActiva()
  const { puedeEmitir, cargando: cargandoHabilitacion } = useHabilitacionDian(empresaId)
  const { notificar } = useNotificaciones()
  const siguienteLinea = useRef(1)
  const [tipoDocumento, setTipoDocumento] = useState('01')
  const [numeroDocumento, setNumeroDocumento] = useState('')
  const [nombre, setNombre] = useState('')
  const [email, setEmail] = useState('')
  const [lineas, setLineas] = useState<LineaEditor[]>(() => [nuevaLinea(0)])
  const [errores, setErrores] = useState<ErrorValidacion[]>([])
  const [emision, setEmision] = useState<ResultadoEmision | null>(null)
  const [asiento, setAsiento] = useState<AsientoContable | null>(null)
  const [asientoPendiente, setAsientoPendiente] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [procesando, setProcesando] = useState(false)

  const totales = useMemo(
    () =>
      lineas.reduce(
        (acumulado, linea) => {
          const calculo = totalesLinea({
            cantidad: linea.cantidad,
            valorUnitario: linea.valorUnitario,
            descuento: 0,
            ivaPct: linea.ivaPct,
            incPct: 0,
          })
          return {
            subtotal: acumulado.subtotal + calculo.base,
            impuestos: acumulado.impuestos + calculo.iva,
            total: acumulado.total + calculo.total,
          }
        },
        { subtotal: 0, impuestos: 0, total: 0 },
      ),
    [lineas],
  )

  const construirSolicitud = (): FacturaRequest => ({
    tipoDocumento,
    adquirente: {
      tipoDocumento: '31',
      numeroDocumento,
      digitoVerificacion: null,
      nombre,
      email: email.trim() || null,
      direccion: null,
      municipio: null,
      regimen: null,
      responsabilidadFiscal: null,
    },
    lineas: lineas.map((linea) => ({
      codigoProducto: null,
      descripcion: linea.descripcion,
      cantidad: linea.cantidad,
      unidadMedida: '94',
      valorUnitario: linea.valorUnitario,
      descuento: 0,
      ivaPct: linea.ivaPct,
      incPct: 0,
      codigoUnspsc: null,
    })),
    observaciones: null,
    formaPago: '1',
    medioPago: '10',
    tipoOperacion: '10',
    fechaVencimiento: null,
    cufeReferencia: null,
    numeroReferencia: null,
    pedidoId: null,
  })

  const validar = async () => {
    if (empresaId == null) return
    setProcesando(true)
    setError(null)
    try {
      const { data } = await api.post<ErrorValidacion[]>(`/facturacion/${empresaId}/validar`, construirSolicitud())
      setErrores(data)
      notificar(data.length === 0 ? 'Documento válido.' : 'El documento tiene observaciones.', 'info')
    } catch {
      setError('No se pudo validar el documento.')
    } finally {
      setProcesando(false)
    }
  }

  const emitir = async () => {
    if (empresaId == null || !puedeEmitir) return
    setProcesando(true)
    setError(null)
    setErrores([])
    setAsiento(null)
    setAsientoPendiente(false)
    try {
      const { data } = await api.post<ResultadoEmision>(`/facturacion/${empresaId}/emitir`, construirSolicitud())
      setEmision(data)
      if (!data.exitoso) {
        setErrores(data.errores)
        setError(data.mensaje ?? 'La DIAN rechazó el documento.')
        return
      }
      if (data.id != null) {
        const asientoGenerado = await buscarAsientoPorOrigen(empresaId, 'FACTURA', data.id, 'VENTA')
        if (asientoGenerado) {
          setAsiento(asientoGenerado)
        } else {
          setAsientoPendiente(true)
        }
      }
    } catch {
      setError('No se pudo emitir el documento.')
    } finally {
      setProcesando(false)
    }
  }

  if (cargandoEmpresa || cargandoHabilitacion) {
    return <EmptyState title="Preparando el editor…" description="Consultando empresa y habilitación DIAN." />
  }

  if (empresaId == null) {
    return <EmptyState title="Sin empresa activa" description="Asocie una empresa antes de emitir facturas." />
  }

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="Facturación"
        title="Nueva factura electrónica"
        subtitle="Emita su primera factura una vez completada la habilitación DIAN."
        actions={
          <Button component={RouterLink} to="/app/habilitacion-dian" variant="outlined">
            Ver habilitación DIAN
          </Button>
        }
      />

      <Alert severity={puedeEmitir ? 'success' : 'error'}>
        {puedeEmitir
          ? 'Habilitación en producción activa: emisión permitida.'
          : 'Emisión bloqueada: se requiere TEST_SET_APROBADO y switchover a producción.'}
      </Alert>
      {error && <Alert severity="error">{error}</Alert>}
      {errores.length > 0 && (
        <Alert severity="warning">
          <Box component="ul" sx={{ m: 0, pl: 2 }}>
            {errores.map((fallo, indice) => (
              <li key={`${fallo.codigo}-${indice}`}>{fallo.mensaje ?? fallo.codigo}</li>
            ))}
          </Box>
        </Alert>
      )}

      <SectionCard title="Adquirente">
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField select label="Tipo de documento" value={tipoDocumento} onChange={(evento) => setTipoDocumento(evento.target.value)} fullWidth>
              <MenuItem value="01">Factura de venta</MenuItem>
              <MenuItem value="91">Nota crédito</MenuItem>
              <MenuItem value="92">Nota débito</MenuItem>
            </TextField>
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField label="Número de documento" value={numeroDocumento} onChange={(evento) => setNumeroDocumento(evento.target.value)} fullWidth />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField label="Nombre / razón social" value={nombre} onChange={(evento) => setNombre(evento.target.value)} fullWidth />
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
            <TextField label="Correo electrónico" type="email" value={email} onChange={(evento) => setEmail(evento.target.value)} fullWidth />
          </Grid>
        </Grid>
      </SectionCard>

      <SectionCard
        title="Detalle de la factura"
        actions={
          <Button
            size="small"
            onClick={() => setLineas((actuales) => [...actuales, nuevaLinea(siguienteLinea.current++)])}
          >
            Agregar línea
          </Button>
        }
      >
        <Stack spacing={2}>
          {lineas.map((linea) => (
            <Grid container spacing={2} key={linea.id}>
              <Grid size={{ xs: 12, md: 5 }}>
                <TextField
                  label="Descripción"
                  value={linea.descripcion}
                  onChange={(evento) =>
                    setLineas((actuales) => actuales.map((item) => item.id === linea.id ? { ...item, descripcion: evento.target.value } : item))
                  }
                  fullWidth
                />
              </Grid>
              <Grid size={{ xs: 6, md: 2 }}>
                <TextField
                  label="Cantidad"
                  type="number"
                  value={linea.cantidad}
                  onChange={(evento) =>
                    setLineas((actuales) => actuales.map((item) => item.id === linea.id ? { ...item, cantidad: Number(evento.target.value) } : item))
                  }
                  fullWidth
                />
              </Grid>
              <Grid size={{ xs: 6, md: 2 }}>
                <TextField
                  label="Valor unitario"
                  type="number"
                  value={linea.valorUnitario}
                  onChange={(evento) =>
                    setLineas((actuales) => actuales.map((item) => item.id === linea.id ? { ...item, valorUnitario: Number(evento.target.value) } : item))
                  }
                  fullWidth
                />
              </Grid>
              <Grid size={{ xs: 6, md: 2 }}>
                <TextField
                  label="IVA %"
                  type="number"
                  value={linea.ivaPct}
                  onChange={(evento) =>
                    setLineas((actuales) => actuales.map((item) => item.id === linea.id ? { ...item, ivaPct: Number(evento.target.value) } : item))
                  }
                  fullWidth
                />
              </Grid>
              <Grid size={{ xs: 6, md: 1 }}>
                <IconButton
                  aria-label="Eliminar línea"
                  disabled={lineas.length === 1}
                  onClick={() => setLineas((actuales) => actuales.filter((item) => item.id !== linea.id))}
                >
                  <DeleteOutlineIcon />
                </IconButton>
              </Grid>
            </Grid>
          ))}
          <Stack direction="row" spacing={2} sx={{ justifyContent: 'flex-end' }}>
            <Typography variant="body2" color="text.secondary">Subtotal {money(totales.subtotal)}</Typography>
            <Typography variant="body2" color="text.secondary">Impuestos {money(totales.impuestos)}</Typography>
            <Typography variant="h6">Total {money(totales.total)}</Typography>
          </Stack>
        </Stack>
      </SectionCard>

      <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
        <Button variant="outlined" loading={procesando} onClick={() => void validar()}>Validar</Button>
        <Button variant="contained" loading={procesando} disabled={!puedeEmitir} onClick={() => void emitir()}>
          Emitir factura
        </Button>
      </Stack>

      {emision && (
        <SectionCard title="Resultado de la emisión">
          <Stack spacing={1}>
            <Typography>Estado: {emision.estado}</Typography>
            <Typography>Número: {emision.numeroCompleto ?? 'pendiente'}</Typography>
            <Typography>CUFE: {emision.cufe ?? 'pendiente'}</Typography>
            {emision.exitoso && (
              <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
                <Button component={RouterLink} to="/app/contabilidad?tab=asientos" variant="outlined">
                  Asientos contables
                </Button>
                <Button component={RouterLink} to="/app/erp/exogena" variant="outlined">
                  Generar exógena 1001
                </Button>
              </Stack>
            )}
            {asiento && (
              <Alert severity="success">
                Asiento {asiento.numero} generado automáticamente por {money(asiento.totalDebito)}.
              </Alert>
            )}
            {asientoPendiente && (
              <Alert severity="info">
                El asiento se generará automáticamente cuando exista un periodo contable abierto para la fecha de la factura.
              </Alert>
            )}
          </Stack>
        </SectionCard>
      )}
    </Box>
  )
}
