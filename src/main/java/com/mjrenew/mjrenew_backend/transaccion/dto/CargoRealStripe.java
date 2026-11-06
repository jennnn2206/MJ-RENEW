package com.mjrenew.mjrenew_backend.transaccion.dto;

/** Datos del cargo y su comisión real, consultados en Stripe. Importes en centavos. */
public record CargoRealStripe(
        StripeCheckoutEstado sesion,
        String paymentIntentId,
        String chargeId,
        String balanceTransactionId,
        Long montoCentavos,
        Long cargoCentavos,
        String moneda,
        Boolean enProduccion
) {}
