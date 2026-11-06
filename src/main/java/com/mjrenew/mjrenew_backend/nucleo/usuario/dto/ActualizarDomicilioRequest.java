package com.mjrenew.mjrenew_backend.nucleo.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Domicilio textual que se utilizará para coordinar traslados. */
public record ActualizarDomicilioRequest(
        @NotBlank(message = "El domicilio es obligatorio")
        @Size(max = 300, message = "El domicilio no puede superar los 300 caracteres")
        String direccionTextoUsuario
) {}
