import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import type { TercerosResumen } from '../../api/client'
import SectionCard from '../ui/SectionCard'

export default function TercerosCard({
  terceros,
  ocupado,
  aplicando,
  onAplicarDv,
}: {
  terceros: TercerosResumen
  ocupado: boolean
  aplicando: boolean
  onAplicarDv: () => void
}) {
  return (
    <SectionCard title="Terceros y normalización de DV">
      <Stack direction="row" spacing={1.5} flexWrap="wrap" useFlexGap sx={{ mb: 2, alignItems: 'center' }}>
        <Chip label={`${terceros.totalRegistros} registros`} />
        <Chip label={`${terceros.tercerosUnicos} terceros únicos`} />
        <Chip color={terceros.nitsConDvIncorrecto > 0 ? 'warning' : 'success'} label={`${terceros.nitsConDvIncorrecto} NIT con DV incorrecto`} />
        {terceros.nitsConDvIncorrecto > 0 && (
          <Button size="small" variant="outlined" color="warning" onClick={onAplicarDv} disabled={ocupado} loading={aplicando}>
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
  )
}
