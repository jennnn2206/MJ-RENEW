# Notificaciones de restauración — primer bloque de RF-027

Implementación del 25 de noviembre de 2026. Fuente de negocio: RF-027 del ERS, además de los avisos descritos en RF-007, RF-011, RF-016 y RF-017. El PDF de API v4 no documenta un controlador de notificaciones; se amplía la API bajo `/api/notificaciones`, conservando las rutas de los demás módulos.

## Alcance implementado

| Operación confirmada | Evento | Destinatario | Referencia para deduplicación |
| --- | --- | --- | --- |
| Seleccionar restaurador | `ANTIGUEDAD_ASIGNADA` | Restaurador seleccionado | Antigüedad |
| Generar cotización | `PRESUPUESTO_PRESENTADO` | Propietario | Cotización |
| Publicar avance | `AVANCE_PUBLICADO` | Propietario | Avance |
| Guardar cierre con evidencia final | `RESTAURACION_TERMINADA` | Propietario | Antigüedad |

Cada aviso queda disponible en la bandeja al confirmar la operación, aunque el navegador esté cerrado. Los destinatarios y las referencias se obtienen de las entidades del negocio. El cliente no puede publicar avisos arbitrarios ni escoger otro destinatario por la API.

Este bloque no incorpora todavía las notificaciones de pagos, catálogo, traslados, disputas ni recordatorios. Tampoco incorpora mensajería, excluida del MVP. No completa por sí solo todo RF-027.

## Persistencia y transacciones

Los servicios publican `NotificacionRestauracionEmitida` dentro de su transacción. Un listener síncrono `@EventListener` con `MANDATORY` guarda la notificación y su estado de correo pendiente en esa misma transacción. Un rollback elimina también el aviso; un reinicio posterior al commit conserva el pendiente.

La combinación `(tipo, referencia_id, destinatario_id)` es única. Se usa un `INSERT ... ON CONFLICT DO NOTHING` de Spring Data JPA para que un evento repetido o concurrente no cree otra fila ni invalide la transacción. Se agregaron bloqueos de antigüedad en selección y cotización, siguiendo los ya existentes en avance y cierre. Un segundo cierre exitoso conserva la evidencia y no genera otro aviso. Cada avance diferente sí genera el suyo.

La tabla `notificaciones` reúne la bandeja y la cola persistente de correo. La lectura y el envío son estados independientes: marcar un aviso como leído no cancela su correo. Los índices cubren destinatario/fecha, no leídas y pendientes de correo. Las FK eliminan estos avisos asociados si se elimina físicamente el usuario o la antigüedad; no son el historial permanente de disputas ni la evidencia fotográfica.

## Entrega y reintentos de correo

- Con el correo habilitado, un listener `@TransactionalEventListener` y `@Async` solicita procesar la cola después del commit.
- `@Scheduled` vuelve a revisar pendientes cada 60 segundos por defecto, incluso si el proceso se reinició o falló el listener inicial.
- Cada lote procesa hasta 20 reservas. La selección usa `FOR UPDATE OF n SKIP LOCKED`, excluyendo destinatarios inactivos o sin verificar para que no bloqueen a otros.
- Una transacción corta reserva la fila como `ENVIANDO`, incrementa los intentos y asigna un UUID de reserva con vencimiento de dos minutos.
- El transporte usa Spring Mail fuera de la transacción SQL. Si retorna sin error, una transacción corta registra `ENVIADO`; si falla, conserva `PENDIENTE` y programa otro intento pasado un minuto.
- Una reserva vencida puede recuperarse. El UUID impide que un trabajador anterior sobrescriba una reserva nueva.
- El destinatario se consulta al reservar, sin guardar su dirección en la notificación ni exponerla por la API. No se registran direcciones, contenido ni excepciones SMTP en el log del fallo.

`ENVIADO` significa que el transporte SMTP retornó sin error; no confirma lectura ni recepción en la bandeja externa. SMTP no ofrece una clave de idempotencia: si acepta el mensaje y el proceso cae antes de registrar el éxito, el reintento puede repetir el correo. La bandeja conserva una sola fila por evento.

## Configuración

| Variable / propiedad | Predeterminado | Función |
| --- | --- | --- |
| `NOTIFICATION_EMAIL_ENABLED` / `mjrenew.notificaciones.correo-habilitado` | `false` | Activa el despacho automático de correo |
| `NOTIFICATION_RETRY_INTERVAL_MS` / `mjrenew.notificaciones.intervalo-ms` | `60000` | Intervalo de revisión y demora inicial del scheduler |

La bandeja se registra siempre. Los correos quedan pendientes hasta habilitar su despacho; primero configurar y comprobar SMTP mediante las variables ya utilizadas en RF-002/RF-004 (`SMTP_HOST`, puerto, autenticación, TLS, remitente y `APP_BASE_URL`). La bandera no altera los correos de verificación o recuperación.

RF-027 exige notificar en menos de cinco minutos. La bandeja se persiste al commit y el panel consulta cada minuto mientras la pestaña está visible. Para correo se implementan despacho inmediato y reintento periódico, pero aún debe validarse el tiempo real con SMTP, carga y disponibilidad de la aplicación. No se afirma cumplimiento de recepción externa con la bandera apagada ni con el proveedor sin configurar.

## API y autorización

| Método y ruta | Resultado |
| --- | --- |
| `GET /api/notificaciones/obtenerMisNotificaciones?page=0&size=10&soloNoLeidas=false` | Página del usuario autenticado |
| `GET /api/notificaciones/obtenerResumen` | `{"noLeidas": número}` |
| `POST /api/notificaciones/marcarComoLeida/{notificacionId}` | Aviso propio con fecha de lectura del servidor |

La identidad sale de `UsuarioPrincipal`, nunca de un ID suministrado por el cliente. Se aplica la seguridad vigente de sesión y correo verificado. El POST exige CSRF y devuelve 404 para una notificación ajena o inexistente. Marcar dos veces mantiene la fecha original; la operación bloquea la fila.

Todas las respuestas exitosas usan `Cache-Control: no-store`. El DTO solo incluye ID del aviso, tipo, ID de antigüedad, título, mensaje, fecha de creación y lectura. Omite destinatario, dirección de correo, reservas y estado del transporte. El listado tiene orden estable por creación e ID descendentes, con máximo 50 registros por página.

## Paneles y CSS

La sección **Notificaciones** está al principio de los paneles `/propietario` y `/restaurador`. Incluye contador, filtro de no leídas, botón de actualización, paginación, acceso al detalle de la antigüedad y marca de lectura.

Un fragmento Thymeleaf comparte el HTML. La bandeja permanece oculta hasta verificar la sesión con el servidor. El JavaScript obtiene datos reales de la API, refresca cada minuto con la pestaña visible y limpia el contenido al cambiar de cuenta o recibir 401/403. Usa `textContent` para mostrar los mensajes. Los detalles reutilizan las funciones y permisos actuales de los paneles.

Se agregaron clases propias en `styles.css`, con distribución flexible y respeto del atributo `hidden`. No depende únicamente de los avisos flotantes existentes de `Toast`.

## Migración local

`database/migraciones/2026-11-25__notificaciones_restauracion.sql` fue aplicada una sola vez a `localhost:5432/MJRenew-DB`, después de identificar la base y verificar el respaldo con `pg_restore -l`:

`C:\Users\angel\AppData\Local\Temp\MJRenew-DB_antes_notificaciones_20261125_095657.dump`

Se verificaron columnas, tipos, PK, FK, restricciones y los tres índices de consulta. En otro entorno aplicar la migración antes del arranque; `ddl-auto=validate` no crea la tabla. No se generan avisos retroactivos de operaciones anteriores.

## Archivos creados

Rutas relativas a la raíz del repositorio:

| Archivo | Responsabilidad |
| --- | --- |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/notificacion/TipoNotificacion.java` | Cuatro eventos, textos y panel de destino |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/notificacion/EstadoCorreoNotificacion.java` | Estados del transporte |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/notificacion/NotificacionRestauracionEmitida.java` | Evento con referencias y destinatario |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/notificacion/config/NotificacionConfig.java` | Reloj UTC inyectable |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/notificacion/entity/Notificacion.java` | Entidad de bandeja y cola |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/notificacion/repository/NotificacionRepository.java` | Consultas JPA, inserción idempotente y reserva concurrente |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/notificacion/dto/NotificacionResponse.java` | Datos públicos del aviso propio |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/notificacion/dto/EnvioNotificacion.java` | Datos internos del envío, con `toString()` reservado |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/notificacion/mapper/NotificacionMapper.java` | Mapeo con MapStruct |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/notificacion/controller/NotificacionController.java` | Consulta y lectura de avisos propios |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/notificacion/service/NotificacionService.java` | Persistencia atómica, bandeja y lectura |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/notificacion/service/ColaNotificacionService.java` | Reserva, recuperación y confirmación en transacciones cortas |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/notificacion/service/CorreoNotificacionService.java` | Transporte SMTP sin transacción SQL |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/notificacion/service/EntregaNotificacionService.java` | Procesamiento de lotes y fallos |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/notificacion/service/DespachadorNotificacion.java` | Despacho posterior al commit y revisión periódica |
| `src/main/resources/templates/fragmentos/bandeja-notificaciones.html` | Bandeja compartida |
| `src/main/resources/static/js/bandeja-notificaciones.js` | Consulta, filtro, paginación, detalle y lectura |
| `src/test/java/com/mjrenew/mjrenew_backend/nucleo/notificacion/NotificacionIntegrationTest.java` | Operaciones reales, seguridad, rollback y cola con PostgreSQL |
| `src/test/java/com/mjrenew/mjrenew_backend/nucleo/notificacion/DespachoNotificacionIntegrationTest.java` | Listener real después del commit y sin despacho en rollback |
| `database/migraciones/2026-11-25__notificaciones_restauracion.sql` | Tabla, restricciones e índices |
| `docs/implementaciones/notificaciones-restauracion/README.md` | Contrato, configuración y validación |

## Archivos modificados

| Archivo | Método o bloque |
| --- | --- |
| `src/main/java/com/mjrenew/mjrenew_backend/propietario/service/AntiguedadService.java` | Constructor y `seleccionarRestaurador`: bloqueo y evento para el restaurador |
| `src/main/java/com/mjrenew/mjrenew_backend/restaurador/service/RestauradorService.java` | Constructor, `generarCotizacion`, `publicarAvanceRestauracion`, `guardarCierreRestauracion` y helper de evento para el propietario |
| `src/main/resources/application.properties` | Bandera de correo e intervalo de revisión |
| `build.gradle` | Desactiva el despacho automático en pruebas aunque el entorno de desarrollo lo habilite; el caso del listener lo activa explícitamente y sustituye el procesador |
| `src/main/resources/templates/pages/propietario.html` | Fragmento de bandeja y carga del JavaScript |
| `src/main/resources/templates/pages/restaurador.html` | Fragmento de bandeja y carga del JavaScript |
| `src/main/resources/static/css/styles.css` | Estilos compartidos del panel y sus elementos |
| `src/main/java/com/mjrenew/mjrenew_backend/transportista/service/TrasladoAntiguedadService.java` | Normaliza las tres marcas de agenda a microsegundos, igual que la ejecución de intentos fallidos, para evitar un fallo intermitente por redondeo en PostgreSQL |
| `src/test/java/com/mjrenew/mjrenew_backend/transportista/AcuseRecoleccionIntegrationTest.java` | Caso determinista que persiste agenda y ejecución separadas por 100 nanosegundos cerca del cambio de segundo; conserva la restricción de orden temporal |
| `src/test/java/com/mjrenew/mjrenew_backend/propietario/DecisionEnTallerServiceTest.java` | Dependencia de eventos en pruebas unitarias |
| `src/test/java/com/mjrenew/mjrenew_backend/restaurador/RestauradorServiceFlujoTest.java` | Dependencia de eventos en pruebas unitarias |

## Validación

`./gradlew.bat --offline test --console=plain`: **238 pruebas, 0 fallos, 0 errores y 0 omitidas**. Se agregaron 19 casos de notificaciones y uno de precisión de traslados.

La revisión de la cola excluye destinatarios sin verificar para que no retrasen avisos de otras cuentas. Durante la suite apareció además un fallo intermitente anterior de traslados: la agenda se redondeaba a microsegundos en PostgreSQL, mientras el intento fallido se truncaba, pudiendo quedar un microsegundo antes. Se normalizó la precisión de agenda, conservando la restricción de base de datos, y se añadió una reproducción determinista que también pasó.

Se prueban los cuatro servicios reales y sus destinatarios; avances distintos; cierre repetido; rollback; eventos duplicados concurrentes; aislamiento entre usuarios; lectura idempotente y filtros; paginación; sesión y CSRF; envío sin transacción; fallo y reintento al minuto; reserva vencida y confirmación antigua; trabajadores concurrentes; `SKIP LOCKED`; lectura durante SMTP lento; destinatario pendiente; renderizado y CSS; y listener habilitado después del commit.

También se ejecutó una comprobación con Node del JavaScript real: carga autenticada, texto sin HTML, contador, detalle, marca de lectura y limpieza al cambiar de cuenta. El correo fue simulado; no se enviaron mensajes reales. La prueba de cola usa un reloj anterior a la creación del módulo para no reservar notificaciones reales del desarrollo; la prueba del listener sustituye el procesador de correo.

Commit sugerido: `feat(notificaciones): registrar avisos de restauración según RF-027`.
