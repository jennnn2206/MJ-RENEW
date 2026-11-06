-- Persiste la relación entre el propietario de MJ Renew y su cuenta conectada.
-- Los datos bancarios y documentos de identidad permanecen en Stripe.

CREATE TABLE IF NOT EXISTS public.cuentas_stripe_conectadas (
                                                                cuenta_stripe_conectada_id UUID PRIMARY KEY,
                                                                usuario_id UUID NOT NULL,
                                                                stripe_account_id VARCHAR(255) NOT NULL,
    detalles_enviados BOOLEAN NOT NULL DEFAULT FALSE,
    transferencias_habilitadas BOOLEAN NOT NULL DEFAULT FALSE,
    payouts_habilitados BOOLEAN NOT NULL DEFAULT FALSE,
    creada_en TIMESTAMPTZ NOT NULL,
    actualizada_en TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_cuenta_stripe_usuario
    FOREIGN KEY (usuario_id)
    REFERENCES public.usuarios (usuarios_id),
    CONSTRAINT uk_cuenta_stripe_usuario
    UNIQUE (usuario_id),
    CONSTRAINT uk_cuenta_stripe_account_id
    UNIQUE (stripe_account_id)
    );
