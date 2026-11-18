import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import EmptyState from './EmptyState'

describe('EmptyState', () => {
  it('renderiza titulo y descripcion', () => {
    render(<EmptyState title="Sin datos" description="Aun no hay registros" />)
    expect(screen.getByText('Sin datos')).toBeInTheDocument()
    expect(screen.getByText('Aun no hay registros')).toBeInTheDocument()
  })
})
