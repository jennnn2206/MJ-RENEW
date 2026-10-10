# MJ Renew | Implementación de pagos con Stripe y entorno demo

**Actualización:** 9 de octubre de 2026  
**Proyecto:** MJ Renew — Plataforma Digital de Restauración de Antigüedades  
**Framework:** Spring Boot 3.5.4 · Java 21 · Spring Data JPA · PostgreSQL · Stripe Java SDK 34.0.0  
**Objetivo:** que cualquier integrante del equipo entienda los cambios, pueda preparar su equipo y sepa qué está probado y qué sigue pendiente.

> Este README complementa el README principal del repositorio. Se elaboró a partir de `FIX-STRIPE.zip`, los cambios de la refactorización de pagos entregados después y las pruebas locales realizadas por el equipo. En caso de conflicto funcional, la referencia aprobada es **Diseño API MJ Renew v4**, seguida de los ERS. La conciliación automática requiere documentar/aprobar una ampliación: el diseño vigente establece que **solo el webhook confirma pagos**.

---

## 1. Resumen sencillo: ¿qué se hizo y por qué?

Antes, el proyecto tenía pantallas y procesos de restauración, pero surgieron problemas al intentar confirmar los cobros con Stripe: el pago podía aparecer exitoso en Stripe y continuar como `PENDIENTE` en PostgreSQL. También hubo dificultades para iniciar sesión con las cuentas de prueba y para procesar algunos eventos del webhook.

Durante este trabajo se prepararon y probaron las siguientes mejoras:

1. **Cuentas demo automáticas:** al arrancar con el perfil `demo`, Spring Boot prepara tres usuarios de prueba y un restaurador aprobado y disponible.
2. **Checkout real en sandbox:** el propietario acepta una cotización y se abre una sesión de Stripe Checkout. **No se marca como pagado únicamente por abrir la página o regresar al sitio.**
3. **Webhook seguro:** Stripe avisa al backend si el pago se completó; se comprueba su firma y que los datos correspondan a la transacción de nuestra base de datos.
4. **Servicios de pago separados:** la lectura del webhook quedó apartada de las reglas que actualizan la transacción y el estado de la antigüedad.
5. **Conciliación automática opcional:** si no llegó la notificación de Stripe, el backend puede consultar periódicamente las transacciones pendientes y corregir su estado **sin volver a cobrar**. Se activa solo por configuración.
6. **Ajustes de compatibilidad con PostgreSQL:** se ampliaron tres columnas de la tabla del catálogo para las referencias y URLs de Stripe, y se corrigieron mapeos Java de enumeraciones nativas de PostgreSQL.
7. **Pruebas:** `gradlew clean test` terminó con `BUILD SUCCESSFUL`; en pruebas manuales, un pago se recuperó por conciliación y otro se confirmó mediante webhook HTTP `200`, ambos con transacción `EXITOSA` y antigüedad `PAGO_EN_ESCROW`.

**No se sustituyeron los endpoints oficiales de usuario por rutas de pago manual; el módulo `transaccion` sigue sin controlador público para marcar transacciones como pagadas.**

## 2. Archivos y carpetas importantes

| Ruta dentro de `src/main/java/com/mjrenew/mjrenew_backend` | ¿Para qué sirve? |
|---|---|
| `nucleo/demo/DatosDemoInitializer.java` | Crea o actualiza las cuentas de prueba al iniciar el perfil `demo`. |
| `propietario/service/AntiguedadService.java` | Gestiona la aceptación de cotizaciones, crea la transacción pendiente y solicita Checkout. |
| `transaccion/service/StripeCheckoutService.java` | Comunica el backend con Stripe para crear o consultar sesiones Checkout. |
| `transaccion/controller/StripeWebhookController.java` | Recibe `POST /webhook/stripe` (sin autenticar como usuario, pero con firma de Stripe obligatoria). |
| `transaccion/service/StripeWebhookService.java` | Verifica la firma y convierte los datos recibidos a un formato que entiende el backend. |
| `transaccion/dto/StripeCheckoutEstado.java` | Representa los datos necesarios para verificar una sesión Stripe. |
| `transaccion/service/StripePagoEstadoService.java` | Verifica referencias, montos, moneda y estados, evita duplicados y actualiza PostgreSQL y el AFD. |
| `transaccion/service/StripeConciliacionService.java` | Consulta automáticamente pagos pendientes a Stripe si se habilita la función. |
| `transaccion/repository/TransaccionBancariaRepository.java` | Consultas de transacciones, incluida la protección contra confirmaciones concurrentes. |
| `nucleo/antiguedad/repository/AntiguedadRepository.java` | Ayuda a bloquear la antigüedad durante operaciones sensibles. |
| `nucleo/exception/GlobalExceptionHandler.java` | Traduce a HTTP las excepciones, incluido el caso transitorio `503`. |
| `MjrenewBackendApplication.java` | Habilita las tareas programadas de Spring con `@EnableScheduling`. |

Los nuevos tests de pagos están en `src/test/java/com/mjrenew/mjrenew_backend/transaccion/`.

## 3. ¿Qué es `DatosDemoInitializer` y por qué existe?

### Problema que resuelve

Las cuentas mostradas en la pantalla de acceso no siempre existían, no tenían una contraseña válida o no contaban con el perfil de restaurador necesario para avanzar en el flujo. Esto dificultaba las pruebas compartidas.

### Cómo funciona

- `@Component`: Spring Boot detecta la clase automáticamente.
- `@Profile("demo")`: **únicamente se ejecuta cuando se activa el perfil `demo`**.
- `ApplicationRunner`: se ejecuta al iniciar la aplicación, después de que Spring prepara sus componentes.
- `@Transactional`: prepara los registros mediante una transacción de base de datos.
- `PasswordEncoder`: almacena la contraseña codificada; no guarda la contraseña en texto plano en la tabla.

**Cuentas exclusivas de desarrollo:**

| Rol | Correo | Contraseña de demostración |
|---|---|---|
| Propietario | `propietario1@test.mx` | `Test1234` |
| Restaurador | `restaurador1@test.mx` | `Test1234` |
| Comprador | `comprador1@test.mx` | `Test1234` |

Al iniciar, busca cada cuenta por su correo electrónico. Si no existe, la crea; si ya existe, la conserva y actualiza los datos necesarios para la demostración. También marca estas cuentas como **activas y con correo verificado**. Para el restaurador, crea o actualiza su perfil, lo deja **aprobado por administrador** y **disponible** para que el propietario lo pueda elegir.

**Precauciones importantes:**

- El inicializador **restablece las contraseñas** de estas tres cuentas a `Test1234` cada vez que se inicia el perfil `demo`. Usarlo únicamente en una base de pruebas controlada.
- No crea antigüedades, cotizaciones ni pagos de forma automática; esas operaciones se prueban desde la aplicación.
- **No crea usuarios ADMINISTRADOR ni TRANSPORTISTA.** Esos roles aún se deben preparar y revisar cuando se implemente logística.
- **Nunca ejecutar el perfil `demo` contra producción ni con cuentas reales.** El uso compartido de la contraseña es exclusivamente para pruebas.

## 4. ¿Cómo se procesa un pago de restauración?

```text
PROPIETARIO acepta cotización
       ↓
MJ Renew registra una transacción PENDIENTE
       ↓
StripeCheckoutService crea la sesión Checkout en MXN
       ↓
El navegador abre la página de pago de Stripe (sandbox)
       ↓
Stripe procesa el pago de prueba
       ↓
POST /webhook/stripe (evento firmado)
       ↓
StripeWebhookService verifica la firma y lee el evento
       ↓
StripePagoEstadoService compara sesión, UUID, tipo, monto y moneda
       ↓
Transacción → EXITOSA
Antigüedad → PAGO_EN_ESCROW
```

El pago **no se confirma por la URL de éxito del navegador**. La confirmación requiere un evento firmado de Stripe o, si se aprueba y habilita, una consulta autenticada de conciliación.

**Estados de restauración probados:** `PIEZA_CAPTURADA` → `EN_EVALUACION` → `CALCULANDO_PRESUPUESTO` → `PRESUPUESTO_PRESENTADO` → `PAGO_EN_ESCROW`.

La compra de catálogo también tiene implementación en servicios (`PAGO_VENTA`, `LINK_PAGO_ENVIADO` → `VENTA_COMPLETADA`), pero **no se verificó manualmente de extremo a extremo en estas sesiones**; todavía requiere su propia prueba completa.

### ¿Por qué se dividió `StripeWebhookService`?

Antes esa clase acumulaba verificación de firma, lectura de JSON, validaciones financieras, consultas y cambios de estado. Ahora:

- `StripeWebhookService` se ocupa de **entender y autenticar lo que informa Stripe**.
- `StripePagoEstadoService` se ocupa de **decidir qué puede cambiar en PostgreSQL**.
- `StripeConciliacionService`, cuando está activado, consulta Stripe y **reutiliza el mismo servicio de estados**. Así evitamos mantener reglas distintas para el webhook y la recuperación.

Se utilizan funcionalidades de Spring como `@Transactional`, `@Scheduled`, `@ConditionalOnProperty`, inyección de dependencias, Spring Data JPA y bloqueos de base de datos para evitar procesar varias veces el mismo pago.

### ¿Cómo se recupera un pago cuya notificación no llegó?

Con la conciliación habilitada, Spring revisa periódicamente hasta 50 transacciones pendientes elegibles que tengan una referencia Stripe. Consulta cada sesión directamente a Stripe; si la sesión está completada y pagada, confirma la operación a través de `StripePagoEstadoService`.

**No genera otro Checkout, no repite un cargo y no utiliza IDs de transacciones hardcodeados.** Si el webhook ya confirmó el pago, la transacción deja de estar pendiente y no vuelve a confirmarse.

**Nota documental:** esta forma de confirmación amplía la regla del Diseño API v4 que dice que *solo el webhook confirma pagos*. Debe revisarse y aprobarse con el equipo/asesor y actualizarse el documento oficial; su activación local no modifica automáticamente ese documento.

## 5. Cambios en PostgreSQL

### Ajuste de columnas para Stripe

En `public.catalogo_antiguedad` se ajustaron los tipos siguientes:

| Columna | Tipo esperado | Motivo |
|---|---|---|
| `id_link_stripe_catalogo` | `VARCHAR(255)` | Admitir identificadores de sesión/enlace de Stripe. |
| `id_pago_stripe_catalogo` | `VARCHAR(255)` | Almacenar una referencia de pago Stripe sin límites excesivamente cortos. |
| `url_link_pago_catalogo` | `TEXT` | Admitir URL de pago completas. |

La entidad Java `CatalogoAntiguedad` ya declara esas longitudes y `TEXT`. El script adjunto permite **aplicar estas ampliaciones sobre una base existente**, conservando las filas. Está pensado para ejecutarse después de un respaldo y no crea datos de pago.

**Archivo:** [`database/migraciones/2026-10-09__ampliar_campos_stripe_catalogo.sql`](../../../database/migraciones/2026-10-09__ampliar_campos_stripe_catalogo.sql).

### Mapeo de enumeraciones nativas

Para los tipos ENUM existentes de PostgreSQL se utilizó `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` en las entidades correspondientes (`TransaccionBancaria`, `PerfilRestaurador`, `Cotizacion`, `TrasladoAntiguedad`, `FotografiaAntiguedad` y `DisputaAntiguedad`). Esto permite que Hibernate maneje correctamente sus tipos nativos y evita incompatibilidades de parámetros.

**No implica que el parche de Stripe haya creado las tablas o los ENUM.** Estas estructuras deben existir previamente según el esquema del proyecto.

### ¿Es una copia completa de nuestra base de datos?

**No.** Este repositorio no incluye un volcado verificable de la base de datos PostgreSQL usada en las pruebas. Por ello, el script adjunto es **una migración incremental de los campos que modificamos**, no un `CREATE DATABASE` completo ni una copia de transacciones reales.

Para que una compañera tenga **el esquema completo y exacto** (tablas, ENUM, claves foráneas y relaciones), una integrante con acceso a la base actual debe exportar **solo el esquema**:

1. Abrir **pgAdmin 4** y seleccionar la base `MJRenew-DB`.
2. Elegir **Backup…** y el formato **Plain** (`.sql`).
3. Seleccionar la opción de **solo esquema / Only schema** (no exportar datos).
4. Guardar, revisar que no incluya contraseñas ni información sensible y versionar el archivo como, por ejemplo, `database/esquema/mjrenew_schema.sql` tras la revisión del equipo.
5. En una base nueva, importar **primero el esquema completo**, y luego aplicar migraciones posteriores que no estén incorporadas en esa exportación. **No reaplicar cambios innecesarios sobre un esquema ya actualizado.**

`spring.jpa.hibernate.ddl-auto=validate` **comprueba** el esquema al iniciar; **no crea** las tablas faltantes. Sin el esquema inicial, la migración por sí sola no basta para ejecutar MJ Renew.

### Cómo aplicar la migración sin borrar información

1. Haz un **backup** de la base de datos en pgAdmin antes de ejecutar DDL.
2. En pgAdmin selecciona la base correcta → **Query Tool**.
3. Abre `database/migraciones/2026-10-09__ampliar_campos_stripe_catalogo.sql`.
4. Verifica que **no estés conectada a producción** y ejecútalo (`F5`).
5. Ejecuta [`database/consultas/2026-10-09__verificar_modulo_pagos.sql`](../../../database/consultas/2026-10-09__verificar_modulo_pagos.sql) y revisa los tipos y los registros.

La migración **no inserta cuentas demo** (de eso se encarga Spring) ni cambia a mano estados `PENDIENTE` o `EXITOSA` (eso corresponde a la lógica de pagos).

## 6. Guía para que cada compañera ejecute el proyecto

### Requisitos

- Java **21** y el Gradle Wrapper incluido (`gradlew.bat`; no se requiere instalar Gradle por separado).
- PostgreSQL y **una base de datos de pruebas con el esquema de MJ Renew**.
- Un editor o IDE (IntelliJ / VS Code).
- Cuenta de Stripe con acceso autorizado al **entorno sandbox de MJ Renew**, y Stripe CLI para recibir eventos en desarrollo local.
- Acceso a las **claves de prueba** correspondientes a esa misma cuenta/sandbox. No se necesitan claves de producción.

### Paso A. Configuración local (NO subir a Git)

El archivo `src/main/resources/application.properties` contiene:

```properties
spring.config.import=optional:classpath:application-local.properties
spring.jpa.hibernate.ddl-auto=validate
```

Por tanto, **sí importa** `application-local.properties` al ejecutar con el perfil `demo`, incluso si no se activa un perfil `local` separado.

Cada compañera debe crear **en su computadora** `src/main/resources/application-local.properties`. Hay un ejemplo sin secretos en [`application-local.properties.example`](application-local.properties.example).

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/MJRenew-DB
spring.datasource.username=postgres
spring.datasource.password=TU_PASSWORD_LOCAL
spring.datasource.driver-class-name=org.postgresql.Driver

stripe.secret-key=sk_test_REEMPLAZAR_EN_TU_EQUIPO
stripe.webhook-secret=whsec_REEMPLAZAR_CON_EL_LISTENER_ACTUAL
app.base-url=http://localhost:8080

# Respaldo opcional (ver decisión documental arriba)
mjrenew.stripe.reconciliation-enabled=false
mjrenew.stripe.reconciliation-initial-delay-ms=120000
mjrenew.stripe.reconciliation-delay-ms=60000
```

**Nunca guardar credenciales reales en el ejemplo, README, un ZIP compartido, una captura o un commit.** El archivo local está excluido por `.gitignore`; antes de hacer commit se debe verificar `git status` y `git diff --cached`.

**¿Necesitan mis claves?** No necesitan recibir por WhatsApp o Git tus secretos personales. Lo recomendable es que cada integrante tenga acceso autorizado al mismo sandbox mediante Stripe, y obtenga las credenciales de prueba con el procedimiento del equipo o por un gestor de secretos. La clave `sk_test_...` es sensible aunque sea de pruebas. El `whsec_...` del listener CLI lo genera cada compañera con `stripe listen`, y puede ser diferente en cada equipo.

**¡Ojo con las variables de entorno!** Si existe `STRIPE_WEBHOOK_SECRET` en PowerShell, puede tener prioridad sobre el archivo local. Comprueba que no haya un valor anterior incompatible. En la sesión de PowerShell se puede quitar así:

```powershell
Remove-Item Env:STRIPE_WEBHOOK_SECRET -ErrorAction SilentlyContinue
```

Después de actualizar un secreto, reinicia Spring Boot.

### Paso B. Compilar y probar

Desde la carpeta del proyecto que contiene `gradlew.bat`:

```powershell
.\gradlew.bat clean test
```

Se espera `BUILD SUCCESSFUL`. Esto valida código y tests incluidos, pero **no reemplaza la prueba con Stripe y PostgreSQL reales**.

### Paso C. Iniciar el backend con cuentas demo

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=demo"
```

Se espera un mensaje `Started MjrenewBackendApplication`. Se podrá acceder a la aplicación en `http://localhost:8080/login` y usar las tres cuentas demo de la sección 3.

### Paso D. Mantener Stripe CLI escuchando en otra terminal

Primero autentica Stripe CLI, si el equipo todavía no está autorizado (`stripe login`). Comprueba que la CLI utiliza **la misma cuenta/sandbox** a la que pertenece tu clave `sk_test_...`.

```powershell
stripe listen --events checkout.session.completed,checkout.session.expired,checkout.session.async_payment_succeeded,checkout.session.async_payment_failed --forward-to http://localhost:8080/webhook/stripe
```

Stripe CLI mostrará `Ready!` y un **secreto `whsec_...`**. Colócalo en la configuración local de tu equipo y reinicia Spring Boot para que se aplique. Si cierras el listener y abres otro, comprueba si cambió su secreto.

> En la computadora original se ejecutó un `stripe.exe` desde una carpeta del escritorio. Las demás integrantes pueden instalar Stripe CLI en la ubicación que prefieran; **no copien rutas absolutas de otra compañera**.

### Paso E. Probar un pago sin usar dinero real

1. Entrar como `propietario1@test.mx` y registrar una antigüedad de prueba con sus fotografías.
2. Seleccionar el restaurador demo y comprobar `EN_EVALUACION`.
3. Entrar como `restaurador1@test.mx`, aceptar la evaluación y crear una cotización; comprobar `PRESUPUESTO_PRESENTADO`.
4. Volver como propietario, aceptar la cotización y abrir el Checkout desde el enlace generado por MJ Renew.
5. **Confirmar que Stripe está en sandbox**, usar una tarjeta ficticia facilitada por Stripe (por ejemplo, `4242 4242 4242 4242`, vencimiento futuro y CVC de prueba) y pagar una sola vez.
6. Comprobar en Stripe CLI que el evento **real de esa sesión** llega con HTTP `200`.
7. Ejecutar la consulta de verificación de la sección 5 y confirmar `EXITOSA` + `PAGO_EN_ESCROW`.

**Para aislar la prueba del webhook**, establecer previamente `mjrenew.stripe.reconciliation-enabled=false` y reiniciar Spring. Después se puede reactivar, si el equipo aprueba ese respaldo.

## 7. ¿Por qué Stripe CLI puede mostrar un error si en BD sí cambió?

Son procesos diferentes:

- **Webhook:** Stripe envía información a `localhost` mediante Stripe CLI.
- **Conciliación:** Spring consulta por su cuenta a Stripe y recupera transacciones pendientes sin pasar por Stripe CLI.

En la prueba se ejecutó `stripe trigger checkout.session.completed` y apareció **HTTP 503** porque ese evento sintético no tenía una sesión registrada en MJ Renew. **No significa que se haya rechazado un pago real.** Stripe CLI conserva los errores anteriores en su historial.

Para una transacción **real registrada y pagada en sandbox**, se observó `checkout.session.completed` → **HTTP 200** y su persistencia correcta en PostgreSQL. La conciliación estaba desactivada en esa prueba.

Otras situaciones a revisar:

| Síntoma | Qué comprobar |
|---|---|
| No inicia Spring Boot | Java 21, datos PostgreSQL, esquema existente y propiedades locales. |
| Fallo de firma / `400` | Mismo `whsec_` entre listener activo y backend; reiniciar backend; sincronización de hora del equipo. |
| JSON Stripe rechazado | Revisar error del log y versión de código. La refactorización lee el cuerpo JSON firmado con Jackson. |
| `503` al usar `stripe trigger` | La sesión ficticia no tiene transacción en BD. Probar el flujo real de MJ Renew. |
| Pago `paid` en Stripe pero `PENDIENTE` en BD | Comprobar llegada de webhook; si el equipo autorizó conciliación, revisar que esté realmente habilitada y los logs. **No pagar dos veces.** |
| El comprador o propietario ve “pago exitoso”, pero la BD sigue pendiente | La redirección del navegador no confirma pagos; revisar webhook/Stripe/BD. |
| Demo no permite iniciar sesión | Asegurarse de usar `--spring.profiles.active=demo`, DB correcta y que `DatosDemoInitializer` no haya fallado. |

### Consultas de verificación útiles

Las consultas de `database/consultas/2026-10-09__verificar_modulo_pagos.sql` son **de solo lectura**. Nunca es necesario hacer `UPDATE` manual para que una prueba llegue a `PAGO_EN_ESCROW`.

## 8. Alcance validado y pendientes

**Validado durante las pruebas locales del 9 de octubre de 2026:**

- Creación de cuentas demo y acceso como propietario/restaurador/comprador.
- Flujo de registro → restaurador → evaluación → cotización.
- Checkout Stripe sandbox para restauración.
- Webhook real `checkout.session.completed` con respuesta `200`.
- Transacción `EXITOSA` y antigüedad `PAGO_EN_ESCROW` persistidas por webhook.
- Recuperación de una operación pagada pero pendiente mediante conciliación, sin repetir el cargo.
- Compilación y pruebas automatizadas `BUILD SUCCESSFUL` en el equipo de desarrollo.

**Pendiente (no afirmar como terminado):**

- Probar completamente la compra del **catálogo** desde el comprador, incluyendo fallos y vencimientos.
- Implementar y validar **agendamiento/logística**, transportista y administrador conforme al diseño API.
- Escrow financiero **real**: `PAGO_EN_ESCROW` por ahora es un estado del AFD, **no significa que Stripe retenga legalmente el dinero**. Liberaciones al restaurador/propietario, Stripe Connect, reembolsos y disputas siguen pendientes.
- Desglose final de comisiones, logística y cargo Stripe. Actualmente el importe de la prueba de restauración es el costo máximo de la cotización.
- Vencimiento de **7 minutos** del ERS: el Checkout hospedado se configuró a aproximadamente **35 minutos**; no se ha implementado una caducidad real de siete minutos.
- Definir monitorización, reintentos durables, auditoría de eventos Stripe y pruebas de despliegue con webhook HTTPS permanente. `stripe listen` es una herramienta local.
- Probar escenarios adversos de concurrencia y confirmar con el asesor la adopción permanente de la conciliación automática en el diseño oficial.

## 9. Reglas para trabajar en equipo sin romper el flujo

1. **No sobrescribir** clases o configuraciones de otra integrante sin comparar primero el diff y hacer commit.
2. **No cambiar estados con SQL** para “pasar” el autómata; hacerlos avanzar desde los servicios y endpoints aprobados.
3. Mantener el webhook como entrada principal de pago, validando siempre la firma y los identificadores.
4. No subir secretos, datos de tarjetas, bases con usuarios reales ni el archivo `application-local.properties`.
5. Ejecutar `.\gradlew.bat clean test` antes de integrar cambios y repetir pruebas de pago en sandbox cuando se modifique el módulo.
6. Si se cambia el comportamiento oficial de confirmación (por ejemplo, habilitar conciliación permanente), actualizar **Diseño API**, ERS y esta guía con la decisión aprobada.

---

### Referencias del proyecto

- `Diseno_API_MJRenew_v4-OFICIAL.pdf`: define el webhook y las transiciones de pago aprobadas.
- `ERS_MJRenew_v4.md`: contiene los requerimientos y supuestos, incluidos pagos y logística.
- Código fuente de la rama de arquitectura por roles, ZIP `FIX-STRIPE.zip` y parche de refactorización de Stripe del 9 de octubre de 2026.

**Resumen:** las compañeras pueden levantar MJ Renew con su propia base de pruebas y acceso autorizado a Stripe sandbox. No necesitan copiar las transacciones realizadas durante las pruebas ni recibir secretos por mensajes. La migración adjunta conserva los registros y ajusta las columnas del catálogo; el esquema completo se debe exportar desde la BD que ya funciona.
