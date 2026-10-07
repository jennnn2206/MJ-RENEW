package com.mjrenew.mjrenew_backend.propietario.dto;

import java.math.BigDecimal;

public record DimensionResponse(
        BigDecimal altoCm,
        BigDecimal anchoCm,
        BigDecimal profundidadCm,
        BigDecimal pesoKg
) {
}