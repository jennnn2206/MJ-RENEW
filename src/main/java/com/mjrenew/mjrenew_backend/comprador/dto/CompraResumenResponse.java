package com.mjrenew.mjrenew_backend.comprador.dto;

import com.mjrenew.mjrenew_backend.nucleo.enums.ResultadoTraslado;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoMueble;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Resumen para "Mis compras". No expone direcciones ni información financiera privada. */
public record CompraResumenResponse(
        UUID catalogoId,
        TipoMueble tipoMueble,
        BigDecimal precioMxn,
        String urlFotoPortada,
        OffsetDateTime pagadaEn,
        ResultadoTraslado resultadoEntrega,
        LocalDate fechaEntregaAcordada,
        OffsetDateTime salidaConfirmadaEn,
        OffsetDateTime entregaRegistradaEn,
        OffsetDateTime recepcionConfirmadaEn,
        boolean puedeConfirmarRecepcion
) {}
