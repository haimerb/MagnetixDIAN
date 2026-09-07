import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Paper from '@mui/material/Paper'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableRow from '@mui/material/TableRow'
import Typography from '@mui/material/Typography'
import { api } from '../api/client'
import type { Validacion } from '../api/client'

export default function ReportePage() {
  const { medioId } = useParams()
  const [validacion, setValidacion] = useState<Validacion | null>(null)

  useEffect(() => {
    if (medioId) {
      api
        .get<Validacion>(`/mediomagnetico/${medioId}/reporte`)
        .then((res) => setValidacion(res.data))
        .catch(() => undefined)
    }
  }, [medioId])

  const json = validacion ? JSON.stringify(validacion, null, 2) : ''

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <Typography variant="h4" component="h1">
        Reporte interno
      </Typography>

      {!medioId && <Alert severity="info">Seleccione un medio magnético desde el panel para ver su reporte.</Alert>}

      {validacion && (
        <>
          <Paper variant="outlined" sx={{ p: { xs: 2, md: 3 } }}>
            <Typography variant="h6" component="h2" sx={{ mb: 2 }}>
              Resumen de la corrida
            </Typography>
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
          </Paper>

          <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="subtitle1" sx={{ mb: 1 }}>
              Detalle JSON (revisión interna)
            </Typography>
            <Typography variant="caption" component="pre" sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
              {json}
            </Typography>
          </Paper>
        </>
      )}
    </Box>
  )
}