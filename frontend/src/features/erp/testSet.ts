export const DOCUMENTOS_TEST_SET = [
  { trackId: 'TEST-01', tipoDocumento: '01', descripcion: 'Factura de venta 1' },
  { trackId: 'TEST-02', tipoDocumento: '01', descripcion: 'Factura de venta 2' },
  { trackId: 'TEST-03', tipoDocumento: '01', descripcion: 'Factura de venta 3' },
  { trackId: 'TEST-04', tipoDocumento: '01', descripcion: 'Factura de venta 4' },
  { trackId: 'TEST-05', tipoDocumento: '01', descripcion: 'Factura de venta 5' },
  { trackId: 'TEST-06', tipoDocumento: '01', descripcion: 'Factura de venta 6' },
  { trackId: 'TEST-07', tipoDocumento: '01', descripcion: 'Factura de venta 7' },
  { trackId: 'TEST-08', tipoDocumento: '01', descripcion: 'Factura de venta 8' },
  { trackId: 'TEST-09-ND', tipoDocumento: '02', descripcion: 'Nota débito' },
  { trackId: 'TEST-10-NC', tipoDocumento: '03', descripcion: 'Nota crédito' },
] as const

export const TOTAL_DOCUMENTOS_TEST_SET = DOCUMENTOS_TEST_SET.length
