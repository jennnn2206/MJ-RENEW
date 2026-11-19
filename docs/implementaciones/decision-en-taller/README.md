# Decisión en taller

Después de que el restaurador marca la pieza como `RESTAURACION_LISTA`, el propietario usa
`POST /api/propietario/abrirDecisionEnTaller/{antiguedadId}` para pasar a
`DECISION_EN_TALLER`. Desde ahí puede elegir una sola ruta:

| Acción | Endpoint | Estado siguiente |
| --- | --- | --- |
| Vender desde el taller | `POST /api/propietario/publicarEnCatalogo/{antiguedadId}` con `precioMxnCatalogo` y `direccionRecogidaTaller` | `EN_CATALOGO` |
| Solicitar entrega a domicilio | `POST /api/propietario/solicitarDevolucionTaller/{antiguedadId}` | `SOLICITUD_DEVOLUCION_TALLER` |
| Confirmar recogida personal | `POST /api/propietario/confirmarRecogidaPersonal/{antiguedadId}` | `DEVUELTA_DESDE_TALLER` |

El administrador solo puede crear un traslado `ENTREGA_PROPIETARIO` después de la
solicitud. Si la entrega falla porque no hay nadie en casa, la pieza vuelve a
`SOLICITUD_DEVOLUCION_TALLER` y se puede reagendar. Tras una entrega exitosa,
el propietario puede publicar desde su domicilio con el endpoint existente, sin
`direccionRecogidaTaller`, o terminar en `RESTAURACION_COMPLETADA` mediante
`POST /api/propietario/conservarRestauracion/{antiguedadId}`.

La dirección del taller queda guardada en `catalogo_antiguedad` y se usa como
origen al programar la entrega al comprador. Para publicaciones desde el domicilio
se sigue usando la dirección del propietario. Antes de iniciar la aplicación con
`spring.jpa.hibernate.ddl-auto=validate`, aplicar
`database/migraciones/2026-11-08__direccion_recogida_taller_catalogo.sql` en cada base.
La migración ya se aplicó en la base local `MJRenew-DB` el 8 de noviembre de 2026.
