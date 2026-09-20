package com.mjrenew.mjrenew_backend.transportista.dto;

import com.mjrenew.mjrenew_backend.nucleo.enums.ResultadoTraslado;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTraslado;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record TrasladoAntiguedadResumenResponse(
        UUID trasladoId,
        UUID antiguedadId,
        TipoTraslado tipoTraslado,
        LocalDate fechaAcordada,
        LocalTime horaInicio,
        LocalTime horaFin,
        ResultadoTraslado resultadoTraslado
) {
}