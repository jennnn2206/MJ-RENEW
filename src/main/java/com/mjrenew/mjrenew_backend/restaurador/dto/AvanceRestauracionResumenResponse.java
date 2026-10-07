package com.mjrenew.mjrenew_backend.restaurador.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AvanceRestauracionResumenResponse(
        UUID avanceId,
        String descripcionAvance,
        OffsetDateTime publicadoEn,
        List<String> fotos
) {
}
