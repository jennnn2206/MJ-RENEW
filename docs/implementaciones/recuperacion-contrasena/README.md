# Recuperación de contraseña — RF-004

Implementado el 25 de noviembre de 2026, a partir de RF-004 del ERS y los requisitos de contraseña de RF-001. El contrato de API v4 documenta registro, inicio y cierre de sesión; se amplía `AuthController` con las operaciones necesarias de recuperación, conservando sus rutas existentes.

## Flujo y decisiones técnicas

1. En `/login`, el enlace **Olvidé mi contraseña** abre `/recuperar-contrasena`.
2. La solicitud recibe siempre la misma respuesta para un correo desconocido, una cuenta inactiva, una cuenta elegible o una solicitud limitada por frecuencia. Las cuentas activas pueden recuperar su contraseña aunque su correo siga pendiente de verificar.
3. Se genera un token de 256 bits; solo se guarda su hash SHA-256. Sus registros están separados de los tokens de verificación de correo y no son intercambiables.
4. El correo se intenta enviar después del commit y sin una transacción SQL abierta. Un rollback no envía correo. El enlace usa `/restablecer-contrasena#token=...`; el fragmento no llega al servidor. La pantalla lo retira del historial y conserva el token en memoria hasta completar el formulario. Para recargar, volver a abrir el enlace del correo.
5. El usuario elige y confirma la contraseña. El backend exige al menos ocho caracteres, rechaza valores en blanco y aplica el límite real de BCrypt de 72 bytes UTF-8. Los mensajes de la pantalla explican que debe elegir una contraseña más corta si supera el límite.
6. Con bloqueo pesimista del usuario, se valida el enlace, se guarda la contraseña con `PasswordEncoder`/BCrypt y se elimina el token en una misma transacción. El correo verificado y el estado activo conservan sus valores.
7. El éxito permite ir al login con la nueva contraseña. No crea una sesión automáticamente. Una cuenta pendiente todavía debe verificar su correo; una cuenta inactiva no puede cambiar la contraseña mediante este enlace.
8. La siguiente petición API de una sesión con la contraseña anterior borra su contexto de seguridad e invalida su sesión. Las demás cuentas conservan el acceso.

**Vigencia:** RF-004 pide una duración limitada sin fijar minutos. Se adopta como parámetro técnico un valor predeterminado de **30 minutos**, configurable con `PASSWORD_RESET_TTL_MINUTES` o `mjrenew.auth.recuperacion.vigencia-minutos`. `@ConfigurationProperties` y Bean Validation admiten de 1 a 1440 minutos. No modifica las 24 horas de RF-002.

**Reenvío:** una emisión por minuto por cuenta como protección técnica. Una solicitud posterior al minuto reemplaza el token y renueva la vigencia; el enlace anterior deja de servir. El bloqueo del usuario evita que dos solicitudes concurrentes emitan dos correos. Dos cambios simultáneos con el mismo token solo permiten un ganador.

## API

| Método y ruta | JSON | Respuesta |
| --- | --- | --- |
| `POST /api/auth/solicitarRecuperacionContrasena` | `{"correoElectronico":"usuario@example.invalid"}` | 200 con mensaje genérico |
| `POST /api/auth/restablecerContrasena` | `{"token":"...","nuevaContrasena":"..."}` | 200 con confirmación; 400 por datos o enlace inválidos |

Ambas rutas son públicas, exigen JSON y CSRF de la misma sesión, y responden con `Cache-Control: no-store` en caso de éxito. Para preparar CSRF, obtener `GET /api/auth/csrf`. Un GET a estas operaciones devuelve 405 y no cambia datos ni envía correos.

La respuesta genérica cubre el contenido del mensaje; no se incorpora un mecanismo para igualar tiempos de respuesta. No se incluye una cola persistente ni reintentos automáticos de SMTP. Ante un fallo del proveedor o una interrupción entre commit y envío, el usuario puede solicitar otro enlace pasado un minuto. El log omite destinatario, token, contraseña y detalles del error SMTP.

## Infraestructura compartida y sesiones

`TokenCuentaService` concentra generación y hash para RF-002 y RF-004. `CorreoCuentaService` reutiliza Spring Mail, remitente y URL base. Los listeners de cada propósito conservan su evento `AFTER_COMMIT` y delegan en el transporte compartido con `NOT_SUPPORTED`. No se agregaron dependencias.

`SesionCredencialesFilter` se registra una sola vez dentro de la cadena de Spring Security, después de `SecurityContextHolderFilter`. En peticiones `/api/` con un principal verificado consulta mediante Spring Data JPA si su ID y hash todavía corresponden a una cuenta activa y verificada. Si cambió la contraseña, invalida la sesión antes de autorizar el recurso. Esto cubre sesiones anteriores a esta implementación y distintas instancias conectadas a la misma base, sin un registro de sesiones en memoria. Añade una consulta por petición API autenticada. Las operaciones en curso no se cancelan retroactivamente.

Las sesiones pendientes de verificación siguen siendo rechazadas por la autorización ya existente de RF-002. Las vistas mantienen la arquitectura actual: el HTML público no contiene datos privados y los obtiene mediante la API protegida.

## Pantallas y CSS

Con Spring Boot ejecutándose en el puerto predeterminado:

- `http://localhost:8080/recuperar-contrasena`: formulario de solicitud.
- `http://localhost:8080/restablecer-contrasena`: requiere abrir el enlace recibido para mostrar el formulario de nueva contraseña; sin token ofrece solicitar otro.
- `http://localhost:8080/login`: entrada al flujo.

Ambas vistas enlazan `/css/styles.css`. Se agregaron clases compartidas para mensajes, ayuda, acciones y botón, además de `.auth-card [hidden]` para respetar la visibilidad incluso en elementos con estilos de botón. El JavaScript impide enviar contraseñas que no coincidan, conserva el formulario ante errores, limpia los campos al completar y elimina el usuario cacheado en `localStorage`.

## Migración aplicada

`database/migraciones/2026-11-25__recuperacion_contrasena.sql` crea una tabla con PK por usuario, FK, hash único y restricción de vigencia. No cambia usuarios existentes.

Se aplicó una sola vez en `localhost:5432/MJRenew-DB`, después de identificar la base y verificar el respaldo mediante `pg_restore -l`:

`C:\Users\angel\AppData\Local\Temp\MJRenew-DB_antes_recuperacion_contrasena_20261125_093133.dump`

Se verificaron columnas, tipos, nulabilidad, PK, FK, índice único y restricción de fechas. En otros entornos aplicar la migración antes de arrancar; `ddl-auto=validate` no crea la tabla.

## Archivos creados

Rutas relativas a la raíz del repositorio:

| Archivo | Responsabilidad |
| --- | --- |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/auth/config/RecuperacionContrasenaConfig.java` | Reloj UTC y habilitación de propiedades |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/auth/config/RecuperacionContrasenaProperties.java` | Vigencia configurable validada |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/auth/dto/SolicitarRecuperacionContrasenaRequest.java` | Validación del correo |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/auth/dto/RestablecerContrasenaRequest.java` | Validación de token y contraseña; `toString()` reservado |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/auth/entity/RecuperacionContrasena.java` | Hash y vigencia por cuenta |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/auth/repository/RecuperacionContrasenaRepository.java` | Consultas JPA del token |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/auth/service/RecuperacionContrasenaService.java` | Solicitud, reenvío y cambio transaccional |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/auth/service/RecuperacionContrasenaEmitida.java` | Evento con token reservado |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/auth/service/CorreoRecuperacionContrasenaService.java` | Listener del correo de recuperación |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/auth/service/CorreoCuentaService.java` | Transporte de correo compartido sin transacción SQL |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/auth/service/TokenCuentaService.java` | Generación aleatoria y hash compartidos |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/security/SesionCredencialesFilter.java` | Revocación de sesiones con credenciales anteriores |
| `src/main/resources/templates/pages/recuperar-contrasena.html` | Formulario de solicitud |
| `src/main/resources/templates/pages/restablecer-contrasena.html` | Formulario de nueva contraseña |
| `src/main/resources/static/js/recuperacion-contrasena.js` | Interacción de las dos pantallas |
| `src/test/java/com/mjrenew/mjrenew_backend/nucleo/auth/RecuperacionContrasenaIntegrationTest.java` | 18 pruebas con PostgreSQL, MockMvc y SMTP simulado |
| `database/migraciones/2026-11-25__recuperacion_contrasena.sql` | Tabla nueva |
| `docs/implementaciones/recuperacion-contrasena/README.md` | Contrato y evidencia de implementación |

## Archivos modificados

| Archivo | Bloque cambiado |
| --- | --- |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/auth/controller/AuthController.java` | Constructor y dos endpoints nuevos |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/auth/service/VerificacionCorreoService.java` | Emisión y hash delegados en `TokenCuentaService` |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/auth/service/CorreoVerificacionService.java` | Transporte delegado en `CorreoCuentaService`, conservando el evento de RF-002 |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/usuario/repository/UsuarioRepository.java` | Consulta `conservaCredencialesActivas` |
| `src/main/java/com/mjrenew/mjrenew_backend/nucleo/security/SecurityConfig.java` | Rutas públicas de recuperación y registro del filtro de credenciales |
| `src/main/java/com/mjrenew/mjrenew_backend/paginas/controller/PageController.java` | Dos rutas de vistas |
| `src/main/resources/application.properties` | Configuración de vigencia |
| `src/main/resources/static/css/styles.css` | Clases de autenticación y respeto de `hidden` |
| `src/main/resources/static/js/autenticacion.js` | Solicitud y cambio con CSRF |
| `src/main/resources/templates/pages/login.html` | Enlace Olvidé mi contraseña |
| `src/test/java/com/mjrenew/mjrenew_backend/nucleo/auth/SeguridadCsrfTest.java` | Nuevas dependencias del controlador y la cadena de seguridad |
| `src/test/java/com/mjrenew/mjrenew_backend/restaurador/CierreRestauracionControllerTest.java` | Simula credenciales actuales en las pruebas MVC aisladas |
| `docs/implementaciones/verificacion-correo/README.md` | Referencia a la infraestructura compartida de RF-004 |

## Validación

`./gradlew.bat --offline test --console=plain`: **218 pruebas, 0 fallos, 0 errores y 0 omitidas**.

Los 18 casos nuevos comprueban hash y vigencia; envío después del commit y sin transacción; cambio de contraseña y nuevo login; rechazo de la anterior; token consumido; revocación de dos sesiones conservando otra cuenta; respuesta genérica; correo pendiente; cuenta inactiva; vencimiento exacto y previo; reenvío y renovación; separación de propósitos; rollback de solicitud y cambio; fallo SMTP recuperable; solicitudes y cambios concurrentes; validación de formato, mínimo y bytes UTF-8; CSRF y GET sin efectos; renderizado de vistas y disponibilidad del CSS.

Se ejecutó además una comprobación con Node del JavaScript real: endpoints y CSRF, solicitud de enlace, token ausente o válido, retiro del fragmento, confirmación de contraseña, longitud UTF-8, resultado exitoso y limpieza de campos/cache.

**Pendiente externo:** configurar SMTP y validar recepción en un buzón real. Los tests simulan `JavaMailSender`; no enviaron correos reales. Se conservan las variables SMTP documentadas para RF-002.

Commit sugerido: `feat(auth): implementar recuperación de contraseña según RF-004`.
