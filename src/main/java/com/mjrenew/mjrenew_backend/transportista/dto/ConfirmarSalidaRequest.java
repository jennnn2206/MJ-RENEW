package com.mjrenew.mjrenew_backend.transportista.dto;

import java.util.List;

/**
 * fotos es obligatorio solo en confirmarSalidaRecoleccion (RF-015); en
 * confirmarSalidaEntregaPropietario no se exige, porque en ese punto el
 * ERS no pide evidencia todavía. La regla la valida el service según
 * el tipo de traslado, no este DTO.
 */
public record ConfirmarSalidaRequest(
        List<String> fotos
) {
}