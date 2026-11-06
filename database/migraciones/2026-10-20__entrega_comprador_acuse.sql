-- MJRenew | Entrega al comprador y acuse, SIN liberación financiera.
-- Ejecutar SOLO después de respaldar PostgreSQL. AFD existente intacto.
BEGIN;

ALTER TABLE public.catalogo_antiguedad
    ADD COLUMN IF NOT EXISTS recepcion_confirmada_en_catalogo TIMESTAMPTZ;

-- Relacionar evidencia con el traslado concreto; registros anteriores siguen NULL.
ALTER TABLE public.fotografias_antiguedad
    ADD COLUMN IF NOT EXISTS traslado_id UUID;

DO $verificacion$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_fotografia_traslado_mjrenew') THEN
ALTER TABLE public.fotografias_antiguedad
    ADD CONSTRAINT fk_fotografia_traslado_mjrenew
        FOREIGN KEY (traslado_id) REFERENCES public.traslados_antiguedad(traslados_antiguedad_id);
END IF;
END
$verificacion$;

CREATE INDEX IF NOT EXISTS idx_fotografias_traslado_mjrenew
    ON public.fotografias_antiguedad(traslado_id);

COMMIT;
