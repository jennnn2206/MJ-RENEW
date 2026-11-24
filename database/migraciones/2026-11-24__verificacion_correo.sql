-- RF-002. No verifica retroactivamente ninguna cuenta existente.
CREATE TABLE IF NOT EXISTS public.verificaciones_correo (
    usuario_id UUID PRIMARY KEY REFERENCES public.usuarios(usuarios_id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    emitido_en TIMESTAMPTZ NOT NULL,
    expira_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_vigencia_verificacion_correo CHECK (expira_en > emitido_en)
);
