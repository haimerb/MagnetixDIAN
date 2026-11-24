import { expect, type Page, type Route } from '@playwright/test'

const PROVEEDORES = [
  'SIIGO',
  'ALEGRA',
  'ODOO',
  'WORLD_OFFICE',
  'HELISA',
  'SHOPIFY',
  'WOOCOMMERCE',
  'SAP_BUSINESS_ONE',
  'DYNAMICS_365',
  'NETSUITE',
] as const

const PLANES = [
  {
    id: 1,
    codigo: 'BASICO',
    nombre: 'Básico',
    descripcion: 'Plan inicial',
    precioMensual: 99000,
    moneda: 'COP',
    limiteFacturasMes: 50,
    limiteUsuarios: 1,
    orden: 1,
  },
  {
    id: 2,
    codigo: 'PRO',
    nombre: 'Profesional',
    descripcion: 'Plan profesional',
    precioMensual: 199000,
    moneda: 'COP',
    limiteFacturasMes: 500,
    limiteUsuarios: 5,
    orden: 2,
  },
]

function token(): string {
  const cabecera = Buffer.from(JSON.stringify({ alg: 'none', typ: 'JWT' })).toString('base64')
  const carga = Buffer.from(
    JSON.stringify({ sub: 'admin', roles: ['ROLE_ADMIN'], empresaId: 1 }),
  ).toString('base64')
  return `${cabecera}.${carga}.firma-e2e`
}

function responder(route: Route, cuerpo: unknown, estado = 200) {
  return route.fulfill({
    status: estado,
    contentType: 'application/json',
    body: JSON.stringify(cuerpo),
  })
}

function perfil() {
  return {
    id: 1,
    username: 'admin',
    nombre: 'Administrador',
    email: 'admin@ejemplo.com',
    roles: ['ROLE_ADMIN'],
    empresaId: 1,
    empresaNit: '900123456',
    empresaRazonSocial: 'MagnetixDIAN SAS',
  }
}

function paginaVacia() {
  return {
    contenido: [],
    pagina: 0,
    tamano: 20,
    total: 0,
    totalPaginas: 0,
    primera: true,
    ultima: true,
  }
}

function branding() {
  return {
    empresaId: 1,
    publicSlug: 'magnetixdian',
    displayName: 'MagnetixDIAN',
    logoUrl: null,
    primaryColor: '#1565c0',
    accentColor: '#ef6c00',
    fontFamily: 'Inter',
    invoiceFooter: null,
    whatsappNumber: null,
    whatsappTemplate: null,
    pwaThemeColor: '#1565c0',
    pwaBackgroundColor: '#ffffff',
  }
}

function suscripcion(planCodigo: string) {
  const plan = PLANES.find((item) => item.codigo === planCodigo) ?? PLANES[0]
  return {
    id: 1,
    empresaId: 1,
    plan,
    estado: 'ACTIVA',
    proveedor: 'SIMULADO',
    referenciaExterna: 'PAY-E2E-001',
    periodoInicio: '2026-11-01',
    periodoFin: '2026-11-30',
    uso: {
      facturasMes: 0,
      facturasRestantes: plan.limiteFacturasMes,
      facturasExcedidas: false,
      usuarios: 1,
      usuariosRestantes: plan.limiteUsuarios,
      usuariosExcedidos: false,
    },
  }
}

export async function mockApi(page: Page) {
  let planCodigo = 'BASICO'

  await page.route('**/api/**', async (route) => {
    const url = new URL(route.request().url())
    const ruta = url.pathname.replace(/^\/api/, '')
    const metodo = route.request().method()

    if (ruta === '/auth/login' && metodo === 'POST') {
      const cuerpo = route.request().postDataJSON() as { password?: string }
      if (cuerpo.password === 'clave-incorrecta') {
        return responder(route, { message: 'Credenciales inválidas' }, 401)
      }
      return responder(route, {
        accessToken: token(),
        refreshToken: 'refresh-e2e',
        username: 'admin',
        roles: ['ROLE_ADMIN'],
      })
    }
    if (ruta === '/auth/me' && metodo === 'GET') return responder(route, perfil())
    if (ruta === '/auth/refresh' && metodo === 'POST') {
      return responder(route, { accessToken: token(), refreshToken: 'refresh-e2e' })
    }
    if (ruta === '/auth/logout' && metodo === 'POST') return responder(route, {})
    if (ruta === '/auth/registro' && metodo === 'POST') {
      const cuerpo = route.request().postDataJSON() as { username?: string }
      return responder(route, {
        empresaId: 1,
        usuarioId: 1,
        username: cuerpo.username ?? 'cliente',
        mensaje: 'Cuenta creada correctamente',
      })
    }
    if (ruta === '/auth/forgot-password' && metodo === 'POST') {
      return responder(route, {
        mensaje: 'Si el usuario existe, enviaremos un enlace de recuperación.',
        enlace: null,
      })
    }
    if (ruta === '/planes' && metodo === 'GET') return responder(route, PLANES)
    if (ruta === '/empresas/1/suscripcion' && metodo === 'GET') {
      return responder(route, suscripcion(planCodigo))
    }
    if (ruta === '/empresas/1/pagos/checkout' && metodo === 'POST') {
      const cuerpo = route.request().postDataJSON() as { planCodigo?: string }
      planCodigo = cuerpo.planCodigo ?? 'BASICO'
      return responder(route, {
        id: 1,
        referencia: 'PAY-E2E-001',
        proveedor: 'SIMULADO',
        planCodigo,
        monto: 99000,
        moneda: 'COP',
        estado: 'APROBADO',
        url: null,
      })
    }
    if (ruta === '/empresas/1/pagos' && metodo === 'GET') return responder(route, [])
    if (ruta === '/empresas/1/api-keys' && metodo === 'GET') return responder(route, [])
    if (ruta === '/empresas/1/integraciones' && metodo === 'GET') {
      return responder(
        route,
        PROVEEDORES.map((proveedor, indice) => ({
          id: indice + 1,
          proveedor,
          slug: proveedor.toLowerCase().replace(/_/g, '-'),
          activo: false,
          estado: 'DESCONFIGURADO',
          configurada: false,
          capacidades: ['SYNC_CLIENTES', 'SYNC_PRODUCTOS', 'EMITIR_FACTURA', 'CONSULTAR_ESTADO'],
          ultimoError: null,
          ultimaSincronizacion: null,
          webhookToken: null,
        })),
      )
    }
    if (ruta === '/empresas/1/integraciones/eventos' && metodo === 'GET') return responder(route, [])
    if (ruta === '/empresas/1/branding' && metodo === 'GET') return responder(route, branding())
    if (ruta === '/admin/transmision' && metodo === 'GET') return responder(route, paginaVacia())
    if (ruta === '/admin/usuarios/paginado' && metodo === 'GET') return responder(route, paginaVacia())
    if (ruta === '/admin/empresas/paginado' && metodo === 'GET') return responder(route, paginaVacia())
    if (ruta === '/admin/auditoria' && metodo === 'GET') return responder(route, paginaVacia())
    if (ruta.startsWith('/admin/calendario') && metodo === 'GET') return responder(route, [])
    if (ruta.startsWith('/mediomagnetico/empresa/') && metodo === 'GET') return responder(route, [])

    return responder(route, metodo === 'GET' ? [] : {})
  })
}

export async function loginAdmin(page: Page) {
  const ip = `198.51.100.${Math.floor(Math.random() * 254) + 1}`
  await page.setExtraHTTPHeaders({ 'X-Forwarded-For': ip })
  await page.goto('/login')
  await page.getByLabel('Usuario').fill('admin')
  await page.getByLabel('Contraseña').fill('admin123')
  await page.getByRole('button', { name: 'Ingresar' }).click()
  await expect(page.getByRole('heading', { name: 'Panel de reportes' })).toBeVisible()
}
