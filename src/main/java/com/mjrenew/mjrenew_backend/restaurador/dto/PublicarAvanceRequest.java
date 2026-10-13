package com.mjrenew.mjrenew_backend.restaurador.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * La fotografía del avance llega como archivo en una parte multipart
 * separada ("foto", ver RestauradorController.publicarAvanceRestauracion),
 * no en este JSON; se valida en RestauradorService.
 */
public record PublicarAvanceRequest(

        @NotBlank(message = "La descripción del avance es obligatoria")
        @Size(
                max = 300,
                message = "La descripción del avance no puede superar los 300 caracteres"
        )
        String descripcionAvance

) {
}
