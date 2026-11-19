import { describe, expect, it } from 'vitest'
import { totalesLinea } from './calculos'

const linea = {
  cantidad: 1,
  valorUnitario: 0,
  descuento: 0,
  ivaPct: 0,
  incPct: 0,
}

describe('totalesLinea', () => {
  it('calcula una linea sin impuestos', () => {
    expect(totalesLinea({ ...linea, cantidad: 3, valorUnitario: 1000 })).toEqual({
      base: 3000,
      iva: 0,
      inc: 0,
      total: 3000,
    })
  })

  it('aplica solo IVA', () => {
    expect(totalesLinea({ ...linea, valorUnitario: 1000, ivaPct: 19 })).toEqual({
      base: 1000,
      iva: 190,
      inc: 0,
      total: 1190,
    })
  })

  it('aplica solo INC', () => {
    expect(totalesLinea({ ...linea, valorUnitario: 1000, incPct: 8 })).toEqual({
      base: 1000,
      iva: 0,
      inc: 80,
      total: 1080,
    })
  })

  it('descuenta la base antes de IVA e INC', () => {
    expect(
      totalesLinea({ ...linea, cantidad: 2, valorUnitario: 5000, descuento: 1000, ivaPct: 19, incPct: 8 }),
    ).toEqual({
      base: 9000,
      iva: 1710,
      inc: 720,
      total: 11430,
    })
  })
})
