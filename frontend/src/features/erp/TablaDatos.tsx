import Alert from '@mui/material/Alert'
import Paper from '@mui/material/Paper'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Typography from '@mui/material/Typography'
import type { ReactNode } from 'react'
import EmptyState from '../../components/ui/EmptyState'
import LoadingState from '../../components/ui/LoadingState'

export type ColumnaDatos<T> = {
  id: string
  label: string
  render: (fila: T) => ReactNode
  align?: 'left' | 'right'
}

type TablaDatosProps<T> = {
  columnas: ColumnaDatos<T>[]
  datos: T[]
  clave: (fila: T) => string | number
  cargando: boolean
  error: string | null
  mensajeVacio?: string
}

export default function TablaDatos<T>({
  columnas,
  datos,
  clave,
  cargando,
  error,
  mensajeVacio = 'No hay registros disponibles.',
}: TablaDatosProps<T>) {
  if (cargando) return <LoadingState label="Cargando registros…" minHeight="24vh" compacto />
  if (error) return <Alert severity="error">{error}</Alert>
  if (datos.length === 0) return <EmptyState title="Sin registros" description={mensajeVacio} />

  return (
    <TableContainer component={Paper} variant="outlined">
      <Table size="small">
        <TableHead>
          <TableRow>
            {columnas.map((columna) => (
              <TableCell key={columna.id} align={columna.align ?? 'left'}>
                <Typography variant="subtitle2">{columna.label}</Typography>
              </TableCell>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {datos.map((fila) => (
            <TableRow key={clave(fila)} hover>
              {columnas.map((columna) => (
                <TableCell key={columna.id} align={columna.align ?? 'left'}>
                  {columna.render(fila)}
                </TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  )
}
