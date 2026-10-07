package com.mjrenew.mjrenew_backend.propietario.dto;

import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstiloMueble;
import com.mjrenew.mjrenew_backend.nucleo.enums.MaterialMueble;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoMueble;

import java.util.UUID;

public record AntiguedadDetalleResponse(
        UUID antiguedadId,
        TipoMueble tipoMueble,
        EstiloMueble estiloMueble,
        MaterialMueble materialMueble,
        String descripcionDaniosAntiguedad,
        String procedenciaMueble,
        EstadoAntiguedad estadoActualAntiguedad,
        String nombreCompletoRestaurador,
        DimensionResponse dimension
) {
}