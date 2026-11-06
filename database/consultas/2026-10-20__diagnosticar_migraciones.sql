-- MJ Renew | Diagnostico de migraciones existentes - SOLO LECTURA
-- Abrir Query Tool de pgAdmin4 en MJRenew-DB y ejecutar el archivo completo.
-- No crea tablas, no cambia datos, no modifica pagos ni estados.
-- Si falta una tabla base o tipo_fotografia no es enum nativo, DETENERSE y revisar.
SELECT current_database() AS base_actual, current_schema() AS esquema_actual,
       current_user AS usuario_conectado, version() AS version_postgresql;

WITH estado AS (
    SELECT
        to_regclass('public.catalogo_antiguedad') IS NOT NULL AS tabla_catalogo,
        to_regclass('public.fotografias_antiguedad') IS NOT NULL AS tabla_fotos,
        to_regclass('public.traslados_antiguedad') IS NOT NULL AS tabla_traslados,
        (SELECT data_type='character varying' AND character_maximum_length>=255 OR data_type='text'
         FROM information_schema.columns WHERE table_schema='public' AND table_name='catalogo_antiguedad'
                                           AND column_name='id_link_stripe_catalogo') AS stripe_link,
        (SELECT data_type='character varying' AND character_maximum_length>=255 OR data_type='text'
         FROM information_schema.columns WHERE table_schema='public' AND table_name='catalogo_antiguedad'
                                           AND column_name='id_pago_stripe_catalogo') AS stripe_pago,
        (SELECT data_type='text' FROM information_schema.columns
         WHERE table_schema='public' AND table_name='catalogo_antiguedad'
           AND column_name='url_link_pago_catalogo') AS stripe_url,
        (SELECT data_type='USER-DEFINED' AND udt_schema='public' AND udt_name='tipo_fotografia'
         FROM information_schema.columns WHERE table_schema='public' AND table_name='fotografias_antiguedad'
                                           AND column_name='tipo_fotografia') AS fotos_enum_nativo,
        EXISTS (
            SELECT 1 FROM pg_enum e JOIN pg_type t ON t.oid=e.enumtypid
                                    JOIN pg_namespace n ON n.oid=t.typnamespace
            WHERE n.nspname='public' AND t.typname='tipo_fotografia'
              AND e.enumlabel='TRASLADO_INTENTO_FALLIDO'
        ) AS fotos_intento_fallido,
        EXISTS (
            SELECT 1 FROM pg_enum e JOIN pg_type t ON t.oid=e.enumtypid
                                    JOIN pg_namespace n ON n.oid=t.typnamespace
            WHERE n.nspname='public' AND t.typname='tipo_traslado'
              AND e.enumlabel='ENTREGA_COMPRADOR'
        ) AS traslado_comprador,
        (SELECT data_type='timestamp with time zone' FROM information_schema.columns
         WHERE table_schema='public' AND table_name='catalogo_antiguedad'
           AND column_name='recepcion_confirmada_en_catalogo') AS recepcion,
        (SELECT data_type='uuid' FROM information_schema.columns
         WHERE table_schema='public' AND table_name='fotografias_antiguedad'
           AND column_name='traslado_id') AS foto_traslado,
        EXISTS (
            SELECT 1 FROM pg_constraint c
                              JOIN pg_attribute a ON a.attrelid=c.conrelid AND a.attname='traslado_id'
                              JOIN pg_attribute r ON r.attrelid=c.confrelid AND r.attname='traslados_antiguedad_id'
            WHERE c.contype='f'
              AND c.conrelid=to_regclass('public.fotografias_antiguedad')
              AND c.confrelid=to_regclass('public.traslados_antiguedad')
              AND c.conkey=ARRAY[a.attnum]::smallint[]
                AND c.confkey=ARRAY[r.attnum]::smallint[]
        ) AS fk_traslado,
        EXISTS (
            SELECT 1 FROM pg_indexes WHERE schemaname='public'
                                       AND tablename='fotografias_antiguedad'
                                       AND indexname='idx_fotografias_traslado_mjrenew'
                                       AND indexdef LIKE '%(traslado_id)%'
        ) AS indice_traslado
)
SELECT item, CASE WHEN hecho THEN 'OK' ELSE 'PENDIENTE / REVISAR' END AS resultado,
       etapa
FROM estado, LATERAL (VALUES
                          ('Tabla catalogo_antiguedad', tabla_catalogo, 'ESQUEMA BASE'),
                          ('Tabla fotografias_antiguedad', tabla_fotos, 'ESQUEMA BASE'),
                          ('Tabla traslados_antiguedad', tabla_traslados, 'ESQUEMA BASE'),
                          ('Stripe: id_link_stripe_catalogo >=255 o TEXT', stripe_link, '2026-10-09'),
                          ('Stripe: id_pago_stripe_catalogo >=255 o TEXT', stripe_pago, '2026-10-09'),
                          ('Stripe: url_link_pago_catalogo TEXT', stripe_url, '2026-10-09'),
                          ('Fotografias: enum nativo tipo_fotografia', fotos_enum_nativo, 'COMPATIBILIDAD JAVA'),
                          ('Fotografias: TRASLADO_INTENTO_FALLIDO', fotos_intento_fallido, '2026-10-20 foto fallida'),
                          ('Traslados: enum ENTREGA_COMPRADOR', traslado_comprador, 'COMPATIBILIDAD JAVA'),
                          ('Catalogo: recepcion_confirmada_en_catalogo TIMESTAMPTZ', recepcion, '2026-10-20 entrega'),
                          ('Fotos: traslado_id UUID', foto_traslado, '2026-10-20 entrega'),
                          ('Fotos: clave foranea traslado_id', fk_traslado, '2026-10-20 entrega'),
                          ('Fotos: indice por traslado_id', indice_traslado, '2026-10-20 entrega')
    ) AS v(item, hecho, etapa)
ORDER BY CASE etapa WHEN 'ESQUEMA BASE' THEN 0 WHEN 'COMPATIBILIDAD JAVA' THEN 1 WHEN '2026-10-09' THEN 2 WHEN '2026-10-20 foto fallida' THEN 3 ELSE 4 END, item;
