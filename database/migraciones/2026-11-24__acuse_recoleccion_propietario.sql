-- RF-015. Los registros anteriores conservan sus campos de acuse nulos.
ALTER TABLE public.traslados_antiguedad
    ADD COLUMN IF NOT EXISTS recoleccion_confirmada_en TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS recoleccion_confirmada_por UUID;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_recoleccion_confirmada_por'
                   AND conrelid = 'public.traslados_antiguedad'::regclass) THEN
        ALTER TABLE public.traslados_antiguedad ADD CONSTRAINT fk_recoleccion_confirmada_por
            FOREIGN KEY (recoleccion_confirmada_por) REFERENCES public.usuarios(usuarios_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_acuse_recoleccion'
                   AND conrelid = 'public.traslados_antiguedad'::regclass) THEN
        ALTER TABLE public.traslados_antiguedad ADD CONSTRAINT chk_acuse_recoleccion CHECK (
            (recoleccion_confirmada_en IS NULL AND recoleccion_confirmada_por IS NULL)
            OR (recoleccion_confirmada_en IS NOT NULL AND recoleccion_confirmada_por IS NOT NULL
                AND tipo_traslado = 'RECOLECCION' AND salida_confirmada_en_traslado IS NOT NULL
                AND recoleccion_confirmada_en >= salida_confirmada_en_traslado));
    END IF;
END $$;
