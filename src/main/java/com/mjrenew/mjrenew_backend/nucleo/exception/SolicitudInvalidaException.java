package com.mjrenew.mjrenew_backend.nucleo.exception;

/**
 * Para violaciones de reglas de negocio sobre los datos de una solicitud que
 * no son un simple error de formato de campo (esas ya las cubre @Valid), por
 * ejemplo "faltan fotos de evidencia" o "falta el motivo del fallo".
 */
public class SolicitudInvalidaException extends RuntimeException {
    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}

// Especificación de excepciones.
//