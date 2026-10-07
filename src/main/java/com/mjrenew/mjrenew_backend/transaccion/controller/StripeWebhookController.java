package com.mjrenew.mjrenew_backend.transaccion.controller;

import com.mjrenew.mjrenew_backend.transaccion.service.StripeWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class StripeWebhookController {

    private final StripeWebhookService stripeWebhookService;

    public StripeWebhookController(
            StripeWebhookService stripeWebhookService
    ) {
        this.stripeWebhookService = stripeWebhookService;
    }


    @PostMapping("/webhook/stripe")
    public ResponseEntity<Void> recibirEventoStripe(
            @RequestBody String payload,

            @RequestHeader(
                    value = "Stripe-Signature",
                    required = false
            )
            String stripeSignature
    ) {

        stripeWebhookService.procesarEvento(
                payload,
                stripeSignature
        );

        return ResponseEntity.ok().build();
    }
}