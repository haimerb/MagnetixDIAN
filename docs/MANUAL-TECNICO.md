# MagnetixDIAN — Manual Técnico

Sistema con **tres módulos**:

1. **Medios magnéticos (información exógena DIAN)** — formatos **1001 v10** (pagos/abonos y retenciones) y **1002 v10** (créditos, descuentos y notas). Flujo: carga de Excel → validación → reporte → XML → marcar presentado + normalización de terceros.
2. **Facturación electrónica (FEV v1.9)** — emisión de facturas (01), notas crédito (91) y notas débito (92) con CUFE/CUDE (SHA-384), UBL 2.1, firma XAdES-EPES, PDF, QR y transmisión por canal (simulado / PAC / DIAN SOAP).
3. **Integraciones y pagos** — sincronización con ERP/contabilidad/e-commerce, webhooks firmados y checkout de suscripciones (simulado / Wompi / MercadoPago).

## 1. Stack

| Capa | Tecnología | Versión |
|---|---|---|
| Lenguaje backend | Java (Temurin) | 17 |
| Framework backend | Spring Boot | 3.3.5 |
| ORM / migraciones | Spring Data JPA / Flyway | 10.20.1 |
| Base de datos | PostgreSQL (Docker / Supabase) | 16 |
| Autenticación | JWT (jjwt) | 0.12.6 |
| Excel | Apache POI | 5.3.0 |
| XML | DOM (JDK) + XSD propio (1001 y 1002) | — |
| Firma XAdES | XML Digital Signature (JDK) + BouncyCastle (bcpkix) | — |
| PDF factura | OpenPDF | — |
| QR | ZXing (core + javase) | — |
| Correo | Spring Boot Mail (SMTP) | 3.3.5 |
| Health | Spring Boot Actuator | 3.3.5 |
| Swagger | springdoc-openapi | 2.6.0 |
| Frontend | React / Vite / MUI / React Router | 18 / 5 / 6.5 / 6 |
| Tests frontend | Vitest + Testing Library | 2.1.9 |

**Requerimiento de JDK 17:** el Maven global del sistema usa JDK 8. Todos los builds exigen la variable de entorno:

```bash
export JAVA_HOME="C:/Users/hbarb/AppData/Local/jdk17"
export PATH="$JAVA_HOME/bin:$PATH"
```

## 2. Arquitectura del código

```
backend/src/main/java/com/magnetixdian/
  interfaces/rest/      Controllers REST (AuthController, MedioMagneticoController,
                        FacturaController, ConfiguracionFeController, AdminController,
                        PlanController, ApiKeyController, PagoController,
                        IntegracionController, IntegracionWebhookController,
                        GlobalExceptionHandler)
  interfaces/dto/       Records de respuesta (NUNCA entidades JPA — open-in-view=false)
                        incluido PaginaDto<T> para listados paginados
  application/          Servicios de caso de uso (AuthService, JwtService, ServicioAdmin,
                        ServicioMedioMagnetico, ServicioValidacion, ServicioFacturacion,
                        ServicioNumeracion, ServicioFirma, ServicioTransmision,
                        ServicioConfigFe, ServicioGeneracionAutomatica, ServicioRecuperacion,
                        ServicioSuscripciones, ServicioApiKeys, ServicioPagos, PasarelasPago,
                        ServicioIntegraciones, RegistroIntegraciones,
                        AuditoriaService, ProgramadorReintentos, ProgramadorAceptacionTacita)
  domain/
    rule/               ReglaValidacion (interfaz), RuleEngine, reglas 1001/1002, CatalogoConceptos
    rule/factura/       Reglas de facturación (ValidadorFactura + ReglaFactura)
    cufe/               CufeCalculator (SHA-384), QrUrlGenerator
    factura/            Modelo de dominio (FacturaDatos, TipoDocumento, LineaFactura, ...)
    firma/              FirmadorXml (interfaz), FirmadorNoOp, FirmadorXades
    transmision/        CanalTransmision (interfaz), ResultadoTransmision
    suscripcion/        LimiteUso (regla pura de límites)
    apikey/             ClaveApi (generación/hash SHA-256 de claves de API)
    integracion/        ProveedorIntegracion, AdaptadorIntegracion (firma HMAC y reintentos),
                        DatosConexionIntegracion, EventoIntegracion, ResultadoIntegracion
     pago/               PasarelaPago (interfaz), MetodoPago, SolicitudPago, CheckoutPago, EventoPago
     uvt/                UvtManager
     model/              OperacionDatos, ResultadoRegla, Severidad
   infrastructure/
     excel/              ImportadorExcel (POI), PlantillaExcelGenerator
     xml/                GeneradorXml1001/1002, GeneradorUblFactura, GeneradorAttachedDocument,
                         GeneradorApplicationResponse
     pdf/                GeneradorPdfFactura (OpenPDF)
     transmision/        CanalSimulado, pac/ClientePac, dian/ClienteDianSoap
     pago/               PasarelaSimulada, PasarelaWompi, PasarelaMercadoPago
     integracion/        Adaptadores por proveedor (siigo, alegra, odoo, worldoffice, helisa,
                         shopify, woocommerce, sap, dynamics, netsuite)
     notification/       NotificadorEmail, ServicioNotificaciones, SchedulerJobs
    persistence/        Repositorios y entidades JPA
  security/             JwtAuthFilter, ApiKeyAuthFilter, RateLimitFilter,
                        CustomUserDetailsService, AutorizacionEmpresa
  config/               SecurityConfig, JwtProperties
```

**Principios:**

- **Reglas extensibles:** toda regla de negocio es un bean `@Component` que implementa `ReglaValidacion` (o `ReglaFactura`). `RuleEngine` / `ValidadorFactura` las detectan automáticamente por Spring y las ejecutan en orden. Nueva regla = nueva clase, **nunca** tocar el engine.
- **Reglas format-aware:** `ReglaValidacion.aplicaA(formato)` permite activar/desactivar reglas por formato (1001/1002). `ContextoValidacion` transporta el `formato`.
- **DTOs en el contrato API:** las respuestas serializan records, no entidades (evita proxys LAZY con `open-in-view: false`).
- **Java 17 estricto:** no usar features de Java 21 (ej. pattern matching en `switch`).
- **Autorización por empresa:** `AutorizacionEmpresa` verifica que el usuario autenticado (no-ADMIN) solo acceda a los recursos de su empresa; `ROLE_ADMIN` accede a todo. Se aplica en los controllers de facturación, medios, configuración, planes, claves de API y pagos.
- **Puertos + adaptadores:** las integraciones externas se definen como interfaz (`CanalTransmision`, `PasarelaPago`, `FirmadorXml`, `AdaptadorIntegracion`) con adaptadores intercambiables; sin credenciales quedan inertes (`CanalSimulado`, `PasarelaSimulada`, `FirmadorNoOp`). `RegistroIntegraciones` descubre los adaptadores Spring y `ServicioIntegraciones` centraliza firmas, idempotencia y reintentos.
- **Sin `WebConfig`:** CORS y seguridad se centralizan en `SecurityConfig`.

## 3. Modelo de datos

Migraciones Flyway en `backend/src/main/resources/db/migration/`:

| Migración | Contenido |
|---|---|
| `V1__esquema_inicial.sql` | Esquema base |
| `V2__seed.sql` | UVT, catálogo de reglas, calendario DIAN |
| `V3__seed_usuario.sql` | Usuario admin + empresa demo |
| `V4__facturacion_electronica.sql` | Tablas de facturación electrónica |
| `V5__catalogos_facturacion.sql` | Catálogos DIAN de facturación |
| `V6__resolucion_demo.sql` | Resolución/prefijo demo por empresa |
| `V7__notificar_adquirente.sql` | Datos de notificación al adquirente en `empresa_fe_config` |
| `V8__ampliar_columnas_secretos.sql` | Ampliación de columnas de secretos |
| `V9__auth_seguridad_auditoria.sql` | `password_reset_token`, `refresh_token`, `evento_auditoria`, columnas de bloqueo/invitación en `usuario` |
| `V10__envio_factura_reintentos.sql` | `envio_factura` (último envío por factura para reintentos) |
| `V11__planes_suscripciones.sql` | `plan` + `suscripcion` (seed FREE/PRO/ENTERPRISE y suscripción demo) |
| `V12__api_keys.sql` | `api_key` (claves de API por empresa, hash SHA-256) |
| `V13__pagos.sql` | `pago` (intención de pago por pasarela) |
| `V14__registro_self_service.sql` | registro público, aceptación de términos y suscripción FREE |
| `V15__integraciones.sql` | `integracion_config` (credenciales cifradas, hash del token de webhook) e `integracion_evento` (idempotencia/reintentos) |
| `V16__pagos_metodo.sql` | columna `pago.metodo` (`MetodoPago`) |

**Tablas de medios magnéticos:** `empresa`, `tercero`, `medio_magnetico`, `operacion`, `uvt_vigencia`, `regla`, `validacion` / `detalle_error`, `calendario_dian`, `notificacion`, `role` / `usuario` / `usuario_role`.

**Tablas de facturación electrónica:**

| Tabla | Propósito |
|---|---|
| `empresa_fe_config` | Configuración DIAN por empresa (ambiente, software, resolución, rango, canal, PAC, certificado, responsabilidad fiscal) |
| `consecutivo` | Consecutivos de numeración por empresa/prefijo/tipo |
| `factura` | Encabezado (tipos 01/91/92/05, CUFE/CUDE, totales, adquirente, `cufe_referencia`, `numero_referencia`, estado) |
| `factura_linea` | Líneas (producto, cantidad, valor, impuestos) |
| `factura_impuesto` | Impuestos desagregados por línea |
| `factura_evento` | Eventos/transmisión (EMISION y eventos DIAN 030-034) |
| `catalogo_dian` | Catálogos DIAN (tipo documento, responsabilidad, tributo, evento, ...) |
| `envio_factura` | Último envío por factura + reintentos (backoff) |
| `password_reset_token` / `refresh_token` | Tokens de un solo uso (reset de contraseña y sesión) |
| `evento_auditoria` | Bitácora de login, cambios de clave, CRUD admin y emisión/rechazo |
| `plan` / `suscripcion` | Planes (FREE/PRO/ENTERPRISE) y suscripción activa por empresa |
| `api_key` | Claves de API por empresa (prefijo + hash SHA-256, propietario, último uso) |
| `pago` | Intención de pago por pasarela (referencia única, monto, estado, URL de checkout y `metodo`) |
| `integracion_config` | Configuración por empresa/proveedor (credenciales cifradas, estado, hash y prefijo del token de webhook) |
| `integracion_evento` | Eventos de sincronización/webhook con `external_id` único, intentos y próximo intento |

**Estados de `medio_magnetico`:** `BORRADOR` → `CARGADO` → `VALIDADO` | `VALIDADO_CON_ERRORES` → `XML_GENERADO` → `PRESENTADO`. Solo se permite `PRESENTADO` desde `VALIDADO`/`VALIDADO_CON_ERRORES`/`XML_GENERADO`.

**Estados de `factura`:** `BORRADOR` | `GENERADO` | `FIRMADO` | `ENVIADO` | `ACEPTADO` | `RECHAZADO` | `ANULADA`.

Constraint SQL importante: `validacion` no permite operaciones duplicadas por línea (`UNIQUE (medio_magnetico_id, linea)`); la recarga de Excel limpia antes (`@Modifying(flushAutomatically = true, clearAutomatically = true)`).

## 4. Formatos — layout del Excel cargado

Columnas (A–N), primera fila de encabezados, datos desde la fila 2. **Los formatos 1001 y 1002 comparten layout** (14 columnas); cambia el catálogo de conceptos y la interpretación de `VALOR_PAGO`:

| # | Columna | Tipo | Regla de negocio |
|---|---|---|---|
| 1 | `TIPO_DOC` | texto (NIT, CC, NITCE) | obligatorio |
| 2 | `NUMERO_DOC` | número/texto | obligatorio |
| 3 | `DV` | número | dígito de verificación del NIT |
| 4 | `CONCEPTO` | número | debe existir en catálogo (1001 o 1002) |
| 5 | `VALOR_PAGO` | número | obligatorio |
| 6 | `RET_RENTA` | número | ≥ 0 |
| 7 | `RET_IVA` | número | ≥ 0 |
| 8 | `RET_ICA` | número | ≥ 0 |
| 9 | `RET_TIMBRE` | número | ≥ 0 |
| 10 | `IVA_PAGADO` | número | ≥ 0 |
| 11 | `IVA_DESC` | número | ≥ 0 |
| 12 | `GASTO` | número | ≥ 0 |
| 13 | `COSTO` | número | ≥ 0 |
| 14 | `NOTA_CREDITO` | número | ≥ 0 |

La **plantilla editable** se descarga desde la app (`GET /api/mediomagnetico/plantilla?formato=`), con filas de ejemplo que el importador omite.

Archivos de prueba regenerables desde `backend/tools/`:
- `GenXlsx.java` → `target/ejemplo-1001.xlsx` (5 registros con **7 errores / 1 advertencia**).
- `GenXlsxLimpio.java` → `target/ejemplo-1001-limpio.xlsx` (5 registros **sin errores** → genera XML válido).
- `GenXlsx1002.java` → `target/ejemplo-1002.xlsx` (3 registros 1002 **sin errores**).

Ver §8 para el procedimiento de compilación de los generadores.

## 5. Catálogo de conceptos

`CatalogoConceptos` mantiene listas por formato:

| Formato | Conceptos de ejemplo |
|---|---|
| **1001** | 2004 servicios, 2005 honorarios, 2007 comisiones, 2008 arrendamiento, 2009 intereses, 2012 compras, 2018 salarios, 2025 dividendos, 2036 exterior, 2050 salud, 2071 regalías, 2073 construcción, 2080 prestaciones, 2086 servicios públicos, 2090 vigilancia, 2091 técnicos |
| **1002** | 7001 devoluciones ventas, 7002 descuentos otorgados, 7003 rebajas, 7004 notas crédito, 7005 devoluciones compras, 7006 descuentos recibidos, 7007 notas débito, 7008 diferencias de cambio |

Reglas implementadas en `domain/rule/` (catalogadas en BD, V2 seed):

| Código | Regla | Formato | Severidad |
|---|---|---|---|
| `DV_NIT` | Dígito de verificación del NIT (módulo 11, algoritmo DIAN) | 1001/1002 | ERROR |
| `OBLIGATORIEDAD` | Campos obligatorios (tipo doc, número doc, concepto, valor pago) | 1001 | ERROR |
| `OBLIGATORIEDAD_CAMPO_1002` | Campos obligatorios del formato 1002 | 1002 | ERROR |
| `CONCEPTO_VALIDO` | Concepto existe en el catálogo del formato | 1001/1002 | ERROR |
| `CONSISTENCIA_VALORES` | Valores numéricos no negativos y retenciones ≤ pago | 1001 | ERROR |
| `TOPE_3_UVT` | Pagos menores a 3 UVT del año (aviso) | 1001 | ADVERTENCIA |

Límite de reporte: 3 UVT × 49.799 (2025) = **149.397 COP**.

### Reglas de facturación (`domain/rule/factura/`)

`ValidadorFactura` ejecuta las reglas `ReglaFactura`: `NitFacturaRegla`, `ObligatoriedadFacturaRegla`, `LineasFacturaRegla`, `TotalesFacturaRegla`, `RangoNumeracionFacturaRegla`.

## 6. Facturación electrónica

- **CUFE/CUDE:** `CufeCalculator` (SHA-384) sobre la cadena DIAN; `QrUrlGenerator` arma la URL del QR.
- **UBL 2.1:** `GeneradorUblFactura` + `GeneradorAttachedDocument` + `GeneradorApplicationResponse`.
- **Firma:** `ServicioFirma` elige `FirmadorXades` si la empresa tiene certificado, o `FirmadorNoOp` en caso contrario. XAdES-EPES enveloped con PKCS#12 (SHA-256, política de firma DIAN v2).
- **Transmisión:** `ServicioTransmision` usa el canal configurado (`domain/transmision/CanalTransmision`): `CanalSimulado` (default), PAC REST (`ClientePac`, JSON con Bearer token, `Idempotency-Key` y timeouts configurables) o DIAN SOAP (`SendBillSync`, `ClienteDianSoap`, firma WS-Security X.509 con Santuario y timeouts configurables).
- **Configuración FE:** `PUT /api/facturacion/config/{empresaId}` valida y audita los cambios; la carga de certificados acepta solo PKCS#12 `.p12`/`.pfx` de hasta 1 MB, verifica la clave privada y registra `CERTIFICADO_FE_ACTUALIZADO`.
- **PDF:** `GeneradorPdfFactura` (OpenPDF), descargable con el XML.
- **Notas crédito/débito:** los tipos 91/92 referencian el documento original mediante `cufe_referencia` / `numero_referencia`, reflejados en el UBL.
- **Eventos DIAN (RADIAN):** los eventos 030-034 (acuse, recibo, reclamo, aceptación) se registran sobre la factura y se persisten en `factura_evento`. El evento `ApplicationResponse` calcula su propio **CUDE** (SHA-384 sobre `Num_DE + Fec_Emi + Hor_Emi + NitFE + DocAdq + ResponseCode + ID + DocumentTypeCode + Software-PIN`, §11.5 del anexo FEV v1.9) y se genera vía `GeneradorApplicationResponse`.
- **Aceptación tácita:** `ProgramadorAceptacionTacita` marca como aceptadas las facturas sin reclamo pasado el plazo (gate `RADIAN_ACEPTACION_TACITA`, default `false`).
- **Reintentos de transmisión:** `envio_factura` guarda el último envío; `ProgramadorReintentos` reintenta cada 60 s con backoff 1→5→15→60→240 min (máx. 5).

## 7. API REST

Base: `/api`. Swagger: `http://localhost:8080/swagger-ui.html`.

### Autenticación

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/login` | Login `{username, password}` → `{accessToken, refreshToken, username, roles}` |
| POST | `/api/auth/refresh` | Renovar token con `{refreshToken}` (rotación/revocación) |
| POST | `/api/auth/logout` | Revoca el refresh token |
| POST | `/api/auth/forgot-password` | Envía enlace de reseteo (token de un solo uso, TTL 30 min) |
| POST | `/api/auth/reset-password` | Restablece la contraseña con el token |
| POST | `/api/auth/accept-invite` | Acepta invitación y define la contraseña (TTL 7 días) |
| POST | `/api/auth/change-password` | Cambio de contraseña autenticado |
| GET | `/api/auth/me` | Datos del usuario autenticado |

**Endurecimiento:** bloqueo tras 5 intentos fallidos durante 15 min (`423`), rate limit de 20 req/min por IP en `/api/auth/**` (`RateLimitFilter`). Las claves de API (`X-API-Key`) heredan los roles de su usuario propietario (`ApiKeyAuthFilter`).

### Medios magnéticos

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/mediomagnetico/plantilla?formato=` | Descarga la plantilla Excel (1001/1002) |
| POST | `/api/mediomagnetico/{empresaId}/cargar` | Multipart: `formato`, `anioGravable`, `archivo` (Excel) |
| POST | `/api/mediomagnetico/{id}/validar?anioGravable=` | Ejecuta validación → `ValidacionProcesoDto` |
| GET | `/api/mediomagnetico/{id}/reporte` | Última corrida de validación |
| POST | `/api/mediomagnetico/{id}/generar-xml` | Genera XML (1001 o 1002 según el formato) |
| GET | `/api/mediomagnetico/{id}/xml` | Descarga el XML (attachment) |
| POST | `/api/mediomagnetico/{id}/presentar` | Marca como presentado (solo si validado) |
| GET | `/api/mediomagnetico/{id}/terceros` | Resumen de terceros + sugerencias de DV |
| POST | `/api/mediomagnetico/{id}/aplicar-dv` | Aplica los DV correctos a los NIT |
| GET | `/api/mediomagnetico/empresa/{empresaId}` | Lista medios magnéticos de la empresa |
| GET | `/api/mediomagnetico/empresa/{empresaId}/paginado` | Listado paginado (`page`, `size`) |

### Facturación electrónica

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/facturacion/{empresaId}/validar` | Valida una factura/nota sin emitir |
| POST | `/api/facturacion/{empresaId}/emitir` | Emite (valida, CUFE, UBL, firma, transmite, persiste) |
| POST | `/api/facturacion/{empresaId}/importar` | Importación masiva desde CSV/Excel (multipart) |
| GET | `/api/facturacion/{empresaId}` | Lista documentos de la empresa |
| GET | `/api/facturacion/{empresaId}/paginado` | Listado paginado (`page`, `size`) |
| GET | `/api/facturacion/{empresaId}/export` | Exportación contable (CSV) |
| GET | `/api/facturacion/{empresaId}/{id}` | Detalle del documento |
| POST | `/api/facturacion/{empresaId}/{id}/eventos` | Registra un evento RADIAN (030-034) sobre la factura |
| GET | `/api/facturacion/{empresaId}/{id}/pdf` | Descarga el PDF |
| GET | `/api/facturacion/{empresaId}/{id}/xml` | Descarga el XML firmado |
| POST | `/api/facturacion/{empresaId}/{id}/reenviar` | Reenvía XML + PDF por correo `{destinatario}` (409 si notificaciones están deshabilitadas) |
| GET | `/api/facturacion/catalogo/{tipo}` | Catálogo DIAN (`TIPO_DOCUMENTO`, `TRIBUTO`, ...) |
| GET/PUT | `/api/facturacion/config/{empresaId}` | Obtiene/guarda la configuración DIAN de la empresa |

### Planes, suscripciones, claves de API y pagos

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/planes` | Catálogo de planes activos |
| GET | `/api/empresas/{empresaId}/suscripcion` | Suscripción activa + uso del mes (documentos/usuarios) |
| POST | `/api/empresas/{empresaId}/suscripcion` | Cambia de plan `{planCodigo, proveedor}` |
| DELETE | `/api/empresas/{empresaId}/suscripcion` | Cancela la suscripción |
| GET | `/api/empresas/{empresaId}/api-keys` | Lista las claves de API de la empresa |
| POST | `/api/empresas/{empresaId}/api-keys` | Crea una clave `{nombre}` (se devuelve **una sola vez**) |
| DELETE | `/api/empresas/{empresaId}/api-keys/{id}` | Revoca una clave |
| GET | `/api/empresas/{empresaId}/pagos` | Historial de pagos de suscripción |
| GET | `/api/empresas/{empresaId}/pagos/{referencia}` | Estado actual de un pago (útil tras volver del checkout) |
| POST | `/api/empresas/{empresaId}/pagos/checkout` | Inicia el pago `{planCodigo, proveedor}` → `{url, ...}` |
| POST | `/api/empresas/{empresaId}/pagos/{referencia}/reconciliar` | Consulta la pasarela y aplica el estado real del pago |
| POST | `/api/pagos/webhook/{proveedor}` | Webhook público de la pasarela (`SIMULADO`/`WOMPI`/`MERCADOPAGO`) |

### Integraciones

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/empresas/{empresaId}/integraciones` | Catálogo de proveedores + estado/configuración de la empresa |
| PUT | `/api/empresas/{empresaId}/integraciones/{proveedor}` | Guarda `{activo, credenciales, configuracion}`; devuelve el token de webhook **una sola vez** |
| POST | `/api/empresas/{empresaId}/integraciones/{proveedor}/probar` | Prueba de conexión del adaptador |
| POST | `/api/empresas/{empresaId}/integraciones/{proveedor}/sincronizar` | Sincroniza `{tipo: CLIENTE\|PRODUCTO\|FACTURA, datos}` |
| GET | `/api/empresas/{empresaId}/integraciones/eventos` | Últimos 50 eventos (estado, intentos, próximo intento, error) |
| POST | `/api/empresas/{empresaId}/integraciones/eventos/{eventoId}/reintentar` | Fuerza el despacho de un evento |
| POST | `/api/integraciones/webhook/{proveedor}/{token}` | Webhook público; cabeceras `X-Integracion-Firma` y `X-Integracion-Timestamp` (ver §13) |

### Administración (rol ADMIN)

| Método | Ruta | Descripción |
|---|---|---|
| GET/POST/PUT/DELETE | `/api/admin/empresas` | CRUD de empresas |
| GET | `/api/admin/empresas/paginado` | Listado paginado (`page`, `size`) |
| GET/POST/PUT | `/api/admin/usuarios` | CRUD de usuarios |
| GET | `/api/admin/usuarios/paginado` | Listado paginado (`page`, `size`) |
| POST | `/api/admin/usuarios/invitar` | Invita a un usuario (enlace con token de 7 días) |
| POST | `/api/admin/usuarios/{id}/enlace-acceso` | Genera un enlace de acceso manual |
| PUT | `/api/admin/usuarios/{id}/estado` | Activa/desactiva un usuario |
| GET | `/api/admin/auditoria` | Bitácora de auditoría |
| GET | `/api/admin/transmision` | Estado de transmisión y reintentos |
| POST | `/api/admin/transmision/{id}/reintentar` | Fuerza el reintento de un envío |
| GET/POST/DELETE | `/api/admin/calendario` | Calendario DIAN |
| GET | `/actuator/health` · `/actuator/health/liveness` · `/actuator/health/readiness` | Health checks (sin auth) |

**Seguridad:** salvo `/api/auth/**`, `/api/pagos/webhook/**`, `/api/integraciones/webhook/**`, `/swagger-ui/**`, `/v3/api-docs/**` y `/actuator/health/**`, todo exige `Authorization: Bearer <jwt>` (o `X-API-Key`). `/api/admin/**` exige además `ROLE_ADMIN`. Los recursos por empresa se filtran con `AutorizacionEmpresa` (403 si no corresponde). CORS configurable con `CORS_ORIGINS` (default `localhost:5173`, `localhost:4173`).

**Respuestas de error** (`GlobalExceptionHandler`): `400` argumentos inválidos, `403` acceso denegado, `409` estado inconsistente, `404` no encontrado, JSON limpio.

## 8. Tareas de desarrollo

```bash
# Levantar BD (contenedor magnetixdian-db)
cd docker && docker compose up -d db

# Backend: correr en :8080
export JAVA_HOME="C:/Users/hbarb/AppData/Local/jdk17"
export PATH="$JAVA_HOME/bin:$PATH"
cd backend
MAVEN_OPTS="-Dfile.encoding=UTF-8" mvn spring-boot:run

# Backend: build con tests
MAVEN_OPTS="-Dfile.encoding=UTF-8" mvn clean package

# Frontend (pnpm — NO usar npm)
cd frontend
pnpm run lint       # ESLint (0 errores, 0 warnings)
pnpm run test       # Vitest
pnpm run build      # tsc -b + vite build
pnpm run test:e2e   # Playwright (requiere backend + preview)

# Regenerar los Excel de prueba (desde backend/)
mvn -q dependency:copy-dependencies -DincludeScope=runtime
"C:/Users/hbarb/AppData/Local/jdk17/bin/javac" -encoding UTF-8 -cp "target/dependency/*" -d target tools/GenXlsx.java
"C:/Users/hbarb/AppData/Local/jdk17/bin/java" -cp "target;target/dependency/*" GenXlsx

# Matar el backend si quedó corriendo
netstat -ano | grep :8080 && taskkill //PID <pid> //F
```

**Cambios a migraciones:** si se modifican migraciones ya aplicadas, recrear el volumen:

```bash
docker compose down -v && docker compose up -d db
```

## 9. Despliegue

### Docker Compose (desarrollo/preproducción)

```bash
cd docker && docker compose up -d --build
```

- `db` → `localhost:5432` / `magnetixdian`
- `backend` → `localhost:8080` (env vars via compose; volumen `uploads`)
- `frontend` (Nginx) → `localhost:5173`, proxya `/api` a `backend:8080`

### Despliegue gratuito (Cloudflare Workers + Render + Supabase)

Guía completa en `docs/DEPLOY-FREE.md`:

- **Frontend:** Cloudflare Worker (`worker.js` + `wrangler.toml`) que sirve el bundle Vite y proxya `/api` al backend.
- **Backend:** Render free (`render.yaml` + `backend/Dockerfile.render`, JVM `-Xmx256m`).
- **BD:** Supabase free vía **Session pooler (IPv4)**; Flyway con `baseline-on-migrate`.

El workflow `.github/workflows/deploy.yml` publica automáticamente tras un merge a `main` (ver §11).
El keep-alive principal es un Cron Trigger de Cloudflare (`wrangler.toml` + handler `scheduled` en
`worker.js`) que consulta `/actuator/health` cada 5 minutos; `.github/workflows/keep-alive.yml` queda
como respaldo cada 10 minutos. Es una **mitigación, no un SLA** (detalles en `docs/DEPLOY-FREE.md`).

## 10. Variables de entorno

| Variable | Default | Uso |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/magnetixdian?characterEncoding=UTF-8` | JDBC |
| `DB_USER` / `DB_PASSWORD` | `magnetixdian` / `magnetixdian` | credenciales BD |
| `JWT_SECRET` | valor base64 interno | firma JWT (cambiar en producción) |
| `JWT_EXPIRATION_MS` | `86400000` | vigencia access token |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` | vigencia refresh token |
| `UPLOAD_DIR` | `./uploads` | archivos subidos (Excel, certificados) |
| `CORS_ORIGINS` | `http://localhost:5173,http://localhost:4173` | orígenes permitidos |
| `NOTIFICATIONS_ENABLED` | `false` local / `true` en Render | envío de correos y recordatorios |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | `smtp-relay.brevo.com` / 2525 / — | SMTP (2525 evita el bloqueo de Render free) |
| `MAIL_FROM` | igual a `MAIL_USERNAME` | remitente verificado de correos |
| `REMINDER_CRON` | `0 0 9 * * *` | cron recordatorios |
| `GENERATOR_CRON` | `0 0 2 1 1 *` | cron generador automático de XML |
| `SERVER_PORT` | `8080` | puerto HTTP |
| `FRONTEND_URL` | `http://localhost:5173` | base para enlaces de correo e retorno de checkout |
| `AUTH_EXPOSE_TOKEN` | `true` | devuelve enlaces/tokens en la respuesta (usar `false` en producción) |
| `CRYPTO_KEY` | valor dev | clave de cifrado de secretos (`empresa_fe_config`) |
| `REINTENTO_TRANSMISION_MS` | `60000` | intervalo del programador de reintentos |
| `RADIAN_ACEPTACION_TACITA` | `false` | habilita la aceptación tácita automática |
| `RADIAN_ACEPTACION_TACITA_CRON` | `0 0 6 * * *` | cron de la aceptación tácita |
| `SENTRY_DSN` / `SENTRY_ENVIRONMENT` / `SENTRY_TRACES_SAMPLE_RATE` | — / `local` / `0.1` | monitoreo backend (inactivo sin DSN) |
| `WOMPI_PUBLIC_KEY` / `WOMPI_INTEGRITY_SECRET` / `WOMPI_EVENTS_SECRET` | — | pasarela Wompi (inactiva sin credenciales) |
| `WOMPI_CHECKOUT_URL` | `https://checkout.wompi.co/p/` | endpoint de checkout de Wompi |
| `MERCADOPAGO_ACCESS_TOKEN` / `MERCADOPAGO_BASE_URL` / `MERCADOPAGO_BACK_URL` | — / `https://api.mercadopago.com` / — | pasarela MercadoPago (inactiva sin credenciales) |
| `MERCADOPAGO_WEBHOOK_SECRET` | — | secreto de firmas `x-signature` (si se define, se valida `id/request-id/ts` con HMAC-SHA256 y frescura) |
| `PAGOS_WEBHOOK_TOLERANCE_SECONDS` | `300` | ventana máxima de antigüedad aceptada en webhooks |
| `PAGOS_EXIGIR_FIRMA_WEBHOOK` | `false` | si `true`, rechaza webhooks sin secreto cuando la pasarela está configurada (**`true` en Render**, fail-closed) |
| `INTEGRACIONES_EXIGIR_FIRMA_WEBHOOK` | `false` | si `true`, exige `webhookSecret` y firma HMAC válida en cada proveedor (**`true` en Render**) |
| `INTEGRACIONES_CONNECT_TIMEOUT_MS` / `INTEGRACIONES_READ_TIMEOUT_MS` | `5000` / `15000` | timeouts de conexión/lectura de los adaptadores de integraciones |
| `INTEGRACIONES_REINTENTOS_MS` | `60000` | intervalo del barrido de eventos `PENDIENTES` |
| `WOMPI_API_TOKEN` / `WOMPI_API_URL` | — / `https://api.wompi.co` | consulta y reconciliación de pagos Wompi |
| `PAC_CONNECT_TIMEOUT_MS` / `PAC_READ_TIMEOUT_MS` | `5000` / `30000` | timeouts del adaptador PAC REST |
| `DIAN_URL_HABILITACION` / `DIAN_URL_PRODUCCION` | endpoints oficiales DIAN | SOAP `SendBillSync` |
| `DIAN_CONNECT_TIMEOUT_MS` / `DIAN_REQUEST_TIMEOUT_MS` | `30000` / `60000` | timeouts de transmisión DIAN directa |
| `VITE_API_BASE_URL` / `VITE_SENTRY_DSN` | — | frontend (build-time) |

## 11. Notificaciones, jobs y CI/CD

- **Recordatorios:** `SchedulerJobs` consulta el calendario DIAN y, a 15/7/1 días del vencimiento (`magnetixdian.notifications.days-before-deadline`), crea `notificacion` y envía correo si `NOTIFICATIONS_ENABLED=true` (cron `REMINDER_CRON`).
- **Generación automática:** `ServicioGeneracionAutomatica` genera el XML de los medios en estado `VALIDADO` (idempotente, un fallo no detiene el lote); programado por `SchedulerJobs` con cron `GENERATOR_CRON`.
- **Transporte:** `ServicioNotificaciones` + `NotificadorEmail` (SMTP) separan la lógica del transporte.
- **Reintentos de transmisión:** `ProgramadorReintentos` reintenta cada 60 s con backoff (1→5→15→60→240 min, máx. 5); panel admin en `/api/admin/transmision`.
- **Aceptación tácita RADIAN:** `ProgramadorAceptacionTacita` (gate `RADIAN_ACEPTACION_TACITA`).
- **Auditoría:** `AuditoriaService` registra login, cambios de clave, CRUD admin y emisión/rechazo de facturas en `evento_auditoria`.
- **Tareas programadas** habilitadas con `@EnableScheduling`.

### CI (GitHub Actions)

`.github/workflows/ci.yml` ejecuta en cada push/PR a `main`/`develop`:
- **backend:** `mvn clean package` (JDK 17, cache Maven).
- **frontend:** `pnpm install --frozen-lockfile && pnpm run lint && pnpm run test && pnpm run build`.
- **e2e:** Playwright (`frontend/e2e/`) contra Postgres 16 + backend + preview; valida las migraciones Flyway.

### CD (GitHub Actions)

`.github/workflows/deploy.yml` despliega tras un push a `main`: publica el frontend en Cloudflare Workers y dispara el deploy de Render (secrets: `CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_ACCOUNT_ID`, `RENDER_DEPLOY_HOOK`).

## 12. Pruebas y smoke test

Suite JUnit en `backend/src/test/java/` — **150 pruebas** (rule engine, reglas de facturación, CUFE/QR/CUDE, importador Excel, seguridad, generador automático, suscripciones, claves de API y pasarelas). Suite Vitest en `frontend/src/**/*.test.tsx` — **22 pruebas** (átomos, ErrorBoundary, notificaciones, tema). Suite E2E Playwright en `frontend/e2e/`.

```bash
# Backend con tests
export JAVA_HOME="C:/Users/hbarb/AppData/Local/jdk17"
export PATH="$JAVA_HOME/bin:$PATH"
cd backend && MAVEN_OPTS="-Dfile.encoding=UTF-8" mvn clean package

# Frontend con tests (pnpm)
cd frontend && pnpm run lint && pnpm run test && pnpm run build && pnpm run test:e2e
```

### Smoke test manual

Requerimientos: BD levantada (`docker compose up -d db`), backend en :8080, el excel de prueba generado.

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r .accessToken)

# 1) Cargar 1001 con errores y validar → esperado: 5 registros, 7 err, 1 adv
curl -s -F "formato=1001" -F "anioGravable=2025" -F "archivo=@target/ejemplo-1001.xlsx" \
  http://localhost:8080/api/mediomagnetico/1/cargar -H "Authorization: Bearer $TOKEN"
curl -s -X POST "http://localhost:8080/api/mediomagnetico/1/validar?anioGravable=2025" -H "Authorization: Bearer $TOKEN"

# 2) Normalización de DV (terceros) → sugerencias y aplicar correcciones
curl -s http://localhost:8080/api/mediomagnetico/1/terceros -H "Authorization: Bearer $TOKEN"
curl -s -X POST http://localhost:8080/api/mediomagnetico/1/aplicar-dv -H "Authorization: Bearer $TOKEN"

# 3) Cargar 1001 LIMPIO → validar (0 errores) → XML → marcar presentado
curl -s -F "formato=1001" -F "anioGravable=2025" -F "archivo=@target/ejemplo-1001-limpio.xlsx" \
  http://localhost:8080/api/mediomagnetico/1/cargar -H "Authorization: Bearer $TOKEN"
curl -s -X POST "http://localhost:8080/api/mediomagnetico/1/validar?anioGravable=2025" -H "Authorization: Bearer $TOKEN"
curl -s -X POST http://localhost:8080/api/mediomagnetico/1/generar-xml -H "Authorization: Bearer $TOKEN"
curl -s -X POST http://localhost:8080/api/mediomagnetico/1/presentar -H "Authorization: Bearer $TOKEN"

# 4) Formato 1002 (IMPORTANTE: empresa/formato/año distinto para no chocar con 1001)
curl -s -X POST http://localhost:8080/api/mediomagnetico/2/cargar \
  -F "formato=1002" -F "anioGravable=2025" -F "archivo=@target/ejemplo-1002.xlsx" \
  -H "Authorization: Bearer $TOKEN"

# 5) Facturación electrónica (empresa demo id=1)
curl -s -X POST http://localhost:8080/api/facturacion/1/validar \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d @factura.json

# 6) Administración (rol ADMIN)
curl -s http://localhost:8080/api/admin/empresas -H "Authorization: Bearer $TOKEN"
curl -s http://localhost:8080/api/admin/calendario?anioGravable=2025 -H "Authorization: Bearer $TOKEN"

# 7) Health (sin auth)
curl -s http://localhost:8080/actuator/health
```

Para el flujo 1002 end-to-end: crear una empresa nueva por admin, luego cargar + validar + generar XML; el generador XML 1002 valida contra `medio-magnetico-1002.xsd`.

Ejemplos de XML de referencia (golden): `docs/ejemplos/medio-magnetico-1001-golden.xml`, `medio-magnetico-1002-golden.xml`, `medio-magnetico-1001-ejemplo.xml` (con errores).

## 13. Integraciones y pagos

### 13.1 Catálogo de integraciones

Los proveedores se identifican por `ProveedorIntegracion` (slug = nombre en minúsculas con `_` → `-`).
Las credenciales se cargan **por empresa** en `/api/empresas/{empresaId}/integraciones` y se cifran en
`integracion_config` con `CRYPTO_KEY`. **No son variables de entorno** y nunca van en `render.yaml`.
Plantilla con valores vacíos:

| Proveedor | Slug | Claves de credenciales (placeholders vacíos) | Referencia |
|---|---|---|---|
| SIIGO | `siigo` | `username=` `accessKey=` `url=` | — |
| ALEGRA | `alegra` | `email=` `apiToken=` | — |
| ODOO | `odoo` | `url=` `db=` `username=` `password=` `webhookSecret=` | — |
| WORLD_OFFICE | `world-office` | `apiToken=` `username=` `password=` `webhookSecret=` | <https://developer.worldoffice.cloud/> |
| HELISA | `helisa` | `usuario=` `clave=` `url=` `sign=` `empresa=` `webhookSecret=` | <https://helisa.com/api/> |
| SHOPIFY | `shopify` | `shop=` `accessToken=` `webhookSecret=` | — |
| WOOCOMMERCE | `woocommerce` | `url=` `consumerKey=` `consumerSecret=` `webhookSecret=` | — |
| SAP_BUSINESS_ONE | `sap-business-one` | `url=` `companyDB=` `username=` `password=` | — |
| DYNAMICS_365 | `dynamics-365` | `url=` `tenantId=` `clientId=` `clientSecret=` | — |
| NETSUITE | `netsuite` | `accountId=` `consumerKey=` `consumerSecret=` `tokenId=` `tokenSecret=` | — |

Cada adaptador declara sus capacidades (`SYNC_CLIENTES`, `SYNC_PRODUCTOS`, `EMITIR_FACTURA`,
`CONSULTAR_ESTADO`, `REGISTRAR_PAGO`, `RECIBIR_WEBHOOK`); el panel muestra solo las soportadas.
`RegistroIntegraciones` las descubre automáticamente como beans Spring.

### 13.2 Webhook de integraciones

`POST /api/integraciones/webhook/{proveedor}/{token}` (público; `MediaType.ALL_VALUE`).

- `{token}` es de un solo uso por configuración: se genera al guardar (32 bytes aleatorios en
  Base64URL), se entrega **una sola vez** en la respuesta y se persiste solo como hash SHA-256 +
  prefijo de 8 caracteres. No hay recuperación: guárdalo o guarda la configuración de nuevo para
  rotarlo. Los reenvíos del mismo evento se absorben por idempotencia, no por reutilizar el token.
- Cabeceras obligatorias: `X-Integracion-Firma` y `X-Integracion-Timestamp`.
- Firma: `HMAC-SHA256(webhookSecret, timestamp + "." + body)` en hexadecimal minúsculo. El
  `timestamp` acepta epoch segundos o `Instant` ISO-8601 con tolerancia de **300 s**.
- Con `INTEGRACIONES_EXIGIR_FIRMA_WEBHOOK=true`, sin `webhookSecret` o con firma inválida el evento
  se rechaza con `401` (**fail-closed**). En Render debe quedar en `true`.
- Idempotencia: `sha256(proveedor + "|" + empresaId + "|" + payload)` más la restricción única
  `(empresa_id, proveedor, external_id, tipo)`; un reenvío devuelve el evento ya registrado.
- Despacho: `ServicioIntegraciones.procesarPendientes()` corre cada `INTEGRACIONES_REINTENTOS_MS`
  (default 60 s) y reintento con backoff **1→5→15→60→240 min (máx. 5 intentos)**. El panel permite
  reintentar manualmente; tras el quinto fallo el evento queda en `ERROR`.

### 13.3 Métodos de pago

| Método | Pasarela | Observaciones |
|---|---|---|
| `CARD` | Wompi | Web Checkout; los datos de tarjeta los captura la pasarela |
| `NEQUI` | Wompi | Requiere celular de 10 dígitos |
| `PSE` | Wompi | Requiere entidad, tipo de persona, documento y datos del pagador |
| `BREB_QR` | Wompi | **QR dinámico generado por el PSP**; no es conexión directa DICE/MOL |
| `DAVIPLATA` | — | Próximamente (`IllegalStateException` en la pasarela) |

`SIMULADO` aprueba al instante y `MERCADOPAGO` mantiene su flujo propio; el selector de la UI
ofrece `CARD`/`NEQUI`/`PSE`/`BREB_QR`. Los webhooks de pago usan
`POST /api/pagos/webhook/{proveedor}` y, con `PAGOS_EXIGIR_FIRMA_WEBHOOK=true`, una pasarela
configurada sin secreto de firma rechaza el evento (fail-closed).
