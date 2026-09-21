package com.mjrenew.mjrenew_backend.catalogo.dto;

import com.mjrenew.mjrenew_backend.nucleo.enums.EstiloMueble;
import com.mjrenew.mjrenew_backend.nucleo.enums.MaterialMueble;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoMueble;
import com.mjrenew.mjrenew_backend.propietario.dto.DimensionResponse;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogoAntiguedadDetalleResponse(
        UUID catalogoId,
        UUID antiguedadId,
        TipoMueble tipoMueble,
        EstiloMueble estiloMueble,
        MaterialMueble materialMueble,
        String descripcionDaniosAntiguedad,
        String procedenciaMueble,
        BigDecimal precioMxn,
        DimensionResponse dimension,
        String urlFotoPortada
) {
}