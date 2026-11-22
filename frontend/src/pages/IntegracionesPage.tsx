import { useCallback, useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Stack from '@mui/material/Stack'
import EmptyState from '../components/ui/EmptyState'
import LoadingState from '../components/ui/LoadingState'
import PageHeader from '../components/ui/PageHeader'
import SectionCard from '../components/ui/SectionCard'
import IntegracionesStatusPanel from '../components/integraciones/IntegracionesStatusPanel'
import MedioPagoSelector, { type CamposMetodoPago } from '../components/integraciones/MedioPagoSelector'
import { useEmpresaActiva } from '../features/auth/useEmpresaActiva'
import {
  guardarIntegracion,
  listarEventosIntegracion,
  listarIntegraciones,
  probarIntegracion,
  reintentarEventoIntegracion,
  sincronizarIntegracion,
  type IntegracionConfig,
  type IntegracionConfigRequest,
  type IntegracionEvento,
  type MetodoPago,
  type ProveedorIntegracion,
  type TipoSincronizacionIntegracion,
} from '../api/client'

type Mensaje = { tipo: 'success' | 'error'; texto: string }

const CAMPOS_VACIOS: CamposMetodoPago = {
  celular: '',
  financialInstitutionId: '',
  paymentType: '',
  documentType: '',
  documentNumber: '',
  payerName: '',
}

export default function IntegracionesPage({ ocultarEncabezado = false }: { ocultarEncabezado?: boolean }) {
  const { empresaId, cargando: cargandoEmpresa } = useEmpresaActiva()
  const [configuraciones, setConfiguraciones] = useState<IntegracionConfig[]>([])
  const [eventos, setEventos] = useState<IntegracionEvento[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState<Mensaje | null>(null)
  const [webhook, setWebhook] = useState<{ proveedor: ProveedorIntegracion; token: string } | null>(null)
  const [accion, setAccion] = useState<string | null>(null)
  const [sincronizando, setSincronizando] = useState<ProveedorIntegracion | null>(null)
  const [metodo, setMetodo] = useState<MetodoPago>('CARD')
  const [campos, setCampos] = useState<CamposMetodoPago>(CAMPOS_VACIOS)

  const cargar = useCallback(
    async (mostrarCarga = true) => {
      if (empresaId == null) {
        setCargando(false)
        return
      }
      if (mostrarCarga) setCargando(true)
      setError(null)
      try {
        const [configs, eventosRecientes] = await Promise.all([
          listarIntegraciones(empresaId),
          listarEventosIntegracion(empresaId),
        ])
        setConfiguraciones(configs)
        setEventos(eventosRecientes)
      } catch {
        setError('No se pudo cargar la información de integraciones.')
      } finally {
        if (mostrarCarga) setCargando(false)
      }
    },
    [empresaId],
  )

  useEffect(() => {
    if (!cargandoEmpresa) void cargar()
  }, [cargandoEmpresa, cargar])

  const onGuardar = async (proveedor: ProveedorIntegracion, solicitud: IntegracionConfigRequest) => {
    if (empresaId == null) return
    setAccion(`guardar:${proveedor}`)
    setMensaje(null)
    setWebhook(null)
    try {
      const guardada = await guardarIntegracion(empresaId, proveedor, solicitud)
      setWebhook(guardada.webhookToken ? { proveedor, token: guardada.webhookToken } : null)
      setMensaje({ tipo: 'success', texto: `Configuración de ${proveedor} guardada.` })
      await cargar(false)
    } catch {
      setMensaje({ tipo: 'error', texto: `No se pudo guardar la integración ${proveedor}.` })
    } finally {
      setAccion(null)
    }
  }

  const onProbar = async (proveedor: ProveedorIntegracion) => {
    if (empresaId == null) return
    setAccion(`probar:${proveedor}`)
    setMensaje(null)
    try {
      const resultado = await probarIntegracion(empresaId, proveedor)
      setMensaje({
        tipo: resultado.exitoso ? 'success' : 'error',
        texto: resultado.mensaje || `Prueba de ${proveedor} finalizada.`,
      })
      await cargar(false)
    } catch {
      setMensaje({ tipo: 'error', texto: `No se pudo probar la integración ${proveedor}.` })
    } finally {
      setAccion(null)
    }
  }

  const onSincronizar = async (proveedor: ProveedorIntegracion, tipo: TipoSincronizacionIntegracion) => {
    if (empresaId == null) return
    setAccion(`sincronizar:${proveedor}`)
    setSincronizando(proveedor)
    setMensaje(null)
    try {
      const resultado = await sincronizarIntegracion(empresaId, proveedor, tipo)
      setMensaje({
        tipo: resultado.exitoso ? 'success' : 'error',
        texto: resultado.mensaje || `Sincronización de ${proveedor} finalizada.`,
      })
      await cargar(false)
    } catch {
      setMensaje({ tipo: 'error', texto: `No se pudo sincronizar ${proveedor}.` })
    } finally {
      setSincronizando(null)
      setAccion(null)
    }
  }

  const onReintentar = async (eventoId: number) => {
    if (empresaId == null) return
    setAccion(`reintentar:${eventoId}`)
    setMensaje(null)
    try {
      const actualizado = await reintentarEventoIntegracion(empresaId, eventoId)
      setEventos((previos) => previos.map((evento) => (evento.id === eventoId ? actualizado : evento)))
      setMensaje({
        tipo: actualizado.estado === 'ERROR' ? 'error' : 'success',
        texto: `Evento ${eventoId}: ${actualizado.estado}.`,
      })
    } catch {
      setMensaje({ tipo: 'error', texto: `No se pudo reintentar el evento ${eventoId}.` })
    } finally {
      setAccion(null)
    }
  }

  if (cargandoEmpresa || cargando) {
    return <LoadingState label="Cargando integraciones…" />
  }

  if (empresaId == null) {
    return (
      <Box component="section" sx={{ display: 'grid', gap: 3 }}>
        {!ocultarEncabezado && <PageHeader overline="Empresa" title="Integraciones" />}
        <EmptyState
          title="Sin empresa activa"
          description="No hay una empresa asociada a su usuario. Contacte al administrador para continuar."
        />
      </Box>
    )
  }

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      {!ocultarEncabezado && (
        <PageHeader
          overline="Empresa"
          title="Integraciones"
          subtitle="Conecte su contabilidad y sus canales de venta desde un solo lugar."
        />
      )}

      <Stack spacing={2}>
        {error && <Alert severity="error">{error}</Alert>}
        {mensaje && <Alert severity={mensaje.tipo}>{mensaje.texto}</Alert>}
      </Stack>

      <IntegracionesStatusPanel
        configuraciones={configuraciones}
        eventos={eventos}
        webhook={webhook}
        accion={accion}
        sincronizando={sincronizando}
        onGuardar={(proveedor, solicitud) => void onGuardar(proveedor, solicitud)}
        onProbar={(proveedor) => void onProbar(proveedor)}
        onSincronizar={(proveedor, tipo) => void onSincronizar(proveedor, tipo)}
        onReintentar={(eventoId) => void onReintentar(eventoId)}
      />

      <SectionCard
        title="Métodos de pago"
        subtitle="Seleccione el método que desea usar en el checkout de su suscripción."
      >
        <MedioPagoSelector
          metodo={metodo}
          onMetodoChange={setMetodo}
          campos={campos}
          onCamposChange={setCampos}
          titulo="Método de pago"
        />
      </SectionCard>
    </Box>
  )
}
