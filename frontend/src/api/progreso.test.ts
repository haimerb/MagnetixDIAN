import { describe, expect, it, vi } from 'vitest'
import { finalizarPeticion, iniciarPeticion, peticionesActivas, suscribirProgreso } from './progreso'

describe('progreso', () => {
  it('cuenta peticiones activas sin bajar de cero', () => {
    expect(peticionesActivas()).toBe(0)
    iniciarPeticion()
    iniciarPeticion()
    expect(peticionesActivas()).toBe(2)
    finalizarPeticion()
    expect(peticionesActivas()).toBe(1)
    finalizarPeticion()
    finalizarPeticion()
    expect(peticionesActivas()).toBe(0)
  })

  it('notifica a los suscriptores en cada cambio', () => {
    const oyente = vi.fn()
    const cancelar = suscribirProgreso(oyente)
    iniciarPeticion()
    finalizarPeticion()
    expect(oyente).toHaveBeenCalledTimes(2)
    cancelar()
    iniciarPeticion()
    finalizarPeticion()
    expect(oyente).toHaveBeenCalledTimes(2)
  })
})
