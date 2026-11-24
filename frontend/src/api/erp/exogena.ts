export {
  generarExogena,
  generarTodoExogena,
  listarConceptosExogena,
  listarFormatosExogena,
  validarXmlExogena,
} from '../client'
export type {
  DetalleValidacionExogena,
  FormatoExogena,
  ResultadoFormatoExogena,
  ResultadoGeneracionExogena,
  ResultadoGenerarTodoExogena,
  SolicitudGenerarTodoExogena,
} from '../client'

export const FORMATO_EXOGENA_PRINCIPAL = '1001'

export function extensionDescarga(xml: string, excel: string | null): { contenido: string; nombre: string; mime: string } {
  return excel
    ? { contenido: excel, nombre: 'exogena.xlsx', mime: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }
    : { contenido: xml, nombre: 'exogena.xml', mime: 'application/xml' }
}
