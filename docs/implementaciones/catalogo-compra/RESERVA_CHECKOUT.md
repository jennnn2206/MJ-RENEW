# Reserva de catálogo y creación de Checkout

## Cambio

`CatalogoCompraReservaService.reservar` usa una transacción corta y bloquea la publicación para reservarla y registrar `PAGO_VENTA` pendiente. La llamada a Stripe ocurre después del commit, sin retener el bloqueo de PostgreSQL. `vincularCheckout` guarda la referencia y la URL en otra transacción corta. No se añadieron estados, rutas ni columnas.

El mismo comprador puede reintentar una reserva pendiente: si ya hay referencia, se consulta la sesión abierta; si aún no la hay, se vuelve a usar la misma transacción interna y, por tanto, la misma clave de idempotencia `mjrenew-checkout-{transaccionId}`. Otro comprador no puede crear un segundo Checkout para la misma antigüedad mientras la reserva está pendiente.

La ruta pública de detalle muestra una publicación inactiva únicamente al comprador autenticado que mantiene esa reserva en `LINK_PAGO_ENVIADO`. Así puede recargar la ficha y reintentar el pago. Para visitantes, otros compradores y ventas terminadas, el detalle responde 404.

Si Stripe responde con un error o el vínculo local falla, la API responde 503 y conserva la reserva. Un fallo de comunicación no demuestra que Stripe haya omitido crear la sesión. No se libera la antigüedad automáticamente en ese caso. Un webhook que llegue antes de que la referencia sea visible recibe 503 y Stripe puede reintentarlo.

## Límites y operación

- Una reserva sin referencia Stripe no entra en la conciliación automática actual, que selecciona solo transacciones con referencia. Si el comprador no vuelve a intentar, requiere revisión operativa; no se debe liberarla mediante SQL sin verificar primero Stripe.
- Stripe conserva las claves de idempotencia de API v1 durante al menos 24 horas y puede eliminarlas después. Un error 500 también puede quedar asociado a esa clave. Los reintentos tardíos o errores persistentes requieren revisión del pago en Stripe antes de abrir otra compra.
- La sesión Checkout continúa configurada para expirar aproximadamente 35 minutos después de crear la transacción interna. Este cambio no implementa el límite de siete minutos del ERS.
- Se validaron los caminos de reserva, reintento, referencia y error con pruebas automatizadas. Una prueba con el contexto real de Spring comprueba que la llamada a Stripe suspende incluso una transacción del invocador. Falta una prueba integrada de dos compradores concurrentes con PostgreSQL y una prueba con Stripe en modo de pruebas que cubra tiempos de espera y reenvío del webhook.
