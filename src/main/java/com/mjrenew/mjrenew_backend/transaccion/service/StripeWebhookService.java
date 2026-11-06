package com.mjrenew.mjrenew_backend.transaccion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.transaccion.dto.StripeCheckoutEstado;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * La verificación criptográfica precede toda lectura o procesamiento del evento.
 * StripePagoEstadoService es el único componente que aplica cambios monetarios y del AFD.
 */
@Service
public class StripeWebhookService {
    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);
    private final StripePagoEstadoService pagos;
    private final ObjectMapper objectMapper;
    private final String secretoWebhook;

    public StripeWebhookService(StripePagoEstadoService pagos, ObjectMapper objectMapper,
                                @Value("${stripe.webhook-secret:}") String secretoWebhook) {
        this.pagos = pagos;
        this.objectMapper = objectMapper;
        this.secretoWebhook = secretoWebhook;
    }

    public void procesarEvento(String payload, String stripeSignature) {
        if (stripeSignature == null || stripeSignature.isBlank()) {
            throw new SolicitudInvalidaException("El evento no contiene Stripe-Signature");
        }
        if (secretoWebhook == null || secretoWebhook.isBlank()) {
            throw new IllegalStateException("El secreto de webhook no está configurado");
        }

        Event evento;
        try {
            evento = Webhook.constructEvent(payload, stripeSignature, secretoWebhook);
        } catch (SignatureVerificationException ex) {
            log.warn("Firma Stripe inválida: {}", ex.getMessage());
            throw new SolicitudInvalidaException("Firma del webhook de Stripe inválida");
        }

        boolean confirmado = "checkout.session.completed".equals(evento.getType())
                || "checkout.session.async_payment_succeeded".equals(evento.getType());
        boolean fallido = "checkout.session.expired".equals(evento.getType())
                || "checkout.session.async_payment_failed".equals(evento.getType());
        if (!confirmado && !fallido) {
            return; // Eventos no relacionados con MJ Renew no generan reintentos.
        }

        final StripeCheckoutEstado sesion;
        try {
            JsonNode cuerpo = objectMapper.readTree(payload);
            sesion = StripeCheckoutEstado.desdeWebhook(cuerpo.path("data").path("object"));
        } catch (JsonProcessingException ex) {
            throw new SolicitudInvalidaException("El JSON firmado de Stripe no es válido");
        }

        if (fallido) {
            if ("checkout.session.expired".equals(evento.getType())
                    && !"expired".equals(sesion.estadoSesion())) {
                throw new SolicitudInvalidaException("El evento expirado tiene un estado inconsistente");
            }
            pagos.marcarNoCompletado(sesion);
        } else {
            pagos.confirmar(sesion);
        }
    }
}
