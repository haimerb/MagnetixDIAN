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
import { api } from '../api/client'
import type { Validacion } from '../api/client'

export default function ValidacionPage() {
  const { medioId } = useParams()
  const [validacion, setValidacion] = useState<Validacion | null>(null)
  const [xml, setXml] = useState<string | null>(null)
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (medioId) {
      api
        .get<Validacion>(`/mediomagnetico/${medioId}/reporte`)
        .then((res) => setValidacion(res.data))
        .catch(() => undefined)
    }
  }, [medioId])

  const onValidar = async () => {
    if (!medioId) return
    setCargando(true)
    setError(null)
    try {
      const { data } = await api.post<Validacion>(`/mediomagnetico/${medioId}/validar?anioGravable=2025`)
      setValidacion(data)
    } catch {
      setError('No se pudo ejecutar la validación.')
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
    a.download = `medio-magnetico-1001-${medioId}.xml`
    a.click()
    URL.revokeObjectURL(url)
  }

  const severidadColor = (s: string) =>
    s === 'ERROR' ? 'error' : s === 'ADVERTENCIA' ? 'warning' : 'info'

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, alignItems: 'center', justifyContent: 'space-between' }}>
        <Typography variant="h4" component="h1">
          Validación de datos
        </Typography>
        <Stack direction="row" spacing={1}>
          <Button variant="contained" onClick={onValidar} disabled={!medioId || cargando}>
            {cargando ? 'Validando…' : 'Ejecutar validación'}
          </Button>
          <Button variant="outlined" onClick={onXml} disabled={!medioId || cargando}>
            Generar XML
          </Button>
          <Button variant="outlined" onClick={onDescargarXml} disabled={!medioId || cargando}>
            Descargar XML
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