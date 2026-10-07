package com.mjrenew.mjrenew_backend.restaurador.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RechazarEvaluacionRequest(

        @NotBlank(message = "El motivo de rechazo es obligatorio")
        @Size(
                max = 1000,
                message = "El motivo de rechazo no puede superar los 1000 caracteres"
        )
        String motivoRechazoEvaluacion

) {
}