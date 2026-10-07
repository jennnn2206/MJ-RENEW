package com.mjrenew.mjrenew_backend.catalogo.dto;

import com.mjrenew.mjrenew_backend.nucleo.enums.TipoMueble;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogoAntiguedadResumenResponse(
        UUID catalogoId,
        UUID antiguedadId,
        TipoMueble tipoMueble,
        BigDecimal precioMxn,
        String urlFotoPortada
) {
}