
package com.mjrenew.mjrenew_backend.transaccion.dto;

import java.math.BigDecimal;

/**
 * Desglose de la comisión de MJ Renew sobre una venta del catálogo.
 *
 * El monto restante todavía no descuenta el cargo real de Stripe.
 */
public record DesgloseComisionVenta(
        BigDecimal montoBrutoMxn,
        BigDecimal comisionMjrenewMxn,
        BigDecimal montoRestanteAntesCargoStripeMxn
) {
}
