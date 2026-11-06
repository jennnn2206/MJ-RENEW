package com.mjrenew.mjrenew_backend.transportista.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * La foto de evidencia (opcional) llega como archivo en una parte multipart
 * separada ("foto", ver TransportistaController.registrarIntentoFallido),
 * no en este JSON; se valida en TrasladoAntiguedadService.
 */
public record RegistrarIntentoFallidoRequest(

        @NotBlank(message = "Describe por qué no se pudo completar el traslado")
        @Size(
                max = 255,
                message = "La nota no puede superar los 255 caracteres"
        )
        String motivoFallo

) {
}
