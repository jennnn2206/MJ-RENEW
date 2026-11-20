package com.mjrenew.mjrenew_backend.transaccion.dto;

import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoTransaccion;
import java.math.BigDecimal;
import java.util.UUID;

public record DesgloseLiberacion(UUID antiguedadId, EstadoTransaccion estado, BigDecimal bruto,
        BigDecimal comision, BigDecimal cargoStripe, BigDecimal neto) {}
