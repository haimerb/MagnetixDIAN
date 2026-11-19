import Box from '@mui/material/Box'
import Skeleton from '@mui/material/Skeleton'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableRow from '@mui/material/TableRow'
import { srOnly } from './srOnly'

type TableSkeletonProps = {
  filas?: number
  columnas?: number
  label?: string
}

export default function TableSkeleton({
  filas = 5,
  columnas = 4,
  label = 'Cargando datos…',
}: TableSkeletonProps) {
  return (
    <Box role="status" aria-live="polite" aria-busy="true">
      <Box sx={srOnly}>{label}</Box>
      <TableContainer aria-hidden="true">
        <Table size="small">
          <TableBody>
            {Array.from({ length: filas }).map((_, fila) => (
              <TableRow key={fila}>
                {Array.from({ length: columnas }).map((_, columna) => (
                  <TableCell key={columna}>
                    <Skeleton variant="text" width={columna === 0 ? '35%' : '75%'} />
                  </TableCell>
                ))}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  )
}
