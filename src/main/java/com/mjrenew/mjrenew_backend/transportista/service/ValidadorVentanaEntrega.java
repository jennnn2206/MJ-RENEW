package com.mjrenew.mjrenew_backend.transportista.service;

import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class ValidadorVentanaEntrega {
    public void validar(LocalDate fecha, LocalTime inicio, LocalTime fin) {
        if (fecha == null || inicio == null || fin == null) {
            throw new SolicitudInvalidaException("La fecha y la ventana de entrega son obligatorias");
        }
        if (!inicio.isBefore(fin)) {
            throw new SolicitudInvalidaException("La hora de fin debe ser posterior a la hora de inicio");
        }
        var ahora = ZonedDateTime.now(ZoneId.of("America/Merida"));
        if (!fecha.atTime(inicio).isAfter(ahora.toLocalDateTime())) {
            throw new SolicitudInvalidaException("Selecciona una fecha y hora de entrega futuras");
        }
    }
}
