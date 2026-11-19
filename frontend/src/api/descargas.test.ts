import { afterEach, describe, expect, it, vi } from 'vitest'
import { descargarArchivo } from './descargas'
import { peticionesActivas } from './progreso'

afterEach(() => {
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})

describe('descargarArchivo', () => {
  it('propaga el error y libera el contador cuando la respuesta falla', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 500 }))
    await expect(descargarArchivo('/api/x', 'x.pdf')).rejects.toThrow('500')
    expect(peticionesActivas()).toBe(0)
  })

  it('descarga el archivo y libera el contador', async () => {
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    Object.defineProperty(URL, 'createObjectURL', {
      configurable: true,
      value: vi.fn(() => 'blob:test'),
    })
    Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: vi.fn() })
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({ ok: true, blob: async () => new Blob(['x']) }),
    )

    await descargarArchivo('/api/x', 'x.pdf')

    expect(click).toHaveBeenCalledTimes(1)
    expect(peticionesActivas()).toBe(0)
  })
})
