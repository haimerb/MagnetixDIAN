import { act, render } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import GlobalProgress from './GlobalProgress'
import { finalizarPeticion, iniciarPeticion } from '../../api/progreso'

describe('GlobalProgress', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('aparece tras el retardo y se oculta al terminar', () => {
    const { container } = render(<GlobalProgress />)
    const barra = container.querySelector('.MuiLinearProgress-root') as HTMLElement
    expect(window.getComputedStyle(barra).visibility).toBe('hidden')

    act(() => {
      iniciarPeticion()
    })
    act(() => {
      vi.advanceTimersByTime(149)
    })
    expect(window.getComputedStyle(barra).visibility).toBe('hidden')

    act(() => {
      vi.advanceTimersByTime(1)
    })
    expect(window.getComputedStyle(barra).visibility).toBe('visible')

    act(() => {
      finalizarPeticion()
    })
    expect(window.getComputedStyle(barra).visibility).toBe('hidden')
  })
})
