import { ThemeProvider } from '@mui/material/styles'
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import theme from '../../theme/theme'
import EstadoChip from './EstadoChip'

function renderChip(estado: string) {
  return render(
    <ThemeProvider theme={theme}>
      <EstadoChip estado={estado} />
    </ThemeProvider>,
  )
}

describe('EstadoChip', () => {
  it('muestra la etiqueta legible del estado conocido', () => {
    renderChip('VALIDADO')
    expect(screen.getByText('Validado')).toBeInTheDocument()
  })

  it('mapea los estados de facturacion electronica', () => {
    renderChip('ACEPTADO')
    expect(screen.getByText('Aceptado')).toBeInTheDocument()
  })

  it('muestra los estados del registro de formatos exógenos', () => {
    renderChip('ACTIVO')
    expect(screen.getByText('Activo')).toBeInTheDocument()
    renderChip('PENDIENTE_XSD')
    expect(screen.getByText('Pendiente de XSD')).toBeInTheDocument()
    renderChip('PENDIENTE_GENERADOR')
    expect(screen.getByText('Pendiente de generador')).toBeInTheDocument()
  })

  it('usa el valor crudo cuando el estado es desconocido', () => {
    renderChip('DESCONOCIDO')
    expect(screen.getByText('DESCONOCIDO')).toBeInTheDocument()
  })
})
