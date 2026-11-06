package com.mjrenew.mjrenew_backend.administrador.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Transportista elegido por el administrador para una solicitud pendiente. */
public record AsignarTransportistaEntregaRequest(
        @NotNull(message = "Debes seleccionar un transportista")
        UUID transportistaId
) {}
