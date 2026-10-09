package com.mjrenew.mjrenew_backend.transaccion.service;

import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.transaccion.dto.StripeCheckoutSession;
import com.mjrenew.mjrenew_backend.transaccion.entity.TransaccionBancaria;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTransaccion;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class StripeCheckoutService {

    private final String stripeSecretKey;
    private final String appBaseUrl;


    public StripeCheckoutService(
            @Value("${stripe.secret-key}")
            String stripeSecretKey,

            @Value("${app.base-url:http://localhost:8080}")
            String appBaseUrl
    ) {

        this.stripeSecretKey = stripeSecretKey;
        this.appBaseUrl = appBaseUrl;
    }


    public StripeCheckoutSession crearSesionPago(
            TransaccionBancaria transaccion
    ) {

        validarTransaccion(transaccion);

        /*
         * Stripe trabaja con la unidad mínima de la moneda.
         *
         * Ejemplo:
         *
         * $12500.00 MXN
         *       ↓
         * 1,250,000 centavos
         */
        long montoCentavos =
                convertirPesosACentavos(
                        transaccion
                                .getMontoBrutoMxnTransaccion()
                );


        /*
         * La llave se obtiene de
         * application-local.properties.
         *
         * Nunca debe escribirse directamente
         * dentro del código.
         */
        Stripe.apiKey = stripeSecretKey;


        // MJRENEW-STRIPE: cada proceso regresa a su pantalla correspondiente.
        String rutaRetorno = transaccion.getTipoTransaccion() == TipoTransaccion.PAGO_RESTAURACION
                ? "/propietario" : "/catalogo";

        SessionCreateParams params =
                SessionCreateParams.builder()
                        // Checkout hospedado exige como mínimo ~30 minutos.
                        // Los 7 minutos del ERS requieren un mecanismo adicional.
                        .setExpiresAt(Instant.now().plus(35, ChronoUnit.MINUTES).getEpochSecond())

                        /*
                         * Esta sesión corresponde
                         * a un pago único.
                         */
                        .setMode(
                                SessionCreateParams.Mode.PAYMENT
                        )


                        /*
                         * Al terminar el pago Stripe
                         * redirigirá al usuario aquí.
                         *
                         * La confirmación real NO depende
                         * de esta URL.
                         *
                         * La confirmación ocurrirá mediante
                         * el webhook.
                         */
                        .setSuccessUrl(
                                appBaseUrl
                                        + rutaRetorno
                                        + "?pago=exitoso"
                                        + "&session_id={CHECKOUT_SESSION_ID}"
                        )


                        /*
                         * Si el usuario cancela el Checkout.
                         */
                        .setCancelUrl(
                                appBaseUrl
                                        + rutaRetorno
                                        + "?pago=cancelado"
                        )


                        /*
                         * Guardamos el ID interno de
                         * TransaccionBancaria dentro
                         * de los metadatos de Stripe.
                         *
                         * Nos sirve como trazabilidad.
                         */
                        .putMetadata(
                                "transaccionId",
                                transaccion
                                        .getTransaccionId()
                                        .toString()
                        )


                        /*
                         * También indicamos el tipo
                         * de transacción.
                         */
                        .putMetadata(
                                "tipoTransaccion",
                                transaccion
                                        .getTipoTransaccion()
                                        .name()
                        )


                        /*
                         * Producto que verá el usuario
                         * dentro de Stripe Checkout.
                         */
                        .addLineItem(
                                SessionCreateParams.LineItem
                                        .builder()

                                        .setQuantity(1L)

                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData
                                                        .builder()

                                                        .setCurrency(
                                                                "mxn"
                                                        )

                                                        .setUnitAmount(
                                                                montoCentavos
                                                        )

                                                        .setProductData(
                                                                SessionCreateParams
                                                                        .LineItem
                                                                        .PriceData
                                                                        .ProductData
                                                                        .builder()

                                                                        .setName(
                                                                                "Antigüedad MJ Renew"
                                                                        )

                                                                        .build()
                                                        )

                                                        .build()
                                        )

                                        .build()
                        )

                        .build();


        try {

            /*
             * Aquí ocurre la comunicación REAL
             * con Stripe.
             */
            Session session =
                    Session.create(params);


            /*
             * Stripe debe devolver:
             *
             * session.getId()
             * → cs_test_...
             *
             * session.getUrl()
             * → https://checkout.stripe.com/...
             */
            if (session.getId() == null
                    || session.getId().isBlank()) {

                throw new SolicitudInvalidaException(
                        "Stripe no devolvió una referencia de pago válida"
                );
            }


            if (session.getUrl() == null
                    || session.getUrl().isBlank()) {

                throw new SolicitudInvalidaException(
                        "Stripe no devolvió una URL de pago válida"
                );
            }


            return new StripeCheckoutSession(
                    session.getId(),
                    session.getUrl()
            );


        } catch (StripeException ex) {

            /*
             * No devolvemos detalles internos
             * de Stripe al cliente.
             */
            throw new SolicitudInvalidaException(
                    "No fue posible generar la sesión de pago con Stripe"
            );
        }
    }


    // MJRENEW-STRIPE: reutilizar Checkout pendiente evita crear cobros duplicados.
    // null significa que Stripe confirmó que la sesión venció.
    public StripeCheckoutSession recuperarSesionAbierta(String referenciaStripe) {
        Stripe.apiKey = stripeSecretKey;
        try {
            Session session = Session.retrieve(referenciaStripe);
            if ("expired".equals(session.getStatus())) {
                return null;
            }
            if (!"open".equals(session.getStatus())) {
                throw new SolicitudInvalidaException(
                        "Este pago está siendo confirmado. Actualiza tu panel en unos momentos");
            }
            if (session.getUrl() == null || session.getUrl().isBlank()) {
                throw new SolicitudInvalidaException("La sesión de pago no tiene una URL válida");
            }
            return new StripeCheckoutSession(session.getId(), session.getUrl());
        } catch (StripeException ex) {
            throw new SolicitudInvalidaException("No fue posible consultar la sesión de Stripe");
        }
    }

    private void validarTransaccion(
            TransaccionBancaria transaccion
    ) {

        if (transaccion == null) {

            throw new SolicitudInvalidaException(
                    "La transacción de pago es obligatoria"
            );
        }


        if (transaccion.getTransaccionId() == null) {

            throw new SolicitudInvalidaException(
                    "La transacción debe estar registrada antes de generar el pago"
            );
        }


        if (transaccion.getMontoBrutoMxnTransaccion()
                == null) {

            throw new SolicitudInvalidaException(
                    "La transacción no contiene un monto de pago"
            );
        }


        if (transaccion
                .getMontoBrutoMxnTransaccion()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new SolicitudInvalidaException(
                    "El monto del pago debe ser mayor que cero"
            );
        }
    }


    private long convertirPesosACentavos(
            BigDecimal montoMxn
    ) {

        return montoMxn

                /*
                 * 12500.00
                 *      ↓
                 * 1250000
                 */
                .movePointRight(2)

                /*
                 * Stripe necesita un entero.
                 */
                .setScale(
                        0,
                        RoundingMode.HALF_UP
                )

                /*
                 * Si el número no cabe en long,
                 * preferimos fallar en vez de
                 * enviar un monto incorrecto.
                 */
                .longValueExact();
    }
}