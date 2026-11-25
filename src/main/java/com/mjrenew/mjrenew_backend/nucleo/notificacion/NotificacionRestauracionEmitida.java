package com.mjrenew.mjrenew_backend.nucleo.notificacion;

import java.util.UUID;

public record NotificacionRestauracionEmitida(TipoNotificacion tipo, UUID antiguedadId,
                                             UUID referenciaId, UUID destinatarioId) {}
