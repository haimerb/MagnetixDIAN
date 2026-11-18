import { ThemeProvider } from '@mui/material/styles'
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import theme from '../../theme/theme'
import StatCard from './StatCard'

describe('StatCard', () => {
  it('renderiza etiqueta, valor y pista', () => {
    render(
      <ThemeProvider theme={theme}>
        <StatCard label="Facturas emitidas" value={42} hint="Ultimo mes" />
      </ThemeProvider>,
    )
    expect(screen.getByText('Facturas emitidas')).toBeInTheDocument()
    expect(screen.getByText('42')).toBeInTheDocument()
    expect(screen.getByText('Ultimo mes')).toBeInTheDocument()
  })
})
