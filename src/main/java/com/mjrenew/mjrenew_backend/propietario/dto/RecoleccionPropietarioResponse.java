package com.mjrenew.mjrenew_backend.propietario.dto;

import com.mjrenew.mjrenew_backend.nucleo.enums.ResultadoTraslado;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Evidencias y acuse del intento de recolección, sin direcciones del taller. */
public record RecoleccionPropietarioResponse(UUID trasladoId, short numeroIntento,
        ResultadoTraslado resultado, OffsetDateTime salidaConfirmadaEn,
        OffsetDateTime entregaConfirmadaEn, UUID confirmadaPor, List<String> fotos) {}
