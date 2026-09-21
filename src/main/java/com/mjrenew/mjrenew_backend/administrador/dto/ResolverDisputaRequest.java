package com.mjrenew.mjrenew_backend.administrador.dto;

import jakarta.validation.constraints.NotBlank;

public record ResolverDisputaRequest(
        @NotBlank(message = "La resolución de la disputa es obligatoria")
        String resolucionDisputa
) {
}