import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import TableSkeleton from './TableSkeleton'

describe('TableSkeleton', () => {
  it('renderiza filas y columnas indicadas y se anuncia como carga', () => {
    const { container } = render(<TableSkeleton filas={3} columnas={2} />)
    expect(screen.getByRole('status')).toHaveAttribute('aria-busy', 'true')
    expect(container.querySelectorAll('tbody tr')).toHaveLength(3)
    expect(container.querySelectorAll('tbody td')).toHaveLength(6)
  })
})
