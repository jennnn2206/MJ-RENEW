package com.mjrenew.mjrenew_backend.restaurador.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Versión completa del perfil, para el propio restaurador (obtenerPerfilRestaurador)
 * y para el administrador al revisar/aprobar solicitudes. Incluye el correo, que
 * PerfilRestauradorPublicoResponse omite por ser un dato privado.
 */
public record PerfilRestauradorCompletoResponse(
        UUID perfilId,
        String nombreCompleto,
        String correoElectronico,
        String especialidadRestaurador,
        Integer anosExperienciaRestaurador,
        String descripcionBioRestaurador,
        String disponibilidadRestaurador,
        Boolean aprobadoPorAdminRestaurador,
        OffsetDateTime actualizadoEn
) {
}
