ALTER TABLE public.transacciones_bancarias
    ADD COLUMN IF NOT EXISTS monto_neto_restaurador_mxn numeric(10,2),
    ADD COLUMN IF NOT EXISTS destino_transferencia character varying(255),
    ADD COLUMN IF NOT EXISTS primer_intento_transferencia timestamp with time zone;
