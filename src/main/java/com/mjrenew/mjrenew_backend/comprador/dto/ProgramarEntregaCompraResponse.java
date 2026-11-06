package com.mjrenew.mjrenew_backend.comprador.dto;

import com.mjrenew.mjrenew_backend.nucleo.enums.ResultadoTraslado;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Respuesta para el comprador; deliberadamente no expone la dirección del propietario. */
public record ProgramarEntregaCompraResponse(
        UUID catalogoId,
        UUID trasladoId,
        LocalDate fechaEntregaAcordada,
        LocalTime horaInicio,
        LocalTime horaFin,
        ResultadoTraslado resultadoEntrega,
        String mensaje
) {}
