package com.mjrenew.mjrenew_backend.nucleo.notificacion.dto;

import com.mjrenew.mjrenew_backend.nucleo.notificacion.TipoNotificacion;
import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificacionResponse(UUID id, TipoNotificacion tipo, UUID antiguedadId, String titulo,
                                   String mensaje, OffsetDateTime creadoEn, OffsetDateTime leidoEn) {}
