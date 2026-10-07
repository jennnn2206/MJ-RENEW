package com.mjrenew.mjrenew_backend.transportista.dto;

import java.util.List;

/**
 * fotos es obligatorio solo en confirmarEntregaPropietario (RF-018); en
 * confirmarLlegadaTaller no se exige, porque la evidencia de esa etapa
 * ya se tomó en la recolección. La regla la valida el service según
 * el tipo de traslado, no este DTO.
 */
public record ConfirmarLlegadaRequest(
        List<String> fotos
) {
}