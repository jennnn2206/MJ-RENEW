package com.mjrenew.mjrenew_backend.nucleo.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerificarCorreoRequest(
        @NotBlank(message = "El enlace de verificación es obligatorio")
        @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "El enlace de verificación no es válido")
        String token
) {}
