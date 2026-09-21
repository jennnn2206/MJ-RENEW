package com.mjrenew.mjrenew_backend.administrador.dto;

import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DisputaAntiguedadDetalleResponse(
        UUID disputaId,
        UUID antiguedadId,
        String descripcionDisputa,
        String nombreAbiertaPor,
        EstadoAntiguedad estadoCicloAlAbrirDisputa,
        String nombreAdminAsignado,
        String resolucionDisputa,
        OffsetDateTime abiertaEn,
        OffsetDateTime resueltaEn,
        List<String> fotos
) {
}