# Verificación de correo — RF-002

Implementación del 24 de noviembre de 2026. Fuente de negocio: RF-001, RF-002 y RF-003 de `docs/Guia-Instrucciones/ERS_MJRenew_v4.md`.

## Comportamiento

1. El registro público conserva los tres roles permitidos, BCrypt y `UsuarioResponse`. Crea una cuenta activa administrativamente, con correo pendiente de verificar. No inicia sesión.
2. En la misma transacción se genera un token aleatorio de 256 bits. PostgreSQL solo conserva su hash SHA-256, fecha de emisión y vencimiento a las 24 horas.
3. Un evento de Spring envía el correo después del commit, mediante Spring Mail y sin una transacción SQL abierta. Un rollback no envía correo.
4. El enlace abre `/verificar-correo#token=...`. El fragmento no llega al servidor; JavaScript lo retira de la barra de direcciones y conserva el token en memoria. La persona confirma con el botón. Recargar esta página requiere abrir de nuevo el enlace del correo.
5. La confirmación comprueba vigencia y cuenta activa, marca el correo como verificado y elimina el token en una transacción. No inicia sesión automáticamente. Un enlace consumido, reemplazado o vencido se rechaza con HTTP 400.
6. El reenvío renueva el token y sus 24 horas de vigencia; invalida el anterior. Como protección técnica, cada cuenta puede generar un correo por minuto. La respuesta es igual para correos desconocidos, verificados, inactivos o pendientes.
7. El inicio de sesión comprueba la contraseña antes de informar que el correo está pendiente. Si es incorrecta, mantiene el mensaje genérico. Si es correcta y falta verificar, responde HTTP 403 con `error: CORREO_NO_VERIFICADO`; la pantalla ofrece solicitar otro enlace.
8. La autorización de la API también bloquea sesiones anteriores cuyo principal conserve el correo sin verificar, para cualquiera de los cinco roles. El catálogo público sigue accesible sin registro.

La emisión y el consumo se serializan con bloqueo pesimista del usuario. Dos confirmaciones concurrentes solo consumen el enlace una vez; dos reenvíos concurrentes solo generan un correo nuevo dentro del minuto.

## Contrato de API

El PDF de API v4 documenta registro, login y logout; no incluye operaciones de verificación. Se añaden las siguientes rutas bajo el mismo controlador, conservando las existentes:

| Método y ruta | Solicitud JSON | Resultado |
| --- | --- | --- |
| `POST /api/auth/verificarCorreo` | `{"token":"..."}` | 200 con mensaje; 400 si el enlace no es válido o venció |
| `POST /api/auth/reenviarVerificacionCorreo` | `{"correoElectronico":"..."}` | 200 con respuesta genérica |

Ambas son públicas y exigen CSRF de la misma sesión. Para un navegador nuevo, obtener primero `GET /api/auth/csrf`. Un GET no consume el enlace ni envía correo. Las respuestas de verificación y reenvío incluyen `Cache-Control: no-store`.

## Configuración SMTP

Se agregó `spring-boot-starter-mail`, administrado por la versión de Spring Boot del proyecto. Configurar mediante variables de entorno o el archivo local excluido de Git:

| Variable | Uso / valor predeterminado |
| --- | --- |
| `APP_BASE_URL` | Origen público de los enlaces; predeterminado `http://localhost:8080` |
| `SMTP_HOST` | Servidor SMTP; predeterminado `localhost` |
| `SMTP_PORT` | Puerto; predeterminado `1025` para un servidor local de pruebas |
| `SMTP_USERNAME` | Usuario SMTP; predeterminado vacío |
| `SMTP_PASSWORD` | Contraseña SMTP; predeterminado vacío |
| `SMTP_AUTH` | `true` si el proveedor requiere autenticación; predeterminado `false` |
| `SMTP_STARTTLS` | `true` para habilitar y exigir STARTTLS; predeterminado `false` para pruebas locales |
| `MAIL_FROM` | Remitente autorizado por el proveedor; predeterminado `no-reply@mjrenew.mx` |

Usar los parámetros del proveedor. Si requiere TLS implícito, configurar además `spring.mail.properties.mail.smtp.ssl.enable=true` y el puerto correspondiente en el archivo local. No guardar credenciales reales en Git. Revisar valores explícitos de `app.base-url` en `application-local.properties`, porque pueden prevalecer sobre el archivo general.

La conexión, lectura y escritura SMTP tienen límites de cinco segundos. Si SMTP falla, la cuenta y el token se conservan pendientes y se registra una advertencia sin dirección, enlace ni detalles del proveedor. El usuario puede solicitar reenvío pasado un minuto. Esta entrega no incorpora una cola persistente ni reintentos automáticos de correo; una interrupción entre el commit y el envío también se recupera mediante reenvío.

**Pendiente externo:** configurar SMTP y comprobar recepción en un buzón real. No se verificó entrega externa ni reputación del dominio. La recuperación de contraseña RF-004 y el módulo de notificaciones RF-027 siguen siendo trabajos posteriores.

## Base de datos

Crear con `database/migraciones/2026-11-24__verificacion_correo.sql` antes de arrancar en otro entorno: `ddl-auto=validate` no crea la tabla. Incluye PK por usuario, FK a usuarios, hash único y restricción de vigencia. No modifica el indicador de verificación de cuentas existentes; las que tengan `false` deberán verificar su correo.

Aplicada y verificada en `localhost:5432/MJRenew-DB`, después de comprobar el respaldo con `pg_restore -l`:

`C:\Users\angel\AppData\Local\Temp\MJRenew-DB_antes_verificacion_correo_20261124_121739.dump`

## Archivos creados

Rutas relativas a la raíz del repositorio:

| Archivo | Responsabilidad |
| --- | --- |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/auth/config/VerificacionCorreoConfig.java` | Reloj UTC y comprobación posterior a la contraseña en Spring Security |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/auth/dto/VerificarCorreoRequest.java` | Validación del token recibido |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/auth/dto/ReenviarVerificacionCorreoRequest.java` | Validación del correo para reenvío |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/auth/entity/VerificacionCorreo.java` | Persistencia de hash y vigencia |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/auth/repository/VerificacionCorreoRepository.java` | Consultas Spring Data JPA |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/auth/service/VerificacionCorreoService.java` | Emisión, reenvío, vencimiento y consumo |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/auth/service/VerificacionCorreoEmitida.java` | Evento transaccional sin exponer datos en `toString()` |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/auth/service/CorreoVerificacionService.java` | Envío con Spring Mail después del commit |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/exception/CorreoNoVerificadoException.java` | Error de autenticación para el correo pendiente |
| `src/main/resources/templates/pages/verificar-correo.html` | Pantalla pública de confirmación y reenvío |
| `src/main/resources/static/js/verificacion-correo.js` | Confirmación, retiro del fragmento y reenvío |
| `src/test/java/com/mjrenew/mjrenew_backend/nucleo/auth/VerificacionCorreoIntegrationTest.java` | Pruebas con PostgreSQL y MockMvc, simulando SMTP |
| `database/migraciones/2026-11-24__verificacion_correo.sql` | Nueva tabla |
| `docs/implementaciones/verificacion-correo/README.md` | Contrato, configuración y validación |

## Archivos modificados

| Archivo | Bloque modificado |
| --- | --- |
| `build.gradle` | Dependencia de Spring Mail |
| `src/main/resources/application.properties` | URL base, SMTP y remitente mediante variables de entorno |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/auth/service/AuthService.java` | Constructor y `registrar`: emisión dentro de la transacción del registro |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/auth/controller/AuthController.java` | Constructor y dos endpoints nuevos |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/usuario/repository/UsuarioRepository.java` | Proyección del ID por correo para bloquear después sin reutilizar una entidad anterior al bloqueo |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/security/SecurityConfig.java` | Rutas públicas nuevas y autorización de cuentas verificadas |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/exception/GlobalExceptionHandler.java` | Error de correo pendiente, autenticación rechazada y método HTTP incorrecto (405 con Allow) |
| `src/main/java/com/mjrenew/mjrenew_backend/paginas/controller/PageController.java` | Vista de verificación |
| `src/main/resources/static/js/autenticacion.js` | Registro sin login automático, error de correo pendiente y solicitudes con CSRF |
| `src/main/resources/templates/pages/registro.html` | `submitRegistro`: dirige a verificar en lugar del panel |
| `src/main/resources/templates/pages/login.html` | Ofrece solicitar otro enlace cuando el backend indica correo pendiente |
| `src/test/java/com/mjrenew/mjrenew_backend/nucleo/auth/AuthServiceRegistroTest.java` | Comprueba emisión al registrar |
| `src/test/java/com/mjrenew/mjrenew_backend/nucleo/auth/SeguridadCsrfTest.java` | Dependencia nueva del controlador |
| `src/test/java/com/mjrenew/mjrenew_backend/restaurador/CierreRestauracionControllerTest.java` | Sesiones de prueba legítimas con correo verificado |
| `src/test/java/com/mjrenew/mjrenew_backend/transportista/AcuseRecoleccionIntegrationTest.java` | Usuarios de prueba legítimos con correo verificado |
| `src/test/java/com/mjrenew/mjrenew_backend/transportista/EntregaPropietarioIntegrationTest.java` | Usuarios de prueba legítimos con correo verificado |

## Validación realizada

`./gradlew.bat --offline test --console=plain`: **200 pruebas, 0 fallos, 0 errores y 0 omitidas**.

La nueva clase ejecuta 19 casos incluyendo parametrizaciones: registro, persistencia del hash, envío después del commit sin transacción, rollback, login pendiente/incorrecto/correcto, sesión persistente, enlace de un solo uso, vencimiento exacto y previo a las 24 horas, reenvío con nueva vigencia, límite de frecuencia, respuesta genérica, cuenta inactiva, fallo SMTP recuperable, confirmación y reenvío concurrentes, bloqueo de sesiones anteriores de los cinco roles, validación, CSRF, GET sin efectos y renderizado de las tres vistas.

También se ejecutó una comprobación del JavaScript con Node: registro sin login automático, reconocimiento del error de correo pendiente, cabeceras CSRF y confirmación desde la pantalla. SMTP se simuló con `JavaMailSender`; no se enviaron correos reales.

Commit sugerido: `feat(auth): implementar verificación de correo según RF-002`.

## Actualización del 25 de noviembre: RF-004

La recuperación de contraseña quedó implementada y probada junto con RF-002. `TokenCuentaService` concentra ahora la generación y el hash; `CorreoCuentaService` concentra el transporte SMTP sin transacción. La verificación conserva sus rutas, tokens de 24 horas, bloqueo de acceso y evento posterior al commit.

La suite completa pasó con 218 pruebas. Consultar `docs/implementaciones/recuperacion-contrasena/README.md` para el contrato nuevo, migración, revocación de sesiones y lista de archivos. La recepción de correos reales sigue pendiente de configurar SMTP.
