export {
  consultarEstadoTestSet,
  enviarDocumentosTestSet,
  guardarHabilitacionDian,
  listarAuditoriaDian,
  listarDocumentosTestDian,
  obtenerConfigFeEmpresa,
  obtenerHabilitacionDian,
  guardarConfigFeEmpresa,
  switchoverProduccion,
  traerDocumentosDianPorArchivo,
  traerDocumentosDianPorToken,
} from '../client'
export type {
  AuditoriaDian,
  ConfigFe,
  DocumentoTestDian,
  EstadoHabilitacionDian,
  HabilitacionDian,
  ModoOperacionDian,
  ResultadoTestSetDian,
  SolicitudHabilitacionDian,
  SolicitudSwitchoverDian,
} from '../client'

export const MODOS_OPERACION_DIAN = [
  {
    codigo: 'ADQUIRIDO_CLIENTE',
    etiqueta: 'Software adquirido por el cliente',
    descripcion: 'El cliente es titular del software y de la resolución ante la DIAN.',
    requierePt: false,
  },
  {
    codigo: 'PROPIO_INTERNO',
    etiqueta: 'Software propio interno',
    descripcion: 'Habilitación interna de MagnetixDIAN para uso propio.',
    requierePt: false,
  },
  {
    codigo: 'PT_MAGNETIXDIAN',
    etiqueta: 'Proveedor tecnológico MagnetixDIAN',
    descripcion: 'MagnetixDIAN factura como proveedor tecnológico autorizado.',
    requierePt: true,
  },
  {
    codigo: 'PT_EXTERNO',
    etiqueta: 'Proveedor tecnológico externo',
    descripcion: 'El cliente usa un proveedor tecnológico externo autorizado.',
    requierePt: true,
  },
] as const

export const CONFIRMACION_SWITCHOVER = 'APRUEBO_SWITCHOVER' as const

export function modoPorDefecto(): 'ADQUIRIDO_CLIENTE' {
  return 'ADQUIRIDO_CLIENTE'
}

export function estadoPermiteEmitir(estado: string | null | undefined): boolean {
  return estado === 'PRODUCCION'
}

export function testSetAprobado(estado: string | null | undefined): boolean {
  return estado === 'TEST_SET_APROBADO' || estado === 'PRODUCCION'
}
