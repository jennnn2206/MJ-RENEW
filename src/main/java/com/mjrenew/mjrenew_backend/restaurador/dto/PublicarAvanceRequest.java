package com.mjrenew.mjrenew_backend.restaurador.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * La cantidad mínima de fotos no se valida aquí con @Size porque el mensaje
 * de error necesita quedar consistente con el resto de las reglas de
 * evidencia fotográfica (ver TrasladoAntiguedadService.guardarFotos). Se
 * valida en RestauradorService.
 */
public record PublicarAvanceRequest(

        @NotBlank(message = "La descripción del avance es obligatoria")
        @Size(
                max = 300,
                message = "La descripción del avance no puede superar los 300 caracteres"
        )
        String descripcionAvance,

        List<String> fotos

) {
}
