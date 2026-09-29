package com.mjrenew.mjrenew_backend.restaurador.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CotizacionResumenResponse(

        UUID cotizacionId,
        String descripcionProcesoCotizacion,
        BigDecimal costoMinimoCotizacion,
        BigDecimal costoMaximoCotizacion,
        Integer tipoSemanasCotizacion

) {
}