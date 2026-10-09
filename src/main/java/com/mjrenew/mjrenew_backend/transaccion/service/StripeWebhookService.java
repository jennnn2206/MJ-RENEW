package com.mjrenew.mjrenew_backend.transaccion.service;

import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.exception.TransicionEstadoInvalidaException;
import com.mjrenew.mjrenew_backend.transaccion.entity.TransaccionBancaria;
import com.mjrenew.mjrenew_backend.transaccion.repository.TransaccionBancariaRepository;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.Objects;

@Service
public class StripeWebhookService {

    private final TransaccionBancariaRepository transaccionRepository;
    private final String stripeWebhookSecret;


    public StripeWebhookService(
            TransaccionBancariaRepository transaccionRepository,

            @Value("${stripe.webhook-secret:}")
            String stripeWebhookSecret
    ) {
        this.transaccionRepository = transaccionRepository;
        this.stripeWebhookSecret = stripeWebhookSecret;
    }


    @Transactional
    public void procesarEvento(
            String payload,
            String stripeSignature
    ) {

        /*
         * Stripe siempre debe enviar el header
         * Stripe-Signature.
         */
        if (stripeSignature == null
                || stripeSignature.isBlank()) {

            throw new SolicitudInvalidaException(
                    "El evento no contiene la firma de Stripe"
            );
        }


        /*
         * Tampoco debemos procesar eventos reales
         * si el secreto del webhook no está configurado.
         */
        if (stripeWebhookSecret == null
                || stripeWebhookSecret.isBlank()) {

            throw new SolicitudInvalidaException(
                    "El secreto del webhook de Stripe no está configurado"
            );
        }


        Event event;

        try {

            /*
             * Esta llamada verifica criptográficamente
             * que el evento realmente procede de Stripe
             * y que el body no fue modificado.
             */
            event = Webhook.constructEvent(
                    payload,
                    stripeSignature,
                    stripeWebhookSecret
            );

        } catch (SignatureVerificationException ex) {

            throw new SolicitudInvalidaException(
                    "La firma del evento de Stripe no es válida"
            );
        }


        // MJRENEW-STRIPE: manejar pago confirmado y fallos/vencimientos.
        boolean pagoConfirmado = "checkout.session.completed".equals(event.getType())
                || "checkout.session.async_payment_succeeded".equals(event.getType());
        boolean pagoNoCompletado = "checkout.session.expired".equals(event.getType())
                || "checkout.session.async_payment_failed".equals(event.getType());
        if (!pagoConfirmado && !pagoNoCompletado) {
            return;
        }

        StripeObject stripeObject =
                event
                        .getDataObjectDeserializer()
                        .getObject()
                        .orElseThrow(() ->
                                new SolicitudInvalidaException(
                                        "No fue posible interpretar el evento de Stripe"
                                )
                        );


        if (!(stripeObject instanceof Session session)) {

            throw new SolicitudInvalidaException(
                    "El evento recibido no contiene una sesión de Checkout"
            );
        }


        String referenciaStripe =
                session.getId();


        if (referenciaStripe == null
                || referenciaStripe.isBlank()) {

            throw new SolicitudInvalidaException(
                    "Stripe no proporcionó una referencia de Checkout válida"
            );
        }


        TransaccionBancaria transaccion =
                transaccionRepository
                        .findByReferenciaStripeTransaccion(
                                referenciaStripe
                        )
                        .orElseThrow(() ->
                                new RecursoNoEncontradoException(
                                        "No existe una transacción asociada a la sesión de Stripe"
                                )
                        );


        // MJRENEW-STRIPE: correlacionar el evento con la operación interna.
        // No confiar solo en el session_id: verificar id interno y monto.
        if (session.getMetadata() == null
                || !Objects.equals(session.getMetadata().get("transaccionId"),
                transaccion.getTransaccionId().toString())) {
            throw new SolicitudInvalidaException("La referencia interna de Stripe no coincide");
        }

        if (pagoNoCompletado) {
            procesarPagoNoCompletado(transaccion);
            return;
        }

        // checkout.session.completed también puede significar pago ASÍNCRONO pendiente.
        // En ese caso esperar checkout.session.async_payment_succeeded.
        if (!"paid".equals(session.getPaymentStatus())) {
            return;
        }
        long esperadoCentavos = transaccion.getMontoBrutoMxnTransaccion()
                .movePointRight(2).longValueExact();
        if (!"mxn".equalsIgnoreCase(session.getCurrency())
                || !Objects.equals(session.getAmountTotal(), esperadoCentavos)) {
            throw new SolicitudInvalidaException("La moneda o el monto confirmado por Stripe no coincide");
        }

        /*
         * Stripe puede reenviar webhooks.
         *
         * Si ya procesamos este pago,
         * no volvemos a modificar nada.
         *
         * Esto hace el webhook idempotente.
         */
        if (transaccion.getEstadoTransaccion()
                == EstadoTransaccion.EXITOSA) {

            return;
        }


        if (transaccion.getEstadoTransaccion()
                != EstadoTransaccion.PENDIENTE) {

            throw new SolicitudInvalidaException(
                    "La transacción ya no se encuentra pendiente"
            );
        }


        /*
         * Según el tipo de operación de MJ Renew
         * ejecutamos la transición correspondiente.
         */
        if (transaccion.getTipoTransaccion()
                == TipoTransaccion.PAGO_VENTA) {

            procesarPagoVenta(
                    transaccion
            );

            return;
        }


        if (transaccion.getTipoTransaccion()
                == TipoTransaccion.PAGO_RESTAURACION) {

            procesarPagoRestauracion(
                    transaccion
            );

            return;
        }


        throw new SolicitudInvalidaException(
                "El tipo de transacción recibido no puede procesarse mediante Stripe"
        );
    }


    private void procesarPagoVenta(
            TransaccionBancaria transaccion
    ) {

        Antiguedad antiguedad =
                transaccion.getAntiguedad();


        if (antiguedad == null) {

            throw new SolicitudInvalidaException(
                    "La transacción no tiene una antigüedad asociada"
            );
        }


        /*
         * Transición oficial del Diseño API:
         *
         * LINK_PAGO_ENVIADO
         *         ↓
         * VENTA_COMPLETADA
         */
        if (antiguedad.getEstadoActualAntiguedad()
                != EstadoAntiguedad.LINK_PAGO_ENVIADO) {

            throw new TransicionEstadoInvalidaException(
                    "La antigüedad debe estar en LINK_PAGO_ENVIADO para confirmar la venta"
            );
        }


        CatalogoAntiguedad catalogo =
                transaccion.getCatalogoAntiguedad();


        if (catalogo == null) {

            throw new SolicitudInvalidaException(
                    "La transacción de venta no tiene una publicación de catálogo asociada"
            );
        }


        antiguedad.setEstadoActualAntiguedad(
                EstadoAntiguedad.VENTA_COMPLETADA
        );


        catalogo.setActivaCatalogo(
                false
        );


        catalogo.setCompletadaEnCatalogo(
                OffsetDateTime.now()
        );

        // MJRENEW-STRIPE: PaymentIntent es trazabilidad, no un cargo duplicado.
        // El session_id continúa en referenciaStripeTransaccion.
        // Se completa desde el evento, nunca desde parámetros del navegador.



        marcarTransaccionExitosa(
                transaccion
        );
    }


    private void procesarPagoRestauracion(
            TransaccionBancaria transaccion
    ) {

        Antiguedad antiguedad =
                transaccion.getAntiguedad();


        if (antiguedad == null) {

            throw new SolicitudInvalidaException(
                    "La transacción no tiene una antigüedad asociada"
            );
        }


        /*
         * Esta transición también está definida
         * en el Diseño API.
         *
         * PRESUPUESTO_PRESENTADO
         *          ↓
         * PAGO_EN_ESCROW
         *
         * Pero todavía NO estamos generando el
         * Checkout real de restauración.
         */
        if (antiguedad.getEstadoActualAntiguedad()
                != EstadoAntiguedad.PRESUPUESTO_PRESENTADO) {

            throw new TransicionEstadoInvalidaException(
                    "La antigüedad debe estar en PRESUPUESTO_PRESENTADO para confirmar el pago de restauración"
            );
        }


        antiguedad.setEstadoActualAntiguedad(
                EstadoAntiguedad.PAGO_EN_ESCROW
        );


        marcarTransaccionExitosa(
                transaccion
        );
    }


    // MJRENEW-STRIPE: liberar la reserva si Checkout expiró o falló.
    private void procesarPagoNoCompletado(TransaccionBancaria transaccion) {
        if (transaccion.getEstadoTransaccion() != EstadoTransaccion.PENDIENTE) {
            return;  // Idempotencia: ignorar reenvíos y eventos tardíos.
        }
        transaccion.setEstadoTransaccion(EstadoTransaccion.FALLIDA);
        transaccion.setProcesadaEnTransaccion(OffsetDateTime.now());

        if (transaccion.getTipoTransaccion() != TipoTransaccion.PAGO_VENTA) {
            // Para restauración no hay transición previa: permanece en PRESUPUESTO_PRESENTADO.
            return;
        }

        Antiguedad antiguedad = transaccion.getAntiguedad();
        CatalogoAntiguedad catalogo = transaccion.getCatalogoAntiguedad();
        if (antiguedad != null && catalogo != null
                && antiguedad.getEstadoActualAntiguedad() == EstadoAntiguedad.LINK_PAGO_ENVIADO) {
            antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.EN_CATALOGO);
            catalogo.setActivaCatalogo(true);
            catalogo.setComprador(null);
            catalogo.setIdLinkStripeCatalogo(null);
            catalogo.setUrlLinkPagoCatalogo(null);
        }
    }

    private void marcarTransaccionExitosa(
            TransaccionBancaria transaccion
    ) {

        transaccion.setEstadoTransaccion(
                EstadoTransaccion.EXITOSA
        );


        transaccion.setProcesadaEnTransaccion(
                OffsetDateTime.now()
        );
    }
}