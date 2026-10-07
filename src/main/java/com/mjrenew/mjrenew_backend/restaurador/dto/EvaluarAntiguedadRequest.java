package com.mjrenew.mjrenew_backend.restaurador.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record EvaluarAntiguedadRequest(

        @DecimalMin(
                value = "0.01",
                message = "El alto debe ser mayor que cero"
        )
        BigDecimal altoCm,

        @DecimalMin(
                value = "0.01",
                message = "El ancho debe ser mayor que cero"
        )
        BigDecimal anchoCm,

        @DecimalMin(
                value = "0.01",
                message = "La profundidad debe ser mayor que cero"
        )
        BigDecimal profundidadCm,

        @DecimalMin(
                value = "0.01",
                message = "El peso debe ser mayor que cero"
        )
        BigDecimal pesoKg

) {
}