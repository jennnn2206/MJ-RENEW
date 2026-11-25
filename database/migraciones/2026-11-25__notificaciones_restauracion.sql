-- RF-027, primer bloque: bandeja y cola persistente de correo para cuatro eventos de restauración.
CREATE TABLE IF NOT EXISTS public.notificaciones (
    notificaciones_id UUID PRIMARY KEY,
    destinatario_id UUID NOT NULL REFERENCES public.usuarios(usuarios_id) ON DELETE CASCADE,
    antiguedad_id UUID NOT NULL REFERENCES public.antiguedades(antiguedades_id) ON DELETE CASCADE,
    referencia_id UUID NOT NULL,
    tipo VARCHAR(40) NOT NULL CHECK (tipo IN ('ANTIGUEDAD_ASIGNADA','PRESUPUESTO_PRESENTADO','AVANCE_PUBLICADO','RESTAURACION_TERMINADA')),
    titulo VARCHAR(160) NOT NULL,
    mensaje VARCHAR(500) NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL,
    leido_en TIMESTAMPTZ,
    estado_correo VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado_correo IN ('PENDIENTE','ENVIANDO','ENVIADO')),
    intentos_correo INTEGER NOT NULL DEFAULT 0 CHECK (intentos_correo >= 0),
    proximo_intento_en TIMESTAMPTZ NOT NULL,
    reserva_id UUID,
    reservado_hasta TIMESTAMPTZ,
    correo_enviado_en TIMESTAMPTZ,
    CONSTRAINT uq_notificacion_evento UNIQUE (tipo, referencia_id, destinatario_id),
    CONSTRAINT chk_notificacion_lectura CHECK (leido_en IS NULL OR leido_en >= creado_en),
    CONSTRAINT chk_notificacion_reserva CHECK (
        (estado_correo = 'ENVIANDO' AND reserva_id IS NOT NULL AND reservado_hasta IS NOT NULL)
        OR (estado_correo <> 'ENVIANDO' AND reserva_id IS NULL AND reservado_hasta IS NULL)),
    CONSTRAINT chk_notificacion_envio CHECK ((estado_correo = 'ENVIADO') = (correo_enviado_en IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS idx_notificaciones_bandeja ON public.notificaciones (destinatario_id, creado_en DESC, notificaciones_id DESC);
CREATE INDEX IF NOT EXISTS idx_notificaciones_no_leidas ON public.notificaciones (destinatario_id) WHERE leido_en IS NULL;
CREATE INDEX IF NOT EXISTS idx_notificaciones_correo ON public.notificaciones (proximo_intento_en, creado_en) WHERE estado_correo <> 'ENVIADO';
