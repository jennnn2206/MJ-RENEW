package com.mjrenew.mjrenew_backend.transaccion.service;

/** Stripe debe reintentar el evento cuando un Checkout se notifica antes del commit JPA. */
public class StripeTransaccionAunNoVisibleException extends RuntimeException {
    public StripeTransaccionAunNoVisibleException(String message) {
        super(message);
    }
}
