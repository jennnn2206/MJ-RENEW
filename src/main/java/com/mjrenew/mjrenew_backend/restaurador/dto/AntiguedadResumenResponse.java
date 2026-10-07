//pendiente

package com.mjrenew.mjrenew_backend.restaurador.dto;

import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoMueble;

import java.util.UUID;

public record AntiguedadResumenResponse(
        UUID antiguedadId,
        TipoMueble tipoMueble,
        EstadoAntiguedad estadoActualAntiguedad,
        String urlFotoPortada
) {
}