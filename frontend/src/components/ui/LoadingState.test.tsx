import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import LoadingState from './LoadingState'

describe('LoadingState', () => {
  it('anuncia el estado de carga', () => {
    render(<LoadingState label="Cargando reportes" />)
    const estado = screen.getByRole('status')
    expect(estado).toHaveAttribute('aria-busy', 'true')
    expect(estado).toHaveAttribute('aria-live', 'polite')
    expect(screen.getByText('Cargando reportes')).toBeInTheDocument()
  })
})
