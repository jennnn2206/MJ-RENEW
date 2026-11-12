# Seguridad mínima del backend para una futura demo

**Alcance de este avance:** cierre del registro público de roles internos y protección CSRF para peticiones que usan la sesión del navegador. Esto no completa el «Módulo de gestión de roles y permisos» previsto para la monografía.

## Registro público

`POST /api/auth/registrarUsuario` acepta únicamente `PROPIETARIO`, `RESTAURADOR` y `COMPRADOR`, las tres opciones visibles en el formulario. Una solicitud con `ADMINISTRADOR` o `TRANSPORTISTA` responde 400 antes de consultar o guardar el usuario. Las cuentas internas de la demo se siguen preparando solo con el perfil `demo`.

## CSRF y navegador

El backend exige token CSRF en las operaciones que modifican datos, incluido registro, inicio y cierre de sesión. Solo `/webhook/stripe` queda excluido porque valida la firma de Stripe. `GET /api/auth/csrf` entrega el nombre de cabecera y el token de la sesión, con `Cache-Control: no-store`. `autenticacion.js` y `api.js` solicitan el token antes de cada POST y lo envían con la misma sesión. Los listados GET no necesitan token.

## Validación

La suite completa de Gradle terminó con 81 pruebas correctas el 12 de noviembre de 2026. Las nuevas pruebas comprueban que los roles internos no se registran y que un POST sin token, o con token de otra sesión, recibe 403. Ambos archivos JavaScript modificados pasan `node --check`.

## Antes de publicar una demo

La página de login todavía muestra credenciales de prueba, incluida la de administrador, y el perfil `demo` crea esa cuenta con contraseña conocida. No se debe exponer así a prospectos. Falta definir cuentas y datos de demostración separados, HTTPS, configuración de cookies para el entorno desplegado y pruebas desde un navegador externo. La auditoría completa de permisos por rol y recurso se mantiene en el alcance de la monografía.
