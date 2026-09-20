package com.mjrenew.mjrenew_backend.transportista.controller;

import com.mjrenew.mjrenew_backend.nucleo.security.UsuarioPrincipal;
import com.mjrenew.mjrenew_backend.transportista.dto.ConfirmarLlegadaRequest;
import com.mjrenew.mjrenew_backend.transportista.dto.ConfirmarSalidaRequest;
import com.mjrenew.mjrenew_backend.transportista.dto.TrasladoAntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.transportista.dto.TrasladoAntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.transportista.service.TrasladoAntiguedadService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/transportista")
public class TransportistaController {

    private final TrasladoAntiguedadService trasladoService;

    public TransportistaController(TrasladoAntiguedadService trasladoService) {
        this.trasladoService = trasladoService;
    }

    @GetMapping("/obtenerMisTraslados")
    @PreAuthorize("hasRole('TRANSPORTISTA')")
    public ResponseEntity<Page<TrasladoAntiguedadResumenResponse>> obtenerMisTraslados(Pageable pageable,
                                                                                       @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(trasladoService.listarMisTraslados(principal.getUsuario().getUsuariosId(), pageable));
    }

    @GetMapping("/obtenerDetalleTraslado/{trasladoId}")
    @PreAuthorize("hasRole('TRANSPORTISTA')")
    public ResponseEntity<TrasladoAntiguedadDetalleResponse> obtenerDetalleTraslado(@PathVariable UUID trasladoId,
                                                                                    @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(trasladoService.obtenerDetalle(trasladoId, principal.getUsuario().getUsuariosId()));
    }

    @PostMapping("/confirmarSalidaRecoleccion/{trasladoId}")
    @PreAuthorize("hasRole('TRANSPORTISTA')")
    public ResponseEntity<TrasladoAntiguedadResumenResponse> confirmarSalidaRecoleccion(@PathVariable UUID trasladoId,
                                                                                        @Valid @RequestBody ConfirmarSalidaRequest request,
                                                                                        @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(trasladoService.confirmarSalidaRecoleccion(trasladoId, principal.getUsuario().getUsuariosId(), request));
    }

    @PostMapping("/confirmarLlegadaTaller/{trasladoId}")
    @PreAuthorize("hasRole('TRANSPORTISTA')")
    public ResponseEntity<TrasladoAntiguedadResumenResponse> confirmarLlegadaTaller(@PathVariable UUID trasladoId,
                                                                                    @Valid @RequestBody ConfirmarLlegadaRequest request,
                                                                                    @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(trasladoService.confirmarLlegadaTaller(trasladoId, principal.getUsuario().getUsuariosId(), request));
    }

    @PostMapping("/confirmarSalidaEntregaPropietario/{trasladoId}")
    @PreAuthorize("hasRole('TRANSPORTISTA')")
    public ResponseEntity<TrasladoAntiguedadResumenResponse> confirmarSalidaEntregaPropietario(@PathVariable UUID trasladoId,
                                                                                               @Valid @RequestBody ConfirmarSalidaRequest request,
                                                                                               @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(trasladoService.confirmarSalidaEntregaPropietario(trasladoId, principal.getUsuario().getUsuariosId(), request));
    }

    @PostMapping("/confirmarEntregaPropietario/{trasladoId}")
    @PreAuthorize("hasRole('TRANSPORTISTA')")
    public ResponseEntity<TrasladoAntiguedadResumenResponse> confirmarEntregaPropietario(@PathVariable UUID trasladoId,
                                                                                         @Valid @RequestBody ConfirmarLlegadaRequest request,
                                                                                         @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(trasladoService.confirmarEntregaPropietario(trasladoId, principal.getUsuario().getUsuariosId(), request));
    }
}