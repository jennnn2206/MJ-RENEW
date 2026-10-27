package com.mjrenew.mjrenew_backend.propietario.dto;

/** URL temporal de Stripe; el backend no recibe ni almacena documentos bancarios. */
public record StripeOnboardingResponse(String urlOnboarding) {
}
