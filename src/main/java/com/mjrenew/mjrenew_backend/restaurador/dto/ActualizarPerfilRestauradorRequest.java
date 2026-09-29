package com.mjrenew.mjrenew_backend.restaurador.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActualizarPerfilRestauradorRequest(

        @NotBlank(message = "La especialidad es obligatoria")
        @Size(
                max = 150,
                message = "La especialidad no puede superar los 150 caracteres"
        )
        String especialidadRestaurador,

        @NotNull(message = "Los años de experiencia son obligatorios")
        @Min(
                value = 0,
                message = "Los años de experiencia no pueden ser negativos"
        )
        Short anosExperienciaRestaurador,

        String descripcionBioRestaurador

) {
}
