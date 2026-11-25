-- RF-004. Los tokens de recuperación se mantienen separados de la verificación de correo.
CREATE TABLE IF NOT EXISTS public.recuperaciones_contrasena (
    usuario_id UUID PRIMARY KEY REFERENCES public.usuarios(usuarios_id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    emitido_en TIMESTAMPTZ NOT NULL,
    expira_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_vigencia_recuperacion_contrasena CHECK (expira_en > emitido_en)
);
