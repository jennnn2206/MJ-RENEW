package com.mjrenew.mjrenew_backend.transaccion.service;

import com.mjrenew.mjrenew_backend.transaccion.dto.StripeCheckoutSession;
import com.mjrenew.mjrenew_backend.transaccion.entity.TransaccionBancaria;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StripeCheckoutService {

    public StripeCheckoutSession crearSesionPago(
            TransaccionBancaria transaccion
    ) {

        /*
         * IMPLEMENTACIÓN MOCK TEMPORAL.
         *
         * La lógica de negocio ya no conocerá cómo se genera
         * el enlace de Stripe.
         *
         * En la siguiente fase esta parte será sustituida por
         * Stripe Checkout real.
         */

        String referenciaStripe =
                "mock_" + UUID.randomUUID();

        String urlPago =
                "https://checkout.stripe.com/mock/"
                        + referenciaStripe;

        return new StripeCheckoutSession(
                referenciaStripe,
                urlPago
        );
    }
}