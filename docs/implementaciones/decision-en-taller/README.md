# Decisión en taller

Después de que el restaurador marca la pieza como `RESTAURACION_LISTA`, el propietario usa
`POST /api/propietario/abrirDecisionEnTaller/{antiguedadId}` para pasar a
`DECISION_EN_TALLER`. Desde ahí puede elegir una sola ruta:

| Acción | Endpoint | Estado siguiente |
| --- | --- | --- |
| Vender desde el taller | `POST /api/propietario/publicarEnCatalogo/{antiguedadId}` con `precioMxnCatalogo` y `direccionRecogidaTaller` | `EN_CATALOGO` |
| Solicitar entrega a domicilio | `POST /api/propietario/solicitarDevolucionTaller/{antiguedadId}` con fecha y horario | `SOLICITUD_DEVOLUCION_TALLER` |
| Confirmar recogida personal | `POST /api/propietario/confirmarRecogidaPersonal/{antiguedadId}` | `DEVUELTA_DESDE_TALLER` |

El propietario registra fecha y horario mediante el siguiente cuerpo JSON.

```json
{
  "fechaAcordadaTraslado": "2026-12-01",
  "horaInicioAcordadaTraslado": "10:00",
  "horaFinAcordadaTraslado": "12:00"
}
```

La fecha y la hora de inicio deben ser futuras en la zona `America/Merida`.
La solicitud guarda el horario y las direcciones de los perfiles, inicialmente sin
transportista. Repetirla conserva el mismo intento. El propietario puede actualizar
el horario mientras la solicitud esté sin asignar, incluso si la ventana anterior venció.
El taller registra su dirección privada desde el perfil del restaurador y el
propietario confirma su domicilio desde el formulario de entrega.

Administración consulta `GET /api/administrador/obtenerEntregasPropietarioPendientesAsignacion`
con paginación y asigna mediante
`POST /api/administrador/asignarTransportistaEntregaPropietario/{trasladoId}`
con `{"transportistaId":"UUID"}`. La asignación valida que el transportista esté
activo y que el horario siga vigente. La antigüedad pasa a `HORARIO_ENTREGA_AGENDADO`.
La operación `crearTraslado` exige usar esta asignación para las entregas al propietario.

Si la entrega falla porque no hay nadie en casa, la antigüedad vuelve a
`SOLICITUD_DEVOLUCION_TALLER` y el propietario elige otro horario. El tercer intento
fallido pasa a `CANCELADA_ENTREGA`, según RF-018. Tras una entrega exitosa,
el propietario confirma la recepción mediante
`POST /api/propietario/conservarRestauracion/{antiguedadId}`. La confirmación pasa a
`RESTAURACION_COMPLETADA` y autoriza la liberación al restaurador. Desde ese estado
puede publicar desde su domicilio sin `direccionRecogidaTaller`. Confirmar la recogida
personal también autoriza la liberación. El equipo decidió que vender directamente
desde el taller mantiene el pago de restauración en depósito hasta definir su liberación.
El pago de la venta al propietario sigue las reglas de RF-023.

La dirección del taller queda guardada en `catalogo_antiguedad` y se usa como
origen al programar la entrega al comprador. Para publicaciones desde el domicilio
se sigue usando la dirección del propietario. Antes de iniciar la aplicación con
`spring.jpa.hibernate.ddl-auto=validate`, aplicar
`database/migraciones/2026-11-08__direccion_recogida_taller_catalogo.sql` en cada base.
La migración ya se aplicó en la base local `MJRenew-DB` el 8 de noviembre de 2026.
