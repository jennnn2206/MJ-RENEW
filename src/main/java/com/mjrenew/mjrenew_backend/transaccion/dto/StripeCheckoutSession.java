package com.mjrenew.mjrenew_backend.transaccion.dto;

public record StripeCheckoutSession(
        String referenciaStripe,
        String urlPagoStripe
) {
}