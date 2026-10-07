package com.mjrenew.mjrenew_backend.transportista.dto;

import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTraslado;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Agenda un traslado (RECOLECCION o ENTREGA_PROPIETARIO) para una antigüedad,
 * asignando un transportista y una ventana horaria. ENTREGA_COMPRADOR queda
 * fuera de alcance por ahora: no existe todavía el flujo de confirmación de
 * pago del comprador que la precede.
 */
public record CrearTrasladoRequest(

        @NotNull(message = "La antigüedad es obligatoria")
        UUID antiguedadId,

        @NotNull(message = "El transportista es obligatorio")
        UUID transportistaId,

        @NotNull(message = "El tipo de traslado es obligatorio")
        TipoTraslado tipoTraslado,

        @NotBlank(message = "La dirección de origen es obligatoria")
        @Size(max = 300, message = "La dirección de origen no puede superar los 300 caracteres")
        String direccionOrigenTraslado,

        @NotBlank(message = "La dirección de destino es obligatoria")
        @Size(max = 300, message = "La dirección de destino no puede superar los 300 caracteres")
        String direccionDestinoTraslado,

        @NotNull(message = "La fecha acordada es obligatoria")
        @FutureOrPresent(message = "La fecha acordada no puede ser en el pasado")
        LocalDate fechaAcordadaTraslado,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime horaInicioAcordadaTraslado,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime horaFinAcordadaTraslado,

        @DecimalMin(value = "0.00", message = "El costo estimado no puede ser negativo")
        BigDecimal costoEstimadoMxnTraslado

) {

    @AssertTrue(message = "La hora de fin debe ser posterior a la hora de inicio")
    public boolean isRangoHorarioValido() {

        return horaInicioAcordadaTraslado == null
                || horaFinAcordadaTraslado == null
                || horaInicioAcordadaTraslado.isBefore(horaFinAcordadaTraslado);
    }
}
