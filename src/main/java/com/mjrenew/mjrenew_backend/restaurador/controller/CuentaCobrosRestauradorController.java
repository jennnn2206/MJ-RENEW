package com.mjrenew.mjrenew_backend.restaurador.controller;

import java.util.List;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTransaccion;
import com.mjrenew.mjrenew_backend.transaccion.dto.DesgloseLiberacion;
import com.mjrenew.mjrenew_backend.transaccion.repository.TransaccionBancariaRepository;
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
@RequestMapping("/api/restaurador")
public class CuentaCobrosRestauradorController {

    private final TransaccionBancariaRepository pagos;
    private final CuentaStripeConectadaService cuentaStripeService;

    public CuentaCobrosRestauradorController(CuentaStripeConectadaService cuentaStripeService,
            TransaccionBancariaRepository pagos) {
        this.pagos = pagos;
        this.cuentaStripeService = cuentaStripeService;
    }

    @GetMapping("/obtenerMisCobros")
    @PreAuthorize("hasRole('RESTAURADOR')")
    public List<DesgloseLiberacion> obtenerMisCobros(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return pagos.obtenerCobros(principal.getUsuario().getUsuariosId(),
                TipoTransaccion.LIBERACION_RESTAURADOR);
    }

    /** Devuelve una URL temporal hospedada por Stripe para completar el onboarding. */
    @PostMapping("/iniciarOnboardingCuentaStripe")
    @PreAuthorize("hasRole('RESTAURADOR')")
    public ResponseEntity<StripeOnboardingResponse> iniciarOnboardingCuentaStripe(
            @AuthenticationPrincipal UsuarioPrincipal principal
    ) {
        return ResponseEntity.ok(cuentaStripeService.iniciarOnboarding(principal.getUsuario()));
    }

    /** Consulta el estado actual de la cuenta conectada del restaurador autenticado. */
    @GetMapping("/obtenerEstadoCuentaStripe")
    @PreAuthorize("hasRole('RESTAURADOR')")
    public ResponseEntity<CuentaStripeEstadoResponse> obtenerEstadoCuentaStripe(
            @AuthenticationPrincipal UsuarioPrincipal principal
    ) {
        return ResponseEntity.ok(cuentaStripeService.obtenerEstado(principal.getUsuario()));
    }
}
