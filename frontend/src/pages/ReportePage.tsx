import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableRow from '@mui/material/TableRow'
import { api } from '../api/client'
import type { Validacion } from '../api/client'
import LoadingState from '../components/ui/LoadingState'
import PageHeader from '../components/ui/PageHeader'
import SectionCard from '../components/ui/SectionCard'

export default function ReportePage() {
  const { medioId } = useParams()
  const [validacion, setValidacion] = useState<Validacion | null>(null)
  const [cargando, setCargando] = useState(Boolean(medioId))
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!medioId) {
      setCargando(false)
      return
    }
    let activo = true
    setCargando(true)
    setError(null)
    api
      .get<Validacion>(`/mediomagnetico/${medioId}/reporte`)
      .then((res) => {
        if (activo) setValidacion(res.data)
      })
      .catch(() => {
        if (activo) setError('No se pudo cargar el reporte.')
      })
      .finally(() => {
        if (activo) setCargando(false)
      })
    return () => {
      activo = false
    }
  }, [medioId])

  const json = validacion ? JSON.stringify(validacion, null, 2) : ''

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="Paso 3 de 3"
        title="Reporte interno"
        subtitle="Resumen de la corrida de validación y detalle JSON para revisión."
      />

      {!medioId && (
        <Alert severity="info">Seleccione un medio magnético desde el panel para ver su reporte.</Alert>
      )}

      {cargando && <LoadingState label="Cargando reporte…" minHeight="30vh" />}

      {error && <Alert severity="error">{error}</Alert>}

      {validacion && (
        <>
          <SectionCard title="Resumen de la corrida">
            <TableContainer>
              <Table size="small" aria-label="Resumen de validación">
                <TableBody>
                  <TableRow>
                    <TableCell component="th" scope="row">Estado</TableCell>
                    <TableCell>{validacion.estado}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell component="th" scope="row">Registros validados</TableCell>
                    <TableCell>{validacion.registrosValidados}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell component="th" scope="row">Errores</TableCell>
                    <TableCell>{validacion.erroresTotal}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell component="th" scope="row">Advertencias</TableCell>
                    <TableCell>{validacion.advertenciasTotal}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell component="th" scope="row">Inicio</TableCell>
                    <TableCell>{new Date(validacion.inicio).toLocaleString('es-CO')}</TableCell>
                  </TableRow>
                  {validacion.fin && (
                    <TableRow>
                      <TableCell component="th" scope="row">Fin</TableCell>
                      <TableCell>{new Date(validacion.fin).toLocaleString('es-CO')}</TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </SectionCard>

          <SectionCard title="Detalle JSON" subtitle="Revisión interna" padded={false}>
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
              {json}
            </Box>
          </SectionCard>
        </>
      )}
    </Box>
  )
}
