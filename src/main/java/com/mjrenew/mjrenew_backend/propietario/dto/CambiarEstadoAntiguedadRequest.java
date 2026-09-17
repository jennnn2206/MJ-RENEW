package com.mjrenew.mjrenew_backend.propietario.dto;

import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoAntiguedadRequest(

        @NotNull(message = "El nuevo estado es obligatorio")
        EstadoAntiguedad nuevoEstado
) {
}
