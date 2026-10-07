package com.mjrenew.mjrenew_backend.restaurador.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record GenerarCotizacionRequest(

        @NotBlank(message = "La descripción del proceso es obligatoria")
        @Size(
                max = 2000,
                message = "La descripción del proceso no puede superar los 2000 caracteres"
        )
        String descripcionProcesoCotizacion,

        @NotNull(message = "El costo mínimo es obligatorio")
        @DecimalMin(
                value = "0.01",
                message = "El costo mínimo debe ser mayor que cero"
        )
        @Digits(
                integer = 8,
                fraction = 2,
                message = "El costo mínimo debe tener máximo 8 enteros y 2 decimales"
        )
        BigDecimal costoMinimoCotizacion,

        @NotNull(message = "El costo máximo es obligatorio")
        @DecimalMin(
                value = "0.01",
                message = "El costo máximo debe ser mayor que cero"
        )
        @Digits(
                integer = 8,
                fraction = 2,
                message = "El costo máximo debe tener máximo 8 enteros y 2 decimales"
        )
        BigDecimal costoMaximoCotizacion,

        @NotNull(message = "El tiempo estimado es obligatorio")
        @Min(
                value = 1,
                message = "El tiempo estimado debe ser de al menos una semana"
        )
        Integer tipoSemanasCotizacion

) {

    @AssertTrue(
            message = "El costo mínimo no puede ser mayor que el costo máximo"
    )
    public boolean isRangoCostosValido() {

        return costoMinimoCotizacion == null
                || costoMaximoCotizacion == null
                || costoMinimoCotizacion.compareTo(costoMaximoCotizacion) <= 0;
    }
}