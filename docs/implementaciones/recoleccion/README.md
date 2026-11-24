# Acuse de recolección del propietario

RF-015 exige cuatro fotografías de recolección y la confirmación del propietario
de que entregó la antigüedad al transportista. La llegada al taller completa el
traslado después de comprobar ambas evidencias.

## Recorrido

1. El transportista usa `POST /api/transportista/confirmarSalidaRecoleccion/{trasladoId}`
   con al menos cuatro archivos multipart `fotos`. La antigüedad pasa a `EN_RECOLECCION`.
2. El propietario consulta las imágenes del intento desde el detalle de su antigüedad.
   El campo `recoleccion` incluye el número de intento, las fotografías y el acuse.
3. El propietario confirma mediante
   `POST /api/propietario/confirmarEntregaRecoleccion/{trasladoId}`, sin cuerpo.
   La sesión autenticada determina quién confirma. El servidor guarda su identidad y fecha.
4. El transportista completa `POST /api/transportista/confirmarLlegadaTaller/{trasladoId}`.
   Se exigen el acuse y cuatro evidencias `TRASLADO_SALIDA` vinculadas a ese traslado.
   La antigüedad pasa a `RECIBIDO_EN_TALLER`.

Los reenvíos de la confirmación conservan la fecha original. Un intento fallido o
un traslado de otro propietario se rechazan. Las operaciones bloquean primero la
antigüedad y después el traslado. La subida de archivos ocurre fuera de la
transacción SQL, con limpieza de subidas que no llegaron a guardarse.

Después del tercer intento fallido por ausencia, la antigüedad pasa a
`CANCELADA_RECOLECCION`. Administración consulta el historial paginado mediante
`GET /api/administrador/obtenerRecolecciones`, donde se muestra el número de intento.

## Persistencia

La migración `database/migraciones/2026-11-24__acuse_recoleccion_propietario.sql`
agrega `recoleccion_confirmada_en` y `recoleccion_confirmada_por`, una clave foránea
y una restricción de consistencia. Se aplicó en `MJRenew-DB` local tras respaldarla
y verificar el respaldo. Otras bases que utilicen esta versión necesitan aplicar
esa migración antes del arranque con `ddl-auto=validate`.

Los traslados históricos completados conservan el acuse nulo si no se registró.
Los que siguen en curso pueden completar sus evidencias desde el panel del
transportista y obtener el acuse. Se mantienen los estados existentes del AFD.

## Verificación

Las pruebas cubren permisos, CSRF, fotografías del intento correcto, reenvíos,
reversión SQL, llegada concurrente con el acuse y el límite de tres intentos.
Los casos de integración utilizan PostgreSQL y almacenamiento simulado.
