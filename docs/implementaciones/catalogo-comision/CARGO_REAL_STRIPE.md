# Conciliación del cargo real de Stripe en ventas del catálogo

**Estado:** implementado en código y migración aplicada a `MJRenew-DB` local el 6 de noviembre de 2026. Suite local: 77 pruebas, sin fallos. No se ha probado contra una venta real de Stripe.

## Fuente del cargo

Con la sesión Checkout guardada en `PAGO_VENTA`, la tarea consulta `payment_intent`, su `latest_charge` y la `balance_transaction` de ese cargo. El campo `fee` de esta última es el cargo real en la unidad mínima de su moneda. No se usa el supuesto porcentual S-05 del ERS. Referencias de la API de Stripe: [Checkout Session](https://docs.stripe.com/api/checkout/sessions/object), [PaymentIntent](https://docs.stripe.com/api/payment_intents/object), [Charge](https://docs.stripe.com/api/charges/object), [Balance Transaction](https://docs.stripe.com/api/balance_transactions/object).

La tarea está **apagada por defecto** y se habilita con `mjrenew.stripe.fee-reconciliation-enabled=true` tras verificarla en Stripe test mode. Tiene espera inicial de 120 segundos y repetición cada 5 minutos por defecto (`mjrenew.stripe.fee-reconciliation-delay-ms`). Solo examina liberaciones pendientes sin cargo registrado. Una transacción de saldo aún no publicada se reintenta después. Las llamadas a Stripe ocurren fuera de una transacción JDBC.

Antes de guardar, se comprueba la relación sesión → PaymentIntent → cargo → transacción de saldo, los metadatos de la transacción interna, estado pagado, importe, moneda MXN, modo de Stripe y ausencia de reembolso o disputa en el momento de la consulta. Un bloqueo de fila serializa reintentos. El neto guardado es `bruto − comisión MJ Renew − cargo Stripe`; debe ser positivo. Si Stripe devuelve datos distintos después de conciliar, se rechaza la modificación automática y se requiere revisión.

La migración `database/migraciones/2026-11-06__cargo_real_stripe_catalogo.sql` agrega a `transacciones_bancarias`: `cargo_stripe_mxn`, `monto_neto_propietario_mxn`, `stripe_payment_intent_id`, `stripe_charge_id` y `stripe_balance_transaction_id`. Son nulos en operaciones anteriores hasta que la tarea las concilie. Cada otra base que use este código necesita aplicar la migración antes de arrancar con `ddl-auto=validate`. El respaldo local previo está en `C:\Users\angel\AppData\Local\Temp\MJRenew-DB_pre_cargo_stripe_20261106_113903.dump`.

## Límite del cambio

No se crea una transferencia ni se marca `LIBERACION_PROPIETARIO` como exitosa. El importe neto refleja la comisión del cargo consultado; reembolsos, disputas o ajustes posteriores pueden cambiar la situación de fondos. Antes de transferir, se debe definir con el equipo la política de fondos y volver a verificar el estado del cargo, además de persistir referencia, resultado e idempotencia de la transferencia. La suite valida cálculo, vínculo con el pago e idempotencia con datos simulados; falta una prueba integrada con Stripe en modo de pruebas.
