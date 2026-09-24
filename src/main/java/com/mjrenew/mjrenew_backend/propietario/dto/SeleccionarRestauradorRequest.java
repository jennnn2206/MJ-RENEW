package com.mjrenew.mjrenew_backend.propietario.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SeleccionarRestauradorRequest(

        @NotNull(message = "El restaurador es obligatorio")
        UUID restauradorId

) {
}