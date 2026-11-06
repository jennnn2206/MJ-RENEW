-- MJ Renew | Verificacion estructural posterior (SOLO LECTURA)
-- Ejecutar en la misma base DESPUES de aplicar migraciones faltantes.
-- Si alguna consulta produce ERROR, falta una tabla/columna: no hacer commit todavia.
SELECT current_database() AS base_verificada;

SELECT table_name, column_name, data_type, udt_name,
       character_maximum_length, is_nullable
FROM information_schema.columns
WHERE table_schema='public' AND
    ((table_name='catalogo_antiguedad' AND column_name IN
                                           ('id_link_stripe_catalogo','id_pago_stripe_catalogo','url_link_pago_catalogo','recepcion_confirmada_en_catalogo'))
        OR (table_name='fotografias_antiguedad' AND column_name IN ('tipo_fotografia','traslado_id'))
        OR (table_name='traslados_antiguedad' AND column_name='traslados_antiguedad_id'))
ORDER BY table_name, column_name;

SELECT t.typname AS tipo_enum, e.enumlabel AS valor
FROM pg_type t
         JOIN pg_namespace n ON n.oid=t.typnamespace
         JOIN pg_enum e ON e.enumtypid=t.oid
WHERE n.nspname='public' AND t.typname IN ('tipo_fotografia','tipo_traslado')
ORDER BY t.typname, e.enumsortorder;

SELECT c.conname AS nombre_fk, pg_get_constraintdef(c.oid) AS definicion
FROM pg_constraint c
WHERE c.contype='f' AND c.conrelid=to_regclass('public.fotografias_antiguedad')
  AND pg_get_constraintdef(c.oid) LIKE '%traslado_id%';

SELECT indexname, indexdef FROM pg_indexes
WHERE schemaname='public' AND tablename='fotografias_antiguedad'
  AND indexname='idx_fotografias_traslado_mjrenew';

-- Datos reales existentes: solo CONTAR. Los registros previos pueden tener traslado_id NULL.
SELECT COUNT(*) AS fotos_existentes,
       COUNT(traslado_id) AS fotos_ya_asociadas_a_traslado
FROM public.fotografias_antiguedad;

SELECT COUNT(*) AS ventas_existentes,
       COUNT(recepcion_confirmada_en_catalogo) AS recepciones_confirmadas
FROM public.catalogo_antiguedad;
