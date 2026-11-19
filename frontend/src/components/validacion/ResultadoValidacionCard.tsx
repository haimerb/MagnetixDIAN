import Chip from '@mui/material/Chip'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Typography from '@mui/material/Typography'
import type { Validacion } from '../../api/client'
import SectionCard from '../ui/SectionCard'

function severidadColor(severidad: string) {
  return severidad === 'ERROR' ? 'error' : severidad === 'ADVERTENCIA' ? 'warning' : 'info'
}

export default function ResultadoValidacionCard({
  validacion,
  estado,
}: {
  validacion: Validacion
  estado: string | undefined
}) {
  return (
    <SectionCard title="Resultado de la validación">
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5} flexWrap="wrap" useFlexGap sx={{ mb: 2 }}>
        <Chip color={validacion.erroresTotal > 0 ? 'error' : 'success'} label={`${validacion.erroresTotal} errores`} />
        <Chip color="warning" label={`${validacion.advertenciasTotal} advertencias`} />
        <Chip color="info" label={`${validacion.registrosValidados} registros validados`} />
        <Chip variant="outlined" label={`Estado: ${estado ?? '—'}`} />
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
    </SectionCard>
  )
}
