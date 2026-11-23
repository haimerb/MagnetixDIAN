import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import type { ReactNode } from 'react'
import EmptyState from '../../components/ui/EmptyState'
import TableSkeleton from '../../components/ui/TableSkeleton'

export type ColumnaTabla = {
  id: string
  label: string
  align?: 'left' | 'right'
}

export type FilaTabla = {
  id: string | number
  celdas: Record<string, ReactNode>
}

type TablaDatosProps = {
  ariaLabel: string
  columnas: ColumnaTabla[]
  filas: FilaTabla[]
  cargando?: boolean
  minWidth?: number
  tituloVacio?: string
  descripcionVacio?: string
  iconoVacio?: ReactNode
  onSeleccionar?: (id: string | number) => void
  seleccionado?: string | number | null
}

export default function TablaDatos({
  ariaLabel,
  columnas,
  filas,
  cargando = false,
  minWidth = 640,
  tituloVacio = 'Sin información',
  descripcionVacio,
  iconoVacio,
  onSeleccionar,
  seleccionado,
}: TablaDatosProps) {
  if (cargando) {
    return <TableSkeleton filas={5} columnas={columnas.length} label="Cargando información…" />
  }

  if (filas.length === 0) {
    return <EmptyState icon={iconoVacio} title={tituloVacio} description={descripcionVacio} />
  }

  return (
    <TableContainer>
      <Table size="small" aria-label={ariaLabel} sx={{ minWidth }}>
        <TableHead>
          <TableRow>
            {columnas.map((columna) => (
              <TableCell key={columna.id} align={columna.align ?? 'left'}>
                {columna.label}
              </TableCell>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {filas.map((fila) => (
            <TableRow
              key={fila.id}
              hover={Boolean(onSeleccionar)}
              selected={seleccionado === fila.id}
              tabIndex={onSeleccionar ? 0 : undefined}
              onClick={onSeleccionar ? () => onSeleccionar(fila.id) : undefined}
              onKeyDown={
                onSeleccionar
                  ? (event) => {
                      if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault()
                        onSeleccionar(fila.id)
                      }
                    }
                  : undefined
              }
              sx={onSeleccionar ? { cursor: 'pointer' } : undefined}
            >
              {columnas.map((columna) => (
                <TableCell key={columna.id} align={columna.align ?? 'left'}>
                  {fila.celdas[columna.id] ?? '—'}
                </TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  )
}
