package com.mjrenew.mjrenew_backend.propietario.dto;

import com.mjrenew.mjrenew_backend.nucleo.enums.EstiloMueble;
import com.mjrenew.mjrenew_backend.nucleo.enums.MaterialMueble;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoMueble;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AntiguedadCreateRequest(

        @NotNull(message = "El tipo de mueble es obligatorio")
        TipoMueble tipoMueble,

        EstiloMueble estiloMueble,

        MaterialMueble materialMueble,

        @Size(max = 1000, message = "La descripción de daños no puede superar los 1000 caracteres")
        String descripcionDaniosAntiguedad,

        @Size(max = 300, message = "La procedencia no puede superar los 300 caracteres")
        String procedenciaMueble
) {
}
