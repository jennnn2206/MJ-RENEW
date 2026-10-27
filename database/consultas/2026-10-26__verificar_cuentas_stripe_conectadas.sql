-- Verifica la estructura y el estado de las cuentas conectadas.
-- No devuelve claves secretas ni información bancaria.

SELECT
    cuenta_stripe_conectada_id,
    usuario_id,
    stripe_account_id,
    detalles_enviados,
    transferencias_habilitadas,
    payouts_habilitados,
    creada_en,
    actualizada_en
FROM public.cuentas_stripe_conectadas
ORDER BY creada_en DESC;

-- Debe devolver cero filas: cada propietario solo puede tener una cuenta Stripe.
SELECT usuario_id, COUNT(*) AS total
FROM public.cuentas_stripe_conectadas
GROUP BY usuario_id
HAVING COUNT(*) > 1;
