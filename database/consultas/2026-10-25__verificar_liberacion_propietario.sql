-- Consulta de solo lectura para revisar las liberaciones pendientes de ventas de catálogo.
-- No modifica estados ni ejecuta transferencias.
SELECT
    liberacion.transaccion_id AS liberacion_id,
    liberacion.tipo_transaccion AS tipo_liberacion,
    liberacion.estado_transaccion AS estado_liberacion,
    liberacion.monto_bruto_mxn_transaccion AS importe_bruto_referencia_mxn,
    liberacion.transaccion_padre_id AS pago_venta_id,
    pago.estado_transaccion AS estado_pago_venta,
    pago.referencia_stripe_transaccion AS checkout_stripe_id,
    liberacion.beneficiario_id AS propietario_beneficiario_id,
    liberacion.antiguedad_id,
    liberacion.catalogo_antiguedad_id,
    liberacion.creada_en_transaccion,
    liberacion.procesada_en_transaccion
FROM public.transacciones_bancarias AS liberacion
         JOIN public.transacciones_bancarias AS pago
              ON pago.transaccion_id = liberacion.transaccion_padre_id
WHERE liberacion.tipo_transaccion::text = 'LIBERACION_PROPIETARIO'
  AND pago.tipo_transaccion::text = 'PAGO_VENTA'
ORDER BY liberacion.creada_en_transaccion DESC;
