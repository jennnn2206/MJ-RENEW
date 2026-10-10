package com.mjrenew.mjrenew_backend.transaccion.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.stripe.model.checkout.Session;

import java.util.Map;

/** Datos mínimos del Checkout; nunca incluye tarjetas ni datos personales. */
public record StripeCheckoutEstado(
        String sesionId,
        String tipoOperacion,
        String transaccionId,
        String estadoSesion,
        String estadoPago,
        String modalidad,
        String moneda,
        Long montoCentavos,
        Boolean enProduccion
) {
    public static StripeCheckoutEstado desdeWebhook(JsonNode nodo) {
        if (!nodo.isObject() || !"checkout.session".equals(texto(nodo, "object"))) {
            throw new SolicitudInvalidaException("El evento no contiene una sesión Checkout válida");
        }
        JsonNode metadata = nodo.path("metadata");
        JsonNode cantidad = nodo.path("amount_total");
        JsonNode livemode = nodo.path("livemode");
        return new StripeCheckoutEstado(
                texto(nodo, "id"), texto(metadata, "tipoTransaccion"),
                texto(metadata, "transaccionId"), texto(nodo, "status"),
                texto(nodo, "payment_status"), texto(nodo, "mode"),
                texto(nodo, "currency"),
                cantidad.isIntegralNumber() && cantidad.canConvertToLong() ? cantidad.longValue() : null,
                livemode.isBoolean() ? livemode.booleanValue() : null
        );
    }

    public static StripeCheckoutEstado desdeStripe(Session session) {
        Map<String, String> metadata = session.getMetadata();
        return new StripeCheckoutEstado(
                session.getId(), metadata == null ? null : metadata.get("tipoTransaccion"),
                metadata == null ? null : metadata.get("transaccionId"),
                session.getStatus(), session.getPaymentStatus(), session.getMode(),
                session.getCurrency(), session.getAmountTotal(), session.getLivemode()
        );
    }

    private static String texto(JsonNode nodo, String propiedad) {
        JsonNode valor = nodo.path(propiedad);
        return valor.isTextual() ? valor.textValue() : null;
    }
}
