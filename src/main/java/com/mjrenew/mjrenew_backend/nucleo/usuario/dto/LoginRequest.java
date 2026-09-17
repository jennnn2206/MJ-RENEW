package com.mjrenew.mjrenew_backend.nucleo.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico no tiene un formato válido")
        String correoElectronico,

        @NotBlank(message = "La contraseña es obligatoria")
        String contrasena
) {
}
