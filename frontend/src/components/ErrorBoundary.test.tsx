import { render, screen } from '@testing-library/react'
import type { ReactElement } from 'react'
import { describe, expect, it, vi } from 'vitest'
import ErrorBoundary from './ErrorBoundary'

function Rompe(): ReactElement {
  throw new Error('boom')
}

describe('ErrorBoundary', () => {
  it('muestra el fallback cuando un componente lanza un error', () => {
    const espia = vi.spyOn(console, 'error').mockImplementation(() => undefined)
    render(
      <ErrorBoundary>
        <Rompe />
      </ErrorBoundary>,
    )
    expect(screen.getByText('Algo salió mal')).toBeInTheDocument()
    espia.mockRestore()
  })
})
