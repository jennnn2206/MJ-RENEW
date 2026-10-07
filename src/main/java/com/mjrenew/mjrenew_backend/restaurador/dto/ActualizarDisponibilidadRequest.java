package com.mjrenew.mjrenew_backend.restaurador.dto;

import com.mjrenew.mjrenew_backend.nucleo.enums.DisponibilidadRestaurador;
import jakarta.validation.constraints.NotNull;

public record ActualizarDisponibilidadRequest(

        @NotNull(message = "La disponibilidad es obligatoria")
        DisponibilidadRestaurador disponibilidadRestaurador

) {
}
