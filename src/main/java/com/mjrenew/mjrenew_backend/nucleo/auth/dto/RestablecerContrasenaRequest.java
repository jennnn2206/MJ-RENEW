package com.mjrenew.mjrenew_backend.nucleo.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RestablecerContrasenaRequest(
        @NotBlank(message = "El enlace de recuperación es obligatorio")
        @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "El enlace de recuperación no es válido")
        String token,
        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String nuevaContrasena
) {
    @Override public String toString() { return "RestablecerContrasenaRequest[datos reservados]"; }
}
