import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Typography from '@mui/material/Typography'
import { api, type MedioMagnetico, type Validacion, type TercerosResumen } from '../api/client'
import { useEmpresaActiva } from '../features/auth/useEmpresaActiva'

export default function ValidacionPage() {
  const { medioId } = useParams()
  const { empresaId } = useEmpresaActiva()
  const [medio, setMedio] = useState<MedioMagnetico | null>(null)
  const [validacion, setValidacion] = useState<Validacion | null>(null)
  const [terceros, setTerceros] = useState<TercerosResumen | null>(null)
  const [xml, setXml] = useState<string | null>(null)
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (medioId && empresaId != null) {
      api
        .get<MedioMagnetico[]>(`/mediomagnetico/empresa/${empresaId}`)
        .then((res) => {
          const encontrado = res.data.find((m) => m.id === Number(medioId))
          if (encontrado) setMedio(encontrado)
          return api.get<Validacion>(`/mediomagnetico/${medioId}/reporte`)
        })
        .then((res) => setValidacion(res.data))
        .catch(() => undefined)
    }
  }, [medioId, empresaId])

  useEffect(() => {
    if (medioId) {
      api.get<TercerosResumen>(`/mediomagnetico/${medioId}/terceros`)
        .then(r => setTerceros(r.data))
        .catch(() => undefined)
    }
  }, [medioId, validacion])

  const onValidar = async () => {
    if (!medioId) return
    setCargando(true)
    setError(null)
    try {
      const anio = medio?.anioGravable ?? 2025
      const { data } = await api.post<Validacion>(`/mediomagnetico/${medioId}/validar?anioGravable=${anio}`)
      setValidacion(data)
    } catch {
      setError('No se pudo ejecutar la validación.')
    } finally {
      setCargando(false)
    }
  }

  const onPresentar = async () => {
    if (!medioId) return
    setCargando(true)
    setError(null)
    try {
      const { data } = await api.post(`/mediomagnetico/${medioId}/presentar`)
      setMedio((prev) => prev ? { ...prev, estado: data.estado } : prev)
    } catch {
      setError('No se pudo marcar como presentado. Verifique que el medio esté validado.')
    } finally {
      setCargando(false)
    }
  }

  const onXml = async () => {
    if (!medioId) return
    setCargando(true)
    setError(null)
    try {
      const { data } = await api.post<string>(`/mediomagnetico/${medioId}/generar-xml`)
      setXml(data)
    } catch {
      setError('Debe resolver los errores antes de generar el XML.')
    } finally {
      setCargando(false)
    }
  }

  const onDescargarXml = async () => {
    if (!medioId) return
    const token = localStorage.getItem('magnetixdian_token')
    const res = await fetch(`/api/mediomagnetico/${medioId}/xml`, {
      headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    })
    if (!res.ok) return
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = window.document.createElement('a')
    a.href = url
    a.download = `medio-magnetico-${medio?.formato ?? 1001}-${medioId}.xml`
    a.click()
    URL.revokeObjectURL(url)
  }

  const onAplicarDv = async () => {
    if (!medioId) return
    const { data } = await api.post<{ corregidos: number }>(`/mediomagnetico/${medioId}/aplicar-dv`)
    alert(`${data.corregidos} registros corregidos.`)
    const res = await api.get<TercerosResumen>(`/mediomagnetico/${medioId}/terceros`)
    setTerceros(res.data)
  }

  const severidadColor = (s: string) =>
    s === 'ERROR' ? 'error' : s === 'ADVERTENCIA' ? 'warning' : 'info'

  const archivoXmlListo = validacion && validacion.erroresTotal === 0

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, alignItems: 'center', justifyContent: 'space-between' }}>
        <Typography variant="h4" component="h1">
          Validación{medio ? ` — ${medio.formato} (${medio.anioGravable})` : ''}
        </Typography>
        <Stack direction="row" spacing={1}>
          <Button variant="contained" onClick={onValidar} disabled={!medioId || cargando}>
            {cargando ? 'Validando…' : 'Ejecutar validación'}
          </Button>
          <Button variant="outlined" onClick={onXml} disabled={!medioId || cargando || !archivoXmlListo}>
            Generar XML
          </Button>
          <Button variant="outlined" onClick={onDescargarXml} disabled={!medioId || cargando || !xml}>
            Descargar XML
          </Button>
          <Button variant="outlined" color="success" onClick={onPresentar} disabled={!medioId || cargando || !archivoXmlListo}>
            Marcar presentado
          </Button>
        </Stack>
      </Box>

      {!medioId && (
        <Alert severity="info">Seleccione un medio magnético desde el panel para validar sus datos.</Alert>
      )}

      {validacion && (
        <Paper variant="outlined" sx={{ p: { xs: 2, md: 3 } }}>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ mb: 2 }}>
            <Chip color={validacion.erroresTotal > 0 ? 'error' : 'success'} label={`${validacion.erroresTotal} errores`} />
            <Chip color="warning" label={`${validacion.advertenciasTotal} advertencias`} />
            <Chip color="info" label={`${validacion.registrosValidados} registros validados`} />
            <Chip color="default" label={`Estado: ${medio?.estado ?? '—'}`} />
          </Stack>

          {validacion.errores.length === 0 && (
            <Typography variant="body2" color="text.secondary">
              Sin errores de validación en la última corrida.
            </Typography>
          )}

          {validacion.errores.length > 0 && (
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
                        <Chip size="small" color={severidadColor(e.severidad) as 'error' | 'warning' | 'info'} label={e.severidad} />
                      </TableCell>
                      <TableCell>{e.campo ?? '—'}</TableCell>
                      <TableCell>{e.mensaje}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </Paper>
      )}

      {terceros && (
        <Paper variant="outlined" sx={{ p: { xs: 2, md: 3 } }}>
          <Typography variant="h6" sx={{ mb: 1 }}>Terceros y normalización de DV</Typography>
          <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
            <Chip label={`${terceros.totalRegistros} registros`} />
            <Chip label={`${terceros.tercerosUnicos} terceros únicos`} />
            <Chip color={terceros.nitsConDvIncorrecto > 0 ? 'warning' : 'success'} label={`${terceros.nitsConDvIncorrecto} NIT con DV incorrecto`} />
            {terceros.nitsConDvIncorrecto > 0 && (
              <Button size="small" variant="outlined" color="warning" onClick={onAplicarDv}>Aplicar DV correctos</Button>
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
        </Paper>
      )}

      {error && <Alert severity="error">{error}</Alert>}

      {xml && (
        <Paper variant="outlined" sx={{ p: 2 }}>
          <Typography variant="subtitle1" sx={{ mb: 1 }}>
            XML generado
          </Typography>
          <Typography variant="caption" component="pre" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
            {xml}
          </Typography>
        </Paper>
      )}
    </Box>
  )
}