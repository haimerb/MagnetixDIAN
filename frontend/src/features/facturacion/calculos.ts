import type { LineaFacturaRequest } from '../../api/client'

export type TotalesLinea = {
  base: number
  iva: number
  inc: number
  total: number
}

type LineaCalculable = Pick<
  LineaFacturaRequest,
  'cantidad' | 'valorUnitario' | 'descuento' | 'ivaPct' | 'incPct'
>

const r2 = (valor: number) => Math.round((valor + Number.EPSILON) * 100) / 100

export function totalesLinea(linea: LineaCalculable): TotalesLinea {
  const bruto = r2(linea.cantidad * linea.valorUnitario)
  const base = r2(bruto - linea.descuento)
  const iva = r2((base * linea.ivaPct) / 100)
  const inc = r2((base * linea.incPct) / 100)
  return { base, iva, inc, total: r2(base + iva + inc) }
}

export function money(v: number) {
  return v.toLocaleString('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })
}
