-- MJ Renew | Migración incremental de PostgreSQL
-- Fecha: 2026-10-20
-- Aplicar sobre una base con el esquema base de MJ Renew ya instalado.
-- Respalda tu base en pgAdmin antes de ejecutar.
-- Agrega el tipo de fotografía TRASLADO_INTENTO_FALLIDO (evidencia opcional
-- cuando el transportista registra que no había nadie en casa).
-- Diseñada para ser idempotente: se puede ejecutar más de una vez.
-- IMPORTANTE: no es un script de creación de la base completa.

BEGIN;

DO $migracion$
DECLARE
    tipo_columna text;
    nombre_restriccion text;
BEGIN
    IF to_regclass('public.fotografias_antiguedad') IS NULL THEN
        RAISE EXCEPTION 'Falta public.fotografias_antiguedad: primero importa el esquema completo de MJ Renew';
    END IF;

    SELECT data_type
    INTO tipo_columna
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'fotografias_antiguedad'
      AND column_name = 'tipo_fotografia';

    IF tipo_columna = 'USER-DEFINED' THEN
        -- Esquema con enum nativo de PostgreSQL (tipo tipo_fotografia).
        ALTER TYPE public.tipo_fotografia ADD VALUE IF NOT EXISTS 'TRASLADO_INTENTO_FALLIDO';
        RAISE NOTICE 'Enum tipo_fotografia: valor TRASLADO_INTENTO_FALLIDO disponible';
    ELSE
        -- Esquema generado por Hibernate: VARCHAR + restricción CHECK con los valores permitidos.
        FOR nombre_restriccion IN
            SELECT conname
            FROM pg_constraint
            WHERE conrelid = 'public.fotografias_antiguedad'::regclass
              AND contype = 'c'
              AND pg_get_constraintdef(oid) LIKE '%tipo_fotografia%'
        LOOP
            EXECUTE format('ALTER TABLE public.fotografias_antiguedad DROP CONSTRAINT %I', nombre_restriccion);
        END LOOP;

        ALTER TABLE public.fotografias_antiguedad
            ADD CONSTRAINT fotografias_antiguedad_tipo_fotografia_check
            CHECK (tipo_fotografia IN (
                'ESTADO_INICIAL', 'AVANCE', 'ESTADO_FINAL',
                'TRASLADO_SALIDA', 'TRASLADO_LLEGADA', 'TRASLADO_INTENTO_FALLIDO'
            ));
        RAISE NOTICE 'Restricción CHECK de tipo_fotografia actualizada con TRASLADO_INTENTO_FALLIDO';
    END IF;
END
$migracion$;

COMMIT;
