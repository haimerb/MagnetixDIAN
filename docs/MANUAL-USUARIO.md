# MagnetixDIAN — Manual de Usuario

Este manual describe el recorrido completo: **venta → pago → verificación → wizard DIAN →
primera factura → asiento → exógena**.

## 0. Costo y modelos de operación

El software funciona con servicios gratuitos (Cloudflare Workers, Render free y Supabase free).
No hay costo de licencia para los modos propios o adquiridos. Si se opera como Proveedor
Tecnológico (PT) se requiere habilitación ante la DIAN; ese trámite es externo al software.

| Modo DIAN | Uso | Facturar a terceros |
| --- | --- | --- |
| `PROPIO_INTERNO` | Pruebas internas del tenant | No |
| `ADQUIRIDO_CLIENTE` (default) | El cliente ya tiene software propio | No |
| `PT_MAGNETIXDIAN` | MagnetixDIAN es el PT | Sí, con habilitación |
| `PT_EXTERNO` | El cliente tiene un PT externo | Sí, con habilitación del tercero |

## 1. Registro y acceso

1. En la landing, seleccione **Comenzar gratis** o el plan deseado.
2. En **Registro** digite NIT, razón social, correo y contraseña. El registro es público;
   la verificación y el reenvío de enlaces también. Las invitaciones requieren sesión.
3. Confirme el correo (token de un solo uso, vigencia limitada).
4. Ingrese con usuario/contraseña. Si olvidó la clave use **Olvidé mi contraseña**.
   Tras 5 intentos fallidos la cuenta se bloquea 15 minutos.

## 2. Venta y pago (checkout)

1. En **Empresa → Suscripción** revise el plan y los límites.
2. Seleccione **Cambiar plan** y el método: Wompi (tarjeta, Nequi, PSE, QR Bre-B) o
   MercadoPago. En instalaciones locales se puede usar el modo **SIMULADO**; en producción
   ese modo está deshabilitado.
3. El pago se confirma mediante webhook firmado. No recargue la página ni cierre la ventana
   hasta ver el estado **Pagado / Suscripción activa**.
4. Si el pago no aparece, use **Verificar pago** (o el enlace de verificación enviado por correo).
   Los eventos son idempotentes: un reenvío no genera doble cobro ni doble activación.

> Nunca comparta el secreto de firma del webhook ni pegue tokens en tickets de soporte.

## 3. Wizard de habilitación DIAN

En **Empresa → Configuración FE / Asistente DIAN**:

1. **Modo:** seleccione uno de los cuatro modos de la tabla anterior.
2. **Resolución:** número, fecha, prefijo y rango autorizado (cuando aplique).
3. **Certificado:** cargue el PKCS#12 y su contraseña; se guarda cifrado por empresa.
4. **Pruebas:** en habilitación use el `testSetId`; en producción, el entorno definitivo.
5. **Adquirente:** active la notificación al adquirente cuando corresponda (modos `PT_*`).
6. Guardar y ejecutar la prueba de emisión. El asistente marca cada paso como completado.

No es necesario tramitar un PT para los modos `PROPIO_INTERNO` o `ADQUIRIDO_CLIENTE`.

## 4. Primera factura

1. **Facturación → Factura rápida** (o **Nueva factura**): cliente, ítems, impuestos y forma de pago.
2. **Validar:** se aplican reglas de NIT/DV, fechas, totales y obligaciones fiscales.
3. **Emitir:** se genera CUFE/CUDE, UBL firmada, PDF y QR.
4. **Entregar:** por correo o **WhatsApp Click-to-Chat** (abre `wa.me`, no requiere token de API).
5. **RADIAN:** los eventos 030–034 se consultan desde **Contabilidad → RADIAN**; la aceptación
   tácita automática es configurable por empresa.

## 5. Asiento contable

1. **Contabilidad → Asentar facturación:** traiga documentos por token DIAN o por archivo
   (Excel/CSV/JSON/ZIP). El token se cifra en BD y nunca se muestra de nuevo.
2. **Causar** los documentos RADIAN y revisar el borrador del asiento.
3. **Contabilizar:** se aplica el PUC de la empresa; el asiento debe quedar balanceado
   (débito = crédito) y numerado `AS-{empresa}-{YYYYMM}-{0001}`.
4. **Conciliación:** compare documentos DIAN contra asientos por fecha, dirección y tipo.

## 6. Información exógena

1. **Contabilidad → Asientos contables → Exógena.**
2. Seleccione el formato oficial activo: **1125 v13**, **2278 v1** o **5253 v2**
   (los formatos clásicos 1001/1002 siguen disponibles en **Inicio**).
3. Revise los conceptos, gener el XML y descargue el archivo.
4. Use **Validar XML**: la validación es **XSD local** (`xsd-local-2025`).
   **No hay prevalidación oficial de la DIAN**: presente el archivo en el portal de la DIAN.

## 7. Administración y seguridad

- **Administración:** empresas, usuarios, invitaciones (con token de un solo uso), calendario
  tributario, auditoría, reintentos de transmisión y transmisión de facturas.
- **Claves de API:** en **Empresa → Claves de API** puede crear claves `X-API-Key` por empresa.
- **Cuenta:** cambio de clave, cierre de sesión y sesiones/refresh tokens con rotación.
- **Branding y PWA:** colores, tipografía, icono y manifiesto por empresa; la app es instalable.

## 8. Reportes y validación clásica (1001/1002)

1. **Inicio → Cargar Excel** (formatos 1001/1002).
2. **Validar:** errores y advertencias por fila/columna; aplique correcciones sugeridas.
3. **Generar XML**, marcar como presentado y conservar el comprobante.

## 9. Checklist rápido antes de operar

- [ ] Suscripción activa y pago verificado.
- [ ] Modo DIAN seleccionado acorde a la habilitación.
- [ ] Certificado vigente cargado y cifrado.
- [ ] Primera factura emitida y entregada.
- [ ] Asiento causado y balanceado.
- [ ] Exógena generada y validada localmente con XSD.
- [ ] Usuarios con roles mínimos necesarios; invitaciones caducadas revocadas.
