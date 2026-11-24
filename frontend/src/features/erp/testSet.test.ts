import { describe, expect, it } from 'vitest'
import { DOCUMENTOS_TEST_SET, TOTAL_DOCUMENTOS_TEST_SET } from './testSet'

function conteoPorTipo(): Record<string, number> {
  return DOCUMENTOS_TEST_SET.reduce<Record<string, number>>((conteo, documento) => {
    conteo[documento.tipoDocumento] = (conteo[documento.tipoDocumento] ?? 0) + 1
    return conteo
  }, {})
}

describe('DOCUMENTOS_TEST_SET', () => {
  it('tiene diez documentos con trackId único', () => {
    expect(TOTAL_DOCUMENTOS_TEST_SET).toBe(10)
    expect(DOCUMENTOS_TEST_SET).toHaveLength(TOTAL_DOCUMENTOS_TEST_SET)
    expect(new Set(DOCUMENTOS_TEST_SET.map((documento) => documento.trackId)).size).toBe(
      TOTAL_DOCUMENTOS_TEST_SET,
    )
  })

  it('coincide con la regla del backend: 8 facturas, 1 nota débito y 1 nota crédito', () => {
    expect(conteoPorTipo()).toEqual({ '01': 8, '02': 1, '03': 1 })
    expect(DOCUMENTOS_TEST_SET.filter((documento) => documento.descripcion === 'Nota débito')).toEqual([
      { trackId: 'TEST-09-ND', tipoDocumento: '02', descripcion: 'Nota débito' },
    ])
    expect(DOCUMENTOS_TEST_SET.filter((documento) => documento.descripcion === 'Nota crédito')).toEqual([
      { trackId: 'TEST-10-NC', tipoDocumento: '03', descripcion: 'Nota crédito' },
    ])
  })

  it('no incluye documentos soporte en el TEST SET DIAN', () => {
    expect(DOCUMENTOS_TEST_SET.some((documento) => documento.descripcion.includes('soporte'))).toBe(false)
    expect(DOCUMENTOS_TEST_SET.some((documento) => String(documento.tipoDocumento) === '05')).toBe(false)
  })
})
