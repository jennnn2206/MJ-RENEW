package com.mjrenew.mjrenew_backend.transportista.dto;

import com.mjrenew.mjrenew_backend.nucleo.enums.ResultadoTraslado;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTraslado;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TrasladoAntiguedadDetalleResponse(
        UUID trasladoId,
        UUID antiguedadId,
        TipoTraslado tipoTraslado,
        String direccionOrigen,
        String direccionDestino,
        LocalDate fechaAcordada,
        LocalTime horaInicio,
        LocalTime horaFin,
        short numeroIntento,
        ResultadoTraslado resultadoTraslado,
        BigDecimal costoEstimado,
        OffsetDateTime salidaConfirmadaEn,
        OffsetDateTime ejecutadoEn
) {
}