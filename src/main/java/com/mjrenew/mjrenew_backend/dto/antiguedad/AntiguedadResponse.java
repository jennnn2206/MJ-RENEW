package com.mjrenew.mjrenew_backend.dto.antiguedad;

import com.mjrenew.mjrenew_backend.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.enums.EstiloMueble;
import com.mjrenew.mjrenew_backend.enums.MaterialMueble;
import com.mjrenew.mjrenew_backend.enums.TipoMueble;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AntiguedadResponse(
        UUID id,
        UUID propietarioId,
        String nombrePropietario,
        UUID restauradorId,
        String nombreRestaurador,
        TipoMueble tipoMueble,
        EstiloMueble estiloMueble,
        MaterialMueble materialMueble,
        String descripcionDaniosAntiguedad,
        String procedenciaMueble,
        EstadoAntiguedad estadoActual,
        OffsetDateTime registradaEn
) {
}
