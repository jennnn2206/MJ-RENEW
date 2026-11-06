-- MJ Renew — RF-024: permitir registrar la ventana del comprador antes de asignar transportista.
-- No agrega columnas ni altera estados; solo relaja NOT NULL si la columna aún lo exige.
-- Ejecutar una vez en la base de desarrollo después de respaldarla.

BEGIN;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'traslados_antiguedad'
          AND column_name = 'transportista_id'
          AND is_nullable = 'NO'
    ) THEN
ALTER TABLE public.traslados_antiguedad
    ALTER COLUMN transportista_id DROP NOT NULL;
END IF;
END $$;

COMMIT;
