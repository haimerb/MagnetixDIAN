# MagnetixDIAN — Manual de Usuario

## Qué hace MagnetixDIAN

MagnetixDIAN le permite **generar la información exógena de la DIAN** en los formatos **1001** (pagos/abonos y retenciones a terceros) y **1002** (créditos, descuentos y notas de ajuste) de forma ágil:

1. Descarga la plantilla Excel del formato y la diligencia (o usa su sistema de origen).
2. Carga el archivo con las operaciones del período.
3. El sistema valida los datos contra las reglas DIAN (dígito de verificación del NIT, conceptos válidos, montos mínimos) y le sugiere corregir los DV de los terceros.
4. Una vez sin errores, genera y descarga el XML listo para presentar en MUISCA.
5. Marca el reporte como presentado para registrar el estado.

## Requisitos

- Archivo Excel `.xlsx` (plantilla del formato 1001 o 1002, 14 columnas — ver §4 del manual técnico o descargue la plantilla del panel).
- Acceso a la aplicación (URL del entorno) con sus credenciales.
- Usuario demo del entorno local: `admin` / `admin123` (rol administrador).

## 1. Ingreso

1. Abra la URL de MagnetixDIAN.
2. Ingrese su usuario y contraseña.
3. Pulse **Ingresar**.

Al entrar verá el **Panel de reportes** con el resumen de sus medios magnéticos (total, sin errores, con errores) y el listado de reportes existentes.

## 2. Cargar datos

1. En el menú superior, seleccione **Carga de datos**.
2. Opcional: pulse **Descargar plantilla (.xlsx)** para obtener la plantilla editable del formato.
3. Arrastre el archivo `.xlsx` a la zona punteada o haga clic para elegirlo.
4. Indique el **formato** (1001 o 1002) y el **año gravable** (por ejemplo `2025`).
5. Pulse **Cargar archivo**.

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

Siga la **Guía MUISCA** disponible en el menú (paso a paso para subir el XML y obtener el acuse de recibo). En resumen:

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

## 8. Recordatorio final

- **Una vez presentado**, el reporte queda en estado `PRESENTADO`; no puede volver a editarse.
- Revise periódicamente los avisos de **vencimiento** que el sistema envía por correo (si están habilitados en su instalación).
- Si un error aparece solo en el prevalidador oficial de la DIAN, ajuste los datos en el Excel y repita desde la carga.