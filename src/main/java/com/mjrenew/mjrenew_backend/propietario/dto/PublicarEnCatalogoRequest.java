package com.mjrenew.mjrenew_backend.propietario.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PublicarEnCatalogoRequest(

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(
                value = "0.01",
                message = "El precio debe ser mayor que cero"
        )
        @Digits(
                integer = 8,
                fraction = 2,
                message = "El precio debe tener máximo 8 enteros y 2 decimales"
        )
        BigDecimal precioMxnCatalogo,

        @Size(max = 300, message = "La dirección de recogida no puede superar los 300 caracteres")
        String direccionRecogidaTaller

) {
}
