package com.mjrenew.mjrenew_backend.propietario.dto;

import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTraslado;

import java.time.OffsetDateTime;

/**
 * Aviso al propietario de que el último traslado de su pieza no se pudo
 * completar (NADIE_EN_CASA). urlFotoEvidencia es null si el transportista
 * no subió foto.
 */
public record AvisoIntentoFallidoResponse(
        TipoTraslado tipoTraslado,
        short numeroIntento,
        String motivoFallo,
        OffsetDateTime registradoEn,
        String urlFotoEvidencia
) {
}
