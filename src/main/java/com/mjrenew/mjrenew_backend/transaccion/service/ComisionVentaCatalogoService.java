
package com.mjrenew.mjrenew_backend.transaccion.service;

import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.transaccion.dto.DesgloseComisionVenta;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcula la comisión de MJ Renew para las ventas del catálogo.
 */
@Service
public class ComisionVentaCatalogoService {

    private static final BigDecimal TASA_COMISION_MJRENEW =
            new BigDecimal("0.07");

    private static final int ESCALA_MONEDA = 2;

    /**
     * Calcula la comisión del 7% sobre el importe bruto de una venta.
     *
     * No descuenta el cargo de Stripe porque debe obtenerse
     * de la información real de la transacción.
     */
    public DesgloseComisionVenta calcular(BigDecimal montoBrutoMxn) {

        if (montoBrutoMxn == null || montoBrutoMxn.signum() <= 0) {
            throw new SolicitudInvalidaException(
                    "El monto bruto de la venta debe ser mayor que cero"
            );
        }

        BigDecimal montoBruto = montoBrutoMxn.setScale(
                ESCALA_MONEDA,
                RoundingMode.HALF_UP
        );

        if (montoBruto.signum() <= 0) {
            throw new SolicitudInvalidaException(
                    "El monto de la venta debe ser válido en pesos mexicanos"
            );
        }

        BigDecimal comision = montoBruto
                .multiply(TASA_COMISION_MJRENEW)
                .setScale(ESCALA_MONEDA, RoundingMode.HALF_UP);

        BigDecimal restante = montoBruto
                .subtract(comision)
                .setScale(ESCALA_MONEDA, RoundingMode.HALF_UP);

        return new DesgloseComisionVenta(
                montoBruto,
                comision,
                restante
        );
    }
}
