package com.mjrenew.mjrenew_backend.catalogo.service;

/** La reserva queda cerrada hasta recuperar o revisar el resultado de Stripe. */
public class CheckoutPendienteException extends RuntimeException {
    public CheckoutPendienteException(String message, Throwable cause) {
        super(message, cause);
    }
}
