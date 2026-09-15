package com.mjrenew.mjrenew_backend.exception;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String mensaje,
        List<String> detalles
) {
    public ErrorResponse(int status, String error, String mensaje) {
        this(OffsetDateTime.now(), status, error, mensaje, List.of());
    }

    public ErrorResponse(int status, String error, String mensaje, List<String> detalles) {
        this(OffsetDateTime.now(), status, error, mensaje, detalles);
    }
}
