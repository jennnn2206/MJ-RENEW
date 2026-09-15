package com.mjrenew.mjrenew_backend.dto.antiguedad;

import com.mjrenew.mjrenew_backend.enums.EstadoAntiguedad;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoAntiguedadRequest(

        @NotNull(message = "El nuevo estado es obligatorio")
        EstadoAntiguedad nuevoEstado
) {
}
