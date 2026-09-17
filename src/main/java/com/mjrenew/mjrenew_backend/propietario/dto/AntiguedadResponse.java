package com.mjrenew.mjrenew_backend.propietario.dto;

import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstiloMueble;
import com.mjrenew.mjrenew_backend.nucleo.enums.MaterialMueble;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoMueble;

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
