# MagnetixDIAN — Manual de Usuario

## Qué hace MagnetixDIAN

MagnetixDIAN es un ERP contable centrado en la DIAN. El menú tiene cuatro áreas: **Inicio**, **Facturación**, **Contabilidad** y **Empresa**. **Contabilidad** abre **Asentar facturación**; desde allí, el botón **Asientos contables** lleva al centro contable de `v0.4.0`.

El flujo principal es:

1. En **Contabilidad → Asentar facturación**, traiga facturas, notas y documentos soporte desde la DIAN usando su **token** o un **archivo Excel/CSV/JSON/ZIP** exportado del catálogo.
2. Cause los documentos y aséntelos a contabilidad desde la misma bandeja.
3. En **Asientos contables**, revise las pestañas **PUC**, **Periodos**, **Asientos**, **Libros**, **Balances**, **Impuestos**, **Exógena** y **Conciliación**.
4. En **Impuestos** calcule tarifas, registre responsabilidades, causaciones, pagos, certificados y declaraciones.
5. En **Exógena** genere XML para los formatos oficiales activos **1125 v13**, **2278 v1** y **5253 v2**, o use el flujo clásico de **1001/1002** desde **Inicio**.
6. Descargue el XML y preséntelo en MUISCA; en el flujo clásico marque el reporte como presentado y en impuestos marque la declaración como presentada.

> **Nota de navegación:** las pantallas antiguas de carga, validación, reportes y guía MUISCA ya no aparecen en el menú principal; siguen accesibles por sus rutas directas (`/app/validacion/:medioId`, `/app/reporte/:medioId`, `/app/guia`) y desde los reportes listados en **Inicio**. Las secciones 2–5 de este manual describen ese flujo heredado de 1001/1002.

## Requisitos

- Archivo Excel `.xlsx` (plantilla del formato 1001 o 1002, 14 columnas — ver §4 del manual técnico o descargue la plantilla del panel).
- Acceso a la aplicación (URL del entorno) con sus credenciales.
- Usuario demo del entorno local: `admin` / `admin123` (rol administrador).

## 1. Ingreso

1. Abra la URL de MagnetixDIAN.
2. Ingrese su usuario y contraseña.
3. Pulse **Ingresar**.

Al entrar verá el **Panel de reportes** con el resumen de sus medios magnéticos (total, sin errores, con errores) y el listado de reportes existentes.

## 2. Cargar datos (flujo heredado 1001/1002)

El flujo clásico de 1001/1002 ya no tiene entrada propia en el menú: el botón **Cargar datos** de **Inicio** redirige a **Contabilidad → Asentar facturación**, que es el flujo preferido. Para alimentar un reporte clásico use `POST /api/mediomagnetico/{empresaId}/cargar` con `formato`, `anioGravable` y `archivo`, o la plantilla descargable desde la API.

Si su entorno conserva la pantalla heredada, el procedimiento es:

1. Abra la pantalla de carga heredada y, si está disponible, pulse **Descargar plantilla (.xlsx)**.
2. Arrastre el archivo `.xlsx` a la zona punteada o haga clic para elegirlo.
3. Indique el **formato** (1001 o 1002) y el **año gravable** (por ejemplo `2025`).
4. Pulse **Cargar archivo**.

- Se permite **un reporte por empresa + formato + año gravable**. Si ya existe información del mismo año, se reemplaza por completo (se borra la anterior).
- Al terminar verá el número de registros cargados y el botón **Ir a validación**.

## 3. Validar datos

1. Vaya a **Validación** (o use «Ir a validación» tras cargar).
2. Pulse **Ejecutar validación**.

El sistema revisa cada registro y muestra:
- Número de **errores** y **advertencias** (¡cuide ambas!).
- Detalle por línea con la columna afectada y el mensaje de la regla incumplida.

**Reglas que aplica (resumen):**

| Regla | Qué revisa |
|---|---|
| Dígito de verificación | Que el DV del NIT coincida con el algoritmo DIAN |
| Obligatoriedad | Tipo documento, número documento, concepto y valor pago presentes |
| Concepto válido | Que el código del concepto exista en el catálogo (1001 o 1002) |
| Valores no negativos | Que retenciones, IVA, gasto, costo y notas no sean negativos |
| Tope 3 UVT | Advertir pagos por valor menor a 3 UVT (149.397 COP en 2025) |

**Terceros y DV:** la sección **Terceros y normalización de DV** muestra cuántos NIT tienen el dígito de verificación incorrecto y sugiere el valor correcto. Use **Aplicar DV correctos** para corregirlos de una vez (recomendado antes de presentar).

**Para corregir:** modifique el Excel (o su sistema de origen), vuelva a cargar las operaciones y revalide. Repita hasta no tener errores.

## 4. Generar, descargar y marcar presentado

1. Con la validación sin errores, pulse **Generar XML**.
2. Use **Descargar XML** para guardar el archivo `medio-magnetico-<formato>.xml`.
3. Conserve el XML generado: es el que presentará ante la DIAN.
4. Una vez presentado en MUISCA, pulse **Marcar presentado** para actualizar el estado del reporte (solo se habilita con la validación sin errores).

## 5. Presentar en MUISCA

Siga la **Guía MUISCA** (ruta directa `/app/guia`, paso a paso para subir el XML y obtener el acuse de recibo). En resumen:

1. Ingrese a [www.muisca.dian.gov.co](https://www.muisca.dian.gov.co).
2. «Información exógena» → presentación del formato 1001 o 1002 correspondiente.
3. Adjunte el XML descargado y confirme.
4. Guarde el **recibo con número de radicado** como soporte.

> Los plazos dependen del calendario tributario vigente y del tipo de contribuyente (gran contribuyente, persona jurídica o natural). Consulte la resolución de la DIAN del año correspondiente y el calendario del panel de administración.

## 6. Administración (rol administrador)

En el menú **Administración** (solo visible con rol `ROLE_ADMIN`) puede:
- **Empresas:** crear y eliminar clientes/declarantes.
- **Usuarios:** crear usuarios y asignarles rol (ADMIN/CONTADOR/CLIENTE) y empresa; activar/desactivar accesos.
- **Calendario DIAN:** gestionar las fechas límite por tipo de reporte y rango de NIT.

## 7. Reportes internos

La sección **Reportes** muestra el resumen de la última corrida de validación de cada medio magnético (registros, errores, advertencias, fecha/hora) y el detalle en JSON para soporte técnico.

## 8. Facturación electrónica

El módulo de **Facturación** permite emitir facturas (01), notas crédito (91), notas débito (92) y documento soporte (05):

1. En el menú seleccione **Facturación** y pulse **Nueva factura**.
2. Diligencie adquirente, líneas (producto, cantidad, valor, impuestos) y datos de pago.
3. Pulse **Validar** para revisar sin emitir; **Emitir** valida, calcula el CUFE, firma el XML (XAdES) y transmite.
4. Desde el detalle puede **descargar el PDF** y el **XML firmado**, **reenviar** ambos por correo, **duplicar** la factura o registrar **eventos RADIAN** (acuse, recibo, reclamo, aceptación).
5. Use **Importar** para cargar varias facturas desde CSV/Excel y **Exportar** para el archivo contable (CSV).

## 9. Suscripción y planes

En el menú **Suscripción** puede:

- Ver su **plan actual**, el consumo del mes (documentos y usuarios) y el precio.
- **Cambiar de plan**: elija la **pasarela de pago** (Simulada para pruebas, Wompi o MercadoPago) y pulse **Cambiar a este plan**. Con la pasarela simulada el cambio se aplica al instante; con las reales será redirigido al pago.
- Consultar el **historial de pagos** y usar **Actualizar estado** en cada pago para consultar el resultado tras volver del checkout.
- **Cancelar** la suscripción (los límites dejan de aplicarse si no hay plan activo).
- Gestionar **claves de API** (ver abajo).

## 10. Claves de API

Si necesita integrar sus sistemas con MagnetixDIAN:

1. En **Suscripción → Claves de API**, pulse **Crear clave** y asígnele un nombre.
2. Copie la clave mostrada (**se muestra una sola vez**).
3. Úsela en sus peticiones con la cabecera `X-API-Key: <clave>`. La clave hereda los permisos de su usuario.
4. Para invalidarla, pulse **Revocar** (deja de funcionar de inmediato).

## 11. Cuenta y seguridad

- **Olvidé mi contraseña:** en el inicio de sesión use **¿Olvidó su contraseña?** y siga el enlace recibido por correo (válido 30 minutos).
- **Invitación:** si un administrador lo invitó, abra el enlace del correo y defina su contraseña (válido 7 días).
- **Bloqueo:** tras 5 intentos fallidos la cuenta se bloquea 15 minutos.
- **Modo oscuro:** alterne el tema claro/oscuro desde el ícono en la barra superior.

## 12. Recordatorio final

- **Una vez presentado**, el reporte queda en estado `PRESENTADO`; no puede volver a editarse.
- Revise periódicamente los avisos de **vencimiento** que el sistema envía por correo (si están habilitados en su instalación).
- Si un error aparece solo en el prevalidador oficial de la DIAN, ajuste los datos en el Excel y repita desde la carga.

## 13. Asentar facturación (`/app/contabilidad/asentar-facturacion`)

1. Ingrese a **Contabilidad → Asentar facturación**.
2. Pulse **Traer de la DIAN** y elija:
   - **Token o enlace:** pegue el token/enlace que obtuvo en el catálogo DIAN. Si lo guarda, queda cifrado y no se muestra de nuevo.
   - **Archivo Excel:** seleccione un `.xlsx`, `.xls`, `.csv`, `.json` o `.zip` exportado desde la DIAN.
3. Indique dirección (**Emitidos** o **Recibidos**), rango de fechas y, si aplica, grupo o NIT del tercero.
4. Pulse **Traer documentos**. Los documentos repetidos por empresa + dirección + CUFE/CUDE se omiten.
5. En la bandeja puede filtrar y pulsar **Causar**. Después pulse **Asentar** para generar el asiento con el plan de cuentas de la empresa.

## 14. Asientos contables y centro contable (`/app/contabilidad`)

1. En **Asientos** consulte los asientos generados al asentar la facturación o desde una migración, con sus líneas de débito/crédito.
2. En **PUC** y **Periodos** verá estados informativos: el plan de cuentas se crea al contabilizar y el cierre de periodos existe en el backend, pero todavía no hay consulta HTTP expuesta. No se muestran datos falsos.
3. En **Libros** elija **Libro diario**, **Libro mayor** o **Auxiliares**; filtre por rango de fechas y, cuando aplique, por código de cuenta.
4. En **Balances** elija **Balance de comprobación**, **Balance general**, **Estado de resultados** o **Flujo de efectivo**. Los dos últimos exigen rango de fechas; el balance general exige fecha de corte.
5. En **Impuestos** use la calculadora y consulte tarifas, causaciones, declaraciones, responsabilidades y calendario tributario.
6. En **Exógena** seleccione el formato, el concepto y el periodo, agregue registros, pulse **Generar XML** y luego **Validar XSD**. El aviso de la pantalla es explícito: la validación es XSD local y **no equivale al prevalidador oficial**.
7. En **Conciliación** elija el rango de fechas y, si lo desea, la dirección y el tipo de documento. El resultado muestra coincidencias, diferencias y valor en diferencias.

## 15. Empresa (`/app/empresa`)

1. En **Usuarios** cree y administre usuarios de la organización.
2. En **Identidad visual** defina nombre visible, colores, tipografía, pie de factura, número/plantilla de WhatsApp y colores de la PWA.
3. En **Integraciones** consulte el catálogo con estados reales y guarde credenciales cuando exista una conexión configurada.
4. En **Suscripción** consulte su plan, consumo y método de pago.
5. En **Administración** gestione empresas, calendario DIAN, reintentos y auditoría.

## 16. Orígenes externos y migración

Cuando su información no viene directamente de la DIAN, exporte desde su sistema de origen y use la importación con mapeo inteligente de columnas o la migración contable por proveedor. Estas herramientas son de apoyo; el flujo de negocio sigue siendo **traer → causar → asentar**.

## 17. Factura rápida y entrega por WhatsApp (`/app/factura-rapida`)

1. Complete el formulario de factura (adquirente, líneas, impuestos) y valide antes de emitir.
2. Tras emitir, use **Entregar por WhatsApp**: el sistema abre un enlace `https://wa.me` con el número y la plantilla configurados.
3. Si no hay número en la empresa, diligéncielo en **Empresa → Identidad visual** o escríbalo en el diálogo de entrega. No se requiere token de API de WhatsApp.

## 18. PWA

1. En el navegador, use **Instalar aplicación** (icono `/icon.svg` y manifiesto `/manifest.webmanifest`) para abrir MagnetixDIAN como app.
2. El service worker funciona solo en HTTPS/localhost y nunca intercepta `/api`.
