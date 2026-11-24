package com.mjrenew.mjrenew_backend.propietario.dto;

import com.mjrenew.mjrenew_backend.nucleo.enums.ResultadoTraslado;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Horario y seguimiento visibles al propietario, sin datos privados del taller. */
public record EntregaPropietarioResponse(UUID trasladoId, LocalDate fecha, LocalTime horaInicio,
        LocalTime horaFin, ResultadoTraslado resultado, boolean transportistaAsignado) {}
