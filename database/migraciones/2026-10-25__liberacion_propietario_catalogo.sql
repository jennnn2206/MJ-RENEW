-- Garantiza que PostgreSQL reconozca el tipo de operación para la venta de catálogo.
-- Es idempotente: si el valor ya existe, no modifica el tipo.
ALTER TYPE public.tipo_transaccion
    ADD VALUE IF NOT EXISTS 'LIBERACION_PROPIETARIO';
