package com.mjrenew.mjrenew_backend.propietario.controller;

import com.mjrenew.mjrenew_backend.nucleo.security.UsuarioPrincipal;
import com.mjrenew.mjrenew_backend.propietario.dto.CuentaStripeEstadoResponse;
import com.mjrenew.mjrenew_backend.propietario.dto.StripeOnboardingResponse;
import com.mjrenew.mjrenew_backend.propietario.service.CuentaStripeConectadaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/propietario")
public class CuentaStripeController {

    private final CuentaStripeConectadaService cuentaStripeService;

    public CuentaStripeController(CuentaStripeConectadaService cuentaStripeService) {
        this.cuentaStripeService = cuentaStripeService;
    }

    /** Devuelve una URL temporal hospedada por Stripe para completar el onboarding. */
    @PostMapping("/iniciarOnboardingCuentaStripe")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<StripeOnboardingResponse> iniciarOnboardingCuentaStripe(
            @AuthenticationPrincipal UsuarioPrincipal principal
    ) {
        return ResponseEntity.ok(cuentaStripeService.iniciarOnboarding(principal.getUsuario()));
    }

    /** Consulta el estado actual de la cuenta conectada del propietario autenticado. */
    @GetMapping("/obtenerEstadoCuentaStripe")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<CuentaStripeEstadoResponse> obtenerEstadoCuentaStripe(
            @AuthenticationPrincipal UsuarioPrincipal principal
    ) {
        return ResponseEntity.ok(cuentaStripeService.obtenerEstado(principal.getUsuario()));
    }
}
