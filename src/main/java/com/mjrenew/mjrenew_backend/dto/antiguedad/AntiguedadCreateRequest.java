package com.mjrenew.mjrenew_backend.dto.antiguedad;

import com.mjrenew.mjrenew_backend.enums.EstiloMueble;
import com.mjrenew.mjrenew_backend.enums.MaterialMueble;
import com.mjrenew.mjrenew_backend.enums.TipoMueble;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AntiguedadCreateRequest(

        @NotNull(message = "El tipo de mueble es obligatorio")
        TipoMueble tipoMueble,

        EstiloMueble estiloMueble,

        MaterialMueble materialMueble,

        String descripcionDaniosAntiguedad,

        @Size(max = 300, message = "La procedencia no puede superar los 300 caracteres")
        String procedenciaMueble
) {
}
