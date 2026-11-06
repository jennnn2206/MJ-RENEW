-- RF-023: registra la comisión del 7 % de las ventas del catálogo.
-- Aplicar a la base de desarrollo después de respaldarla y antes de iniciar
-- la aplicación con la entidad TransaccionBancaria actualizada.
-- El importe antes del cargo de Stripe NO es el neto transferible.
BEGIN;

ALTER TABLE public.transacciones_bancarias
    ADD COLUMN IF NOT EXISTS comision_mjrenew_mxn NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS monto_antes_cargo_stripe_mxn NUMERIC(10, 2);

-- Completa liberaciones pendientes creadas antes de esta migración.
-- Repetir la migración no sobrescribe desgloses ya existentes.
UPDATE public.transacciones_bancarias
SET comision_mjrenew_mxn = ROUND(monto_bruto_mxn_transaccion * 0.07, 2),
    monto_antes_cargo_stripe_mxn = monto_bruto_mxn_transaccion
        - ROUND(monto_bruto_mxn_transaccion * 0.07, 2)
WHERE tipo_transaccion::text = 'LIBERACION_PROPIETARIO'
  AND monto_bruto_mxn_transaccion > 0
  AND comision_mjrenew_mxn IS NULL
  AND monto_antes_cargo_stripe_mxn IS NULL;

COMMIT;
