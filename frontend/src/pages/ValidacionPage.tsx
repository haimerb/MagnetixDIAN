import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Typography from '@mui/material/Typography'
import { api, type MedioMagnetico, type Validacion, type TercerosResumen } from '../api/client'
import { descargarArchivo } from '../api/descargas'
import LoadingState from '../components/ui/LoadingState'
import PageHeader from '../components/ui/PageHeader'
import SectionCard from '../components/ui/SectionCard'
import { useEmpresaActiva } from '../features/auth/useEmpresaActiva'
import { useNotificaciones } from '../features/notificaciones/contexto'

type AccionValidacion = 'validar' | 'xml' | 'descargar' | 'presentar' | 'dv'

export default function ValidacionPage() {
  const { medioId } = useParams()
  const { empresaId, cargando: cargandoEmpresa } = useEmpresaActiva()
  const { notificar } = useNotificaciones()
  const [medio, setMedio] = useState<MedioMagnetico | null>(null)
  const [validacion, setValidacion] = useState<Validacion | null>(null)
  const [terceros, setTerceros] = useState<TercerosResumen | null>(null)
  const [xml, setXml] = useState<string | null>(null)
  const [cargandoInicial, setCargandoInicial] = useState(true)
  const [accion, setAccion] = useState<AccionValidacion | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (cargandoEmpresa) return
    if (!medioId || empresaId == null) {
      setCargandoInicial(false)
      return
    }
    let activo = true
    setCargandoInicial(true)
    api
      .get<MedioMagnetico[]>(`/mediomagnetico/empresa/${empresaId}`)
      .then((res) => {
        if (!activo) return null
        const encontrado = res.data.find((m) => m.id === Number(medioId))
        if (encontrado) setMedio(encontrado)
        return api.get<Validacion>(`/mediomagnetico/${medioId}/reporte`)
      })
      .then((res) => {
        if (activo && res) setValidacion(res.data)
      })
      .catch(() => {
        if (activo) setError('No se pudo cargar la validación.')
      })
      .finally(() => {
        if (activo) setCargandoInicial(false)
      })
    return () => {
      activo = false
    }
  }, [medioId, empresaId, cargandoEmpresa])

  useEffect(() => {
    if (medioId) {
      api
        .get<TercerosResumen>(`/mediomagnetico/${medioId}/terceros`)
        .then((r) => setTerceros(r.data))
        .catch(() => undefined)
    }
  }, [medioId, validacion])

  const onValidar = async () => {
    if (!medioId) return
    setAccion('validar')
    setError(null)
    try {
      const anio = medio?.anioGravable ?? 2025
      const { data } = await api.post<Validacion>(`/mediomagnetico/${medioId}/validar?anioGravable=${anio}`)
      setValidacion(data)
    } catch {
      setError('No se pudo ejecutar la validación.')
    } finally {
      setAccion(null)
    }
  }

  const onPresentar = async () => {
    if (!medioId) return
    setAccion('presentar')
    setError(null)
    try {
      const { data } = await api.post(`/mediomagnetico/${medioId}/presentar`)
      setMedio((prev) => (prev ? { ...prev, estado: data.estado } : prev))
    } catch {
      setError('No se pudo marcar como presentado. Verifique que el medio esté validado.')
    } finally {
      setAccion(null)
    }
  }

  const onXml = async () => {
    if (!medioId) return
    setAccion('xml')
    setError(null)
    try {
      const { data } = await api.post<string>(`/mediomagnetico/${medioId}/generar-xml`)
      setXml(data)
    } catch {
      setError('Debe resolver los errores antes de generar el XML.')
    } finally {
      setAccion(null)
    }
  }

  const onDescargarXml = async () => {
    if (!medioId) return
    setAccion('descargar')
    setError(null)
    try {
      await descargarArchivo(
        `/api/mediomagnetico/${medioId}/xml`,
        `medio-magnetico-${medio?.formato ?? 1001}-${medioId}.xml`,
      )
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo descargar el XML.')
    } finally {
      setAccion(null)
    }
  }

  const onAplicarDv = async () => {
    if (!medioId) return
    setAccion('dv')
    setError(null)
    try {
      const { data } = await api.post<{ corregidos: number }>(`/mediomagnetico/${medioId}/aplicar-dv`)
      notificar(`${data.corregidos} registros corregidos.`, 'success')
      const res = await api.get<TercerosResumen>(`/mediomagnetico/${medioId}/terceros`)
      setTerceros(res.data)
    } catch {
      setError('No se pudieron aplicar los DV corregidos.')
    } finally {
      setAccion(null)
    }
  }

  const severidadColor = (s: string) =>
    s === 'ERROR' ? 'error' : s === 'ADVERTENCIA' ? 'warning' : 'info'

  const archivoXmlListo = validacion && validacion.erroresTotal === 0
  const ocupado = accion !== null

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="Paso 2 de 3"
        title={medio ? `Validación — Formato ${medio.formato} (${medio.anioGravable})` : 'Validación'}
        subtitle="Ejecute las reglas DIAN, corrija los hallazgos y genere el XML del formato."
        actions={
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <Button variant="contained" onClick={onValidar} disabled={!medioId || ocupado} loading={accion === 'validar'}>
              Ejecutar validación
            </Button>
            <Button
              variant="outlined"
              onClick={onXml}
              disabled={!medioId || ocupado || !archivoXmlListo}
              loading={accion === 'xml'}
            >
              Generar XML
            </Button>
            <Button
              variant="outlined"
              onClick={onDescargarXml}
              disabled={!medioId || ocupado || !xml}
              loading={accion === 'descargar'}
            >
              Descargar XML
            </Button>
            <Button
              variant="outlined"
              color="success"
              onClick={onPresentar}
              disabled={!medioId || ocupado || !archivoXmlListo}
              loading={accion === 'presentar'}
            >
              Marcar presentado
            </Button>
          </Stack>
        }
      />

      {!medioId && (
        <Alert severity="info">Seleccione un medio magnético desde el panel para validar sus datos.</Alert>
      )}

      {cargandoInicial && <LoadingState label="Cargando validación…" minHeight="30vh" />}

      {validacion && (
        <SectionCard title="Resultado de la validación">
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5} flexWrap="wrap" useFlexGap sx={{ mb: 2 }}>
            <Chip
              color={validacion.erroresTotal > 0 ? 'error' : 'success'}
              label={`${validacion.erroresTotal} errores`}
            />
            <Chip color="warning" label={`${validacion.advertenciasTotal} advertencias`} />
            <Chip color="info" label={`${validacion.registrosValidados} registros validados`} />
            <Chip variant="outlined" label={`Estado: ${medio?.estado ?? '—'}`} />
          </Stack>

          {validacion.errores.length === 0 ? (
            <Typography variant="body2" color="text.secondary">
              Sin errores de validación en la última corrida.
            </Typography>
          ) : (
            <TableContainer>
              <Table size="small" aria-label="Errores de validación">
                <TableHead>
                  <TableRow>
                    <TableCell>Línea</TableCell>
                    <TableCell>Severidad</TableCell>
                    <TableCell>Campo</TableCell>
                    <TableCell>Mensaje</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {validacion.errores.map((e, idx) => (
                    <TableRow key={`${e.linea}-${e.campo}-${idx}`}>
                      <TableCell>{e.linea ?? '—'}</TableCell>
                      <TableCell>
                        <Chip
                          size="small"
                          color={severidadColor(e.severidad) as 'error' | 'warning' | 'info'}
                          label={e.severidad}
                        />
                      </TableCell>
                      <TableCell>{e.campo ?? '—'}</TableCell>
                      <TableCell>{e.mensaje}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </SectionCard>
      )}

      {terceros && (
        <SectionCard title="Terceros y normalización de DV">
          <Stack direction="row" spacing={1.5} flexWrap="wrap" useFlexGap sx={{ mb: 2, alignItems: 'center' }}>
            <Chip label={`${terceros.totalRegistros} registros`} />
            <Chip label={`${terceros.tercerosUnicos} terceros únicos`} />
            <Chip
              color={terceros.nitsConDvIncorrecto > 0 ? 'warning' : 'success'}
              label={`${terceros.nitsConDvIncorrecto} NIT con DV incorrecto`}
            />
            {terceros.nitsConDvIncorrecto > 0 && (
              <Button
                size="small"
                variant="outlined"
                color="warning"
                onClick={onAplicarDv}
                disabled={ocupado}
                loading={accion === 'dv'}
              >
                Aplicar DV correctos
              </Button>
            )}
          </Stack>
          {terceros.sugerenciasDv.length > 0 && (
            <TableContainer>
              <Table size="small" aria-label="Sugerencias de DV">
                <TableHead>
                  <TableRow>
                    <TableCell>Línea</TableCell>
                    <TableCell>Tipo</TableCell>
                    <TableCell>Número</TableCell>
                    <TableCell>DV actual</TableCell>
                    <TableCell>DV esperado</TableCell>
                    <TableCell>Concepto</TableCell>
                    <TableCell>Valor pago</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {terceros.sugerenciasDv.map((s) => (
                    <TableRow key={s.operacionId}>
                      <TableCell>{s.linea}</TableCell>
                      <TableCell>{s.tipoDocumento}</TableCell>
                      <TableCell>{s.numero}</TableCell>
                      <TableCell>{s.dvActual ?? '—'}</TableCell>
                      <TableCell>{s.dvEsperado}</TableCell>
                      <TableCell>{s.concepto}</TableCell>
                      <TableCell>{s.valorPago?.toLocaleString('es-CO')}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </SectionCard>
      )}

      {error && <Alert severity="error">{error}</Alert>}

      {xml && (
        <SectionCard title="XML generado" padded={false}>
          <Box
            component="pre"
            sx={{
              m: 0,
              p: { xs: 2, md: 3 },
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-word',
              fontFamily: '"JetBrains Mono", monospace',
              fontSize: '0.8125rem',
              bgcolor: (t) => t.palette.action.hover,
              borderRadius: '0 0 10px 10px',
            }}
          >
            {xml}
          </Box>
        </SectionCard>
      )}
    </Box>
  )
}
