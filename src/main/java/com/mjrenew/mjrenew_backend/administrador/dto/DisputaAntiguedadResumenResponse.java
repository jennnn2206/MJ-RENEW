package com.mjrenew.mjrenew_backend.administrador.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DisputaAntiguedadResumenResponse(
        UUID disputaId,
        UUID antiguedadId,
        String descripcionDisputa,
        String nombreAbiertaPor,
        OffsetDateTime abiertaEn
) {
}