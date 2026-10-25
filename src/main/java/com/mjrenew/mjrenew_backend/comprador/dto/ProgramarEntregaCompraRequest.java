package com.mjrenew.mjrenew_backend.comprador.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/** Ventana que el comprador solicita para recibir una compra ya pagada. */
public record ProgramarEntregaCompraRequest(
        @NotNull(message = "La fecha de entrega es obligatoria")
        LocalDate fechaAcordadaTraslado,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime horaInicioAcordadaTraslado,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime horaFinAcordadaTraslado
) {
    @AssertTrue(message = "La hora de fin debe ser posterior a la hora de inicio")
    public boolean isRangoHorarioValido() {
        return horaInicioAcordadaTraslado == null || horaFinAcordadaTraslado == null
                || horaInicioAcordadaTraslado.isBefore(horaFinAcordadaTraslado);
    }
}
