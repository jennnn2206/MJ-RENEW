package com.mjrenew.mjrenew_backend.propietario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RechazarCotizacionRequest(

        @NotBlank(message = "El motivo de rechazo es obligatorio")
        @Size(
                max = 1000,
                message = "El motivo de rechazo no puede superar los 1000 caracteres"
        )
        String motivoRechazoCotizacion

) {
}
