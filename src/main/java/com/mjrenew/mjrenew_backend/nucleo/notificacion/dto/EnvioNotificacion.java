package com.mjrenew.mjrenew_backend.nucleo.notificacion.dto;

import java.util.UUID;

public record EnvioNotificacion(UUID notificacionId, UUID reservaId, String correo, String titulo,
                                String mensaje, String rutaPanel) {
    @Override public String toString() { return "EnvioNotificacion[datos reservados]"; }
}
