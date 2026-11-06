-- RF-023: concilia el cargo de Stripe con una venta del catálogo.
-- Los campos permanecen NULL hasta consultar la transacción de saldo real.
-- Esta migración no inicia transferencias.
BEGIN;

ALTER TABLE public.transacciones_bancarias
    ADD COLUMN IF NOT EXISTS cargo_stripe_mxn NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS monto_neto_propietario_mxn NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS stripe_payment_intent_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS stripe_charge_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS stripe_balance_transaction_id VARCHAR(100);

COMMIT;
