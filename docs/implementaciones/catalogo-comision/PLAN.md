# Integración de la comisión de ventas del catálogo

**Estado:** implementado en código y migración aplicada en la base local `MJRenew-DB` el 6 de noviembre de 2026; suite de pruebas ejecutada correctamente.
**Base revisada:** rama `Arquitectura-Roles`, commit `7ab9f0d`.

## Regla y estado actual

El ERS RF-023 fija una comisión de MJ Renew del 7 % sobre una venta del catálogo. El propietario recibe el importe de la venta después de descontar esa comisión y el cargo de Stripe. El cargo real de Stripe no debe sustituirse por el supuesto S-05 del ERS.

`ComisionVentaCatalogoService.calcular` obtiene el 7 % y el restante antes del cargo de Stripe. `StripePagoEstadoService.confirmar` marca `PAGO_VENTA` como exitoso y llama a `LiberacionPropietarioService.registrarPendiente`, que crea `LIBERACION_PROPIETARIO` con el importe bruto como referencia. Ese registro ahora guarda también el desglose, una vez aplicada la migración. Todavía no hay transferencia a la cuenta conectada.

## Cambio preparado

1. Conservar `monto_bruto_mxn_transaccion` como importe bruto tanto en `PAGO_VENTA` como en `LIBERACION_PROPIETARIO`; no reinterpretar esa columna como importe neto.
2. Agregar a `transacciones_bancarias` dos columnas decimales, inicialmente nulas para operaciones anteriores y de otros tipos:
   - `comision_mjrenew_mxn`: comisión del 7 % calculada al confirmar la venta.
   - `monto_antes_cargo_stripe_mxn`: bruto menos comisión, sin llamarlo importe transferible.
3. Al registrar la liberación pendiente, calcular el desglose con `ComisionVentaCatalogoService` y persistir esos dos valores en la misma transacción. Mantener la operación idempotente: reenvíos del webhook no deben crear otra liberación ni recalcular una ya existente.
4. Probar el cálculo y su persistencia en `LiberacionPropietarioServiceTest`, incluidos los reintentos. Ejecutar la suite completa.

La migración `database/migraciones/2026-11-06__desglose_comision_catalogo.sql` agrega las columnas y completa el desglose de liberaciones anteriores que no lo tengan. Se aplicó tras verificar un respaldo de la base local; no había liberaciones anteriores que actualizar. La suite completa terminó con 68 pruebas correctas, sin fallos ni errores.

Este primer cambio no ejecutaría transferencias, no cambiaría el estado de la antigüedad ni modificaría las rutas de la API.

## Conciliación posterior del cargo de Stripe

La consulta del cargo real y su registro se implementaron en el cambio siguiente, descrito en [CARGO_REAL_STRIPE.md](CARGO_REAL_STRIPE.md). La transferencia al propietario sigue pendiente de diseño e implementación; el importe neto registrado es una fotografía del cargo al momento de conciliarlo, no una autorización para transferir.

## Aplicación en PostgreSQL

La usuaria aprobó la adición de las dos columnas. Se identificó `MJRenew-DB` en PostgreSQL local, se creó y verificó un respaldo, se aplicó el SQL y se comprobó que las columnas existen. El test de arranque pasó con `ddl-auto=validate`. Cada otra base de datos que use esta versión del código requiere la misma migración antes de iniciar la aplicación.
