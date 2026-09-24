package com.mjrenew.mjrenew_backend.restaurador.dto;

import java.util.UUID;

public record PerfilRestauradorPublicoResponse(
        UUID restauradorId,
        String especialidadRestaurador,
        Integer anosExperienciaRestaurador,
        String descripcionBioRestaurador,
        String disponibilidadRestaurador
) {
}