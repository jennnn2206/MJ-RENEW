package com.mjrenew.mjrenew_backend.comprador.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Acuse logístico; no representa una transferencia financiera. */
public record ConfirmarRecepcionCompraResponse(
        UUID catalogoId,
        UUID trasladoId,
        OffsetDateTime recepcionConfirmadaEn
) {}
