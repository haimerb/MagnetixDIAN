import { type Page, type Route } from '@playwright/test'
import { expect, test } from './fixtures'

type OpcionesMock = {
  empresaId?: number
  firmado?: boolean
  duplicado?: boolean
  estadoHabilitacion?: string
}

type AuditoriaMock = {
  id: number
  accion: string
  detalle: string
  usuario: string | null
  createdAt: string
}

type HabilitacionMock = {
  empresaId: number
  modo: string
  estado: string
  resolucionNumero: string | null
  resolucionFecha: string | null
  prefijo: string | null
  rangoDesde: number | null
  rangoHasta: number | null
  testSetId: string | null
  softwareId: string | null
  notificarAdquirente: boolean
  certificadoNombre: string | null
  switchoverEn: string | null
  aprobadoEn: string | null
  auditoria: AuditoriaMock[]
}

const TRACK_IDS = [
  'TEST-01', 'TEST-02', 'TEST-03', 'TEST-04', 'TEST-05',
  'TEST-06', 'TEST-07', 'TEST-08', 'TEST-09-ND', 'TEST-10-NC',
]

function token(empresaId: number): string {
  const cabecera = Buffer.from(JSON.stringify({ alg: 'none', typ: 'JWT' })).toString('base64')
  const carga = Buffer.from(JSON.stringify({
    sub: 'admin',
    roles: ['ROLE_ADMIN'],
    empresaId,
  })).toString('base64')
  return `${cabecera}.${carga}.firma-e2e`
}

async function entrar(page: Page, empresaId = 1) {
  await page.addInitScript(({ valor }) => {
    localStorage.setItem('magnetixdian_token', valor)
    localStorage.setItem('magnetixdian_refresh', 'refresh-e2e')
  }, { valor: token(empresaId) })
}

function responder(route: Route, cuerpo: unknown, estado = 200) {
  return route.fulfill({
    status: estado,
    contentType: 'application/json',
    body: JSON.stringify(cuerpo),
  })
}

function cuerpo(route: Route): Record<string, unknown> {
  try {
    return route.request().postDataJSON() as Record<string, unknown>
  } catch {
    return {}
  }
}

function habilitacionInicial(empresaId: number, estado: string): HabilitacionMock {
  return {
    empresaId,
    modo: 'ADQUIRIDO_CLIENTE',
    estado,
    resolucionNumero: null,
    resolucionFecha: null,
    prefijo: null,
    rangoDesde: null,
    rangoHasta: null,
    testSetId: null,
    softwareId: null,
    notificarAdquirente: false,
    certificadoNombre: null,
    switchoverEn: null,
    aprobadoEn: null,
    auditoria: [],
  }
}

async function mockApi(page: Page, opciones: OpcionesMock = {}) {
  const empresaId = opciones.empresaId ?? 1
  const firmado = opciones.firmado ?? true
  const duplicado = opciones.duplicado ?? false
  const habilitacion = habilitacionInicial(empresaId, opciones.estadoHabilitacion ?? 'BORRADOR')
  const peticiones: string[] = []

  const resultadoTestSet = {
    testSetId: 'TS-E2E-001',
    estado: 'TEST_SET_APROBADO',
    total: TRACK_IDS.length,
    aprobados: TRACK_IDS.length,
    rechazados: 0,
    documentos: TRACK_IDS.map((trackId) => ({
      trackId,
      tipoDocumento: '01',
      descripcion: `Documento ${trackId}`,
      estado: 'APROBADO',
      mensaje: null,
    })),
  }

  await page.route('**/api/**', async (route) => {
    const url = new URL(route.request().url())
    const ruta = url.pathname.replace(/^\/api/, '')
    const metodo = route.request().method()
    peticiones.push(`${metodo} ${ruta}`)

    if (ruta === '/auth/me' && metodo === 'GET') {
      return responder(route, {
        id: 1,
        username: 'admin',
        nombre: 'Admin',
        email: 'admin@ejemplo.com',
        roles: ['ROLE_ADMIN'],
        empresaId,
        empresaNit: '900123456',
        empresaRazonSocial: 'MagnetixDIAN SAS',
      })
    }
    if (ruta === '/planes' && metodo === 'GET') {
      return responder(route, [{
        id: 1,
        codigo: 'BASICO',
        nombre: 'Básico',
        descripcion: 'Plan de prueba',
        precioMensual: 99000,
        moneda: 'COP',
        limiteFacturasMes: 100,
        limiteUsuarios: 2,
        orden: 1,
      }])
    }
    if (ruta === '/pagos/checkout' && metodo === 'POST') {
      return responder(route, {
        checkoutUrl: 'https://checkout.e2e.local/p/PAY-E2E-001',
        referencia: 'PAY-E2E-001',
        monto: 99000,
        moneda: 'COP',
        estado: 'PENDIENTE',
      })
    }
    if (/^\/pagos\/[^/]+\/webhook-estado$/.test(ruta) && metodo === 'GET') {
      return responder(route, {
        referencia: 'PAY-E2E-001',
        proveedor: 'WOMPI',
        estado: duplicado ? 'RECHAZADO' : 'APROBADO',
        firmado,
        eventId: duplicado ? 'evt-pago-001' : 'evt-pago-002',
        procesadoEn: firmado && !duplicado ? new Date().toISOString() : null,
        comprobanteNumero: firmado && !duplicado ? 'COMP-E2E-001' : null,
        mensaje: duplicado
          ? 'Evento duplicado: no procesado'
          : firmado ? 'Webhook verificado' : 'Firma ausente',
      })
    }
    if (ruta === '/registro/verificar' && metodo === 'GET') {
      return responder(route, {
        empresaId: 1,
        estadoTenant: 'ACTIVO',
        emailVerificado: true,
        mensaje: 'Correo verificado correctamente.',
      })
    }
    if (ruta === '/registro/reenviar' && metodo === 'POST') {
      return responder(route, {
        enviado: true,
        mensaje: 'Envíe el enlace desde su correo para completar la verificación.',
        token: null,
      })
    }
    if (ruta === `/empresas/${empresaId}/dian/habilitacion` && metodo === 'GET') {
      return responder(route, habilitacion)
    }
    if (ruta === `/empresas/${empresaId}/dian/habilitacion` && metodo === 'PUT') {
      Object.assign(habilitacion, cuerpo(route))
      return responder(route, habilitacion)
    }
    if (ruta === `/empresas/${empresaId}/dian/test-set/documentos` && metodo === 'POST') {
      habilitacion.estado = 'TEST_SET_APROBADO'
      habilitacion.testSetId = resultadoTestSet.testSetId
      return responder(route, resultadoTestSet)
    }
    if (/^\/empresas\/\d+\/dian\/test-set\/[^/]+$/.test(ruta) && metodo === 'GET') {
      return responder(route, resultadoTestSet)
    }
    if (ruta === `/empresas/${empresaId}/dian/switchover` && metodo === 'POST') {
      habilitacion.estado = 'PRODUCCION'
      habilitacion.switchoverEn = new Date().toISOString()
      habilitacion.auditoria = [{
        id: 1,
        accion: 'SWITCHOVER',
        detalle: 'Switchover confirmado con APRUEBO_SWITCHOVER',
        usuario: 'admin',
        createdAt: new Date().toISOString(),
      }]
      return responder(route, habilitacion)
    }
    if (ruta === `/facturacion/${empresaId}/validar` && metodo === 'POST') {
      return responder(route, [])
    }
    if (ruta === `/facturacion/${empresaId}/emitir` && metodo === 'POST') {
      return responder(route, {
        exitoso: true,
        id: 77,
        numeroCompleto: 'FV 0001',
        cufe: 'cufe-e2e-0001',
        estado: 'EMITIDO',
        qrUrl: null,
        mensaje: 'Aceptado',
        errores: [],
      })
    }
    if (ruta === `/empresas/${empresaId}/contabilidad/asientos` && metodo === 'POST') {
      return responder(route, {
        id: 55,
        numero: 'AS-77',
        fecha: new Date().toISOString().slice(0, 10),
        tipoEvento: 'EMISION_FACTURA',
        descripcion: 'Asiento de la factura FV 0001',
        totalDebito: 1000000,
        totalCredito: 1000000,
        lineas: [],
      })
    }
    if (ruta === `/empresas/${empresaId}/exogena/formatos` && metodo === 'GET') {
      return responder(route, [])
    }
    if (ruta === `/empresas/${empresaId}/exogena/generar` && metodo === 'POST') {
      return responder(route, {
        anioGravable: new Date().getFullYear(),
        totalFormatos: 1,
        resultados: [{
          formato: '1001',
          descripcion: 'Principal',
          registros: 1,
          xml: '<Reporte/>',
          excel: null,
          validacion: 'VALIDADO',
          errores: [],
          prevalidacionOficial: false,
        }],
        prevalidacionOficial: false,
      })
    }
    if (ruta === '/auth/logout' && metodo === 'POST') {
      return responder(route, {})
    }

    return responder(route, { mensaje: 'Ruta no implementada en el mock E2E' }, 404)
  })

  return { habilitacion, peticiones }
}

test.describe('Flujo ERP completo', () => {
  test('landing → checkout → DIAN → factura → asiento → exógena 1001', async ({ page }) => {
    await entrar(page)
    await mockApi(page)

    await page.goto('/')
    await expect(page.getByRole('link', { name: 'Empezar gratis' }).first()).toBeVisible()

    await page.goto('/checkout?empresa=1&plan=BASICO&email=admin%40ejemplo.com')
    await expect(page.getByRole('heading', { name: 'Finalizar suscripción' })).toBeVisible()
    await page.getByRole('button', { name: 'Continuar al pago' }).click()
    await expect(page.getByText('Referencia: PAY-E2E-001')).toBeVisible()
    await page.getByRole('button', { name: 'Consultar estado del webhook' }).click()
    await expect(page.getByText(/firma válida/)).toBeVisible()

    await page.getByRole('link', { name: 'Verificar correo' }).click()
    await expect(page.getByRole('heading', { name: 'Verificación de correo' })).toBeVisible()
    await page.getByRole('button', { name: 'Reenviar verificación' }).click()
    await expect(page.getByText('Envíe el enlace desde su correo para completar la verificación.')).toBeVisible()

    await page.goto('/verificar-email?token=tok-e2e&email=admin%40ejemplo.com')
    await expect(page.getByText('Correo verificado correctamente.')).toBeVisible()
    await page.getByRole('link', { name: 'Continuar a habilitación DIAN' }).click()

    await expect(page.getByRole('heading', { name: 'Habilitación DIAN' })).toBeVisible()
    await expect(page.getByText(/bloqueada hasta completar el switchover/)).toBeVisible()
    await page.getByRole('button', { name: 'Guardar y continuar' }).click()

    await page.getByLabel('Número de resolución').fill('RES-2026-001')
    await page.getByLabel('Test Set ID').fill('TS-E2E-001')
    await page.getByRole('button', { name: 'Guardar y continuar' }).click()

    await expect(page.getByText(/El TEST SET tiene 10 documentos/)).toBeVisible()
    await page.getByRole('button', { name: 'Enviar TEST SET' }).click()
    await page.getByRole('button', { name: 'Consultar estado del TEST SET' }).click()

    await expect(page.getByText('Paso 4 · Switchover a producción')).toBeVisible()
    await page.getByLabel('Escriba APRUEBO_SWITCHOVER para confirmar').fill('APRUEBO_SWITCHOVER')
    await page.getByRole('button', { name: 'Confirmar switchover a producción' }).click()
    await expect(page.getByText(/Habilitación aprobada/)).toBeVisible()

    await page.getByRole('link', { name: 'Ir a facturación' }).click()
    await expect(page.getByRole('heading', { name: 'Nueva factura electrónica' })).toBeVisible()
    await page.getByLabel('Número de documento').fill('900123456')
    await page.getByLabel('Nombre / razón social').fill('Cliente E2E')
    await page.getByLabel('Descripción').fill('Servicio profesional')
    await page.getByLabel('Valor unitario').fill('1000000')
    await page.getByRole('button', { name: 'Validar' }).click()
    await expect(page.getByText('Documento válido.')).toBeVisible()
    await page.getByRole('button', { name: 'Emitir factura' }).click()
    await expect(page.getByText('Estado: EMITIDO')).toBeVisible()

    await page.getByRole('button', { name: 'Asentar factura' }).click()
    await expect(page.getByText('Asiento AS-77 generado')).toBeVisible()
    await page.getByRole('link', { name: 'Generar exógena 1001' }).click()

    await expect(page.getByRole('heading', { name: 'Información exógena' })).toBeVisible()
    await expect(page.getByText('Prevalidación oficial DIAN: no disponible en este módulo.')).toBeVisible()
    await page.getByRole('button', { name: 'Generar Todo' }).click()
    await expect(page.getByRole('heading', { name: /Principal/ })).toBeVisible()
    await expect(page.getByText(/1 formato\(s\) generado\(s\)/)).toBeVisible()
  })
})
