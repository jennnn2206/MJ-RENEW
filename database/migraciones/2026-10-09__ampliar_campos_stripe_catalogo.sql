-- MJ Renew | Migración incremental de PostgreSQL
-- Fecha: 2026-10-09
-- Aplicar sobre una base con el esquema base de MJ Renew ya instalado.
-- Respalda tu base en pgAdmin antes de ejecutar. No insertar datos ni modificar pagos.
-- Diseñada para ser idempotente: solo amplía capacidades; NO reduce columnas mayores.
-- IMPORTANTE: no es un script de creación de la base completa.

BEGIN;

DO $migracion$
DECLARE
nombre_columna text;
    tipo_actual text;
    largo_actual integer;
BEGIN
    IF to_regclass('public.catalogo_antiguedad') IS NULL THEN
        RAISE EXCEPTION 'Falta public.catalogo_antiguedad: primero importa el esquema completo de MJ Renew';
END IF;

    FOREACH nombre_columna IN ARRAY ARRAY['id_link_stripe_catalogo', 'id_pago_stripe_catalogo'] LOOP
SELECT data_type, character_maximum_length
INTO tipo_actual, largo_actual
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'catalogo_antiguedad'
  AND column_name = nombre_columna;

IF NOT FOUND THEN
            RAISE EXCEPTION 'Falta la columna %.%', 'catalogo_antiguedad', nombre_columna;
END IF;

        IF tipo_actual = 'character varying' THEN
            IF largo_actual IS NOT NULL AND largo_actual < 255 THEN
                EXECUTE format('ALTER TABLE public.catalogo_antiguedad ALTER COLUMN %I TYPE VARCHAR(255)', nombre_columna);
                RAISE NOTICE 'Columna % ampliada a VARCHAR(255)', nombre_columna;
ELSE
                RAISE NOTICE 'Columna % ya admite 255 caracteres o más; no se reduce', nombre_columna;
END IF;
        ELSIF tipo_actual = 'text' THEN
            RAISE NOTICE 'Columna % ya es TEXT; no se modifica', nombre_columna;
ELSE
            RAISE EXCEPTION 'Tipo inesperado en catalogo_antiguedad.%: %', nombre_columna, tipo_actual;
END IF;
END LOOP;

SELECT data_type
INTO tipo_actual
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'catalogo_antiguedad'
  AND column_name = 'url_link_pago_catalogo';

IF NOT FOUND THEN
        RAISE EXCEPTION 'Falta catalogo_antiguedad.url_link_pago_catalogo';
END IF;

    IF tipo_actual = 'text' THEN
        RAISE NOTICE 'url_link_pago_catalogo ya es TEXT';
    ELSIF tipo_actual = 'character varying' OR tipo_actual = 'character' THEN
ALTER TABLE public.catalogo_antiguedad
ALTER COLUMN url_link_pago_catalogo TYPE TEXT;
        RAISE NOTICE 'url_link_pago_catalogo ampliada a TEXT';
ELSE
        RAISE EXCEPTION 'Tipo inesperado en url_link_pago_catalogo: %', tipo_actual;
END IF;
END
$migracion$;

COMMIT;

-- Verificación posterior (consulta independiente, solo lectura):
SELECT column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'catalogo_antiguedad'
  AND column_name IN ('id_link_stripe_catalogo', 'id_pago_stripe_catalogo', 'url_link_pago_catalogo')
ORDER BY column_name;
