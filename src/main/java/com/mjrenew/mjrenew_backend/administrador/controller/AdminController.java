package com.mjrenew.mjrenew_backend.administrador.controller;

import com.mjrenew.mjrenew_backend.administrador.dto.DisputaAntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.administrador.dto.DisputaAntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.administrador.dto.ResolverDisputaRequest;
import com.mjrenew.mjrenew_backend.administrador.service.AdministradorService;
import com.mjrenew.mjrenew_backend.nucleo.security.UsuarioPrincipal;
import com.mjrenew.mjrenew_backend.nucleo.usuario.dto.UsuarioResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/administrador")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminController {

    private final AdministradorService administradorService;

    public AdminController(AdministradorService administradorService) {
        this.administradorService = administradorService;
    }

    @GetMapping("/obtenerDisputasAbiertas")
    public ResponseEntity<Page<DisputaAntiguedadResumenResponse>> obtenerDisputasAbiertas(Pageable pageable) {
        return ResponseEntity.ok(administradorService.obtenerDisputasAbiertas(pageable));
    }

    @PostMapping("/resolverDisputaAntiguedad/{disputaId}")
    public ResponseEntity<DisputaAntiguedadDetalleResponse> resolverDisputaAntiguedad(@PathVariable UUID disputaId,
                                                                                      @Valid @RequestBody ResolverDisputaRequest request,
                                                                                      @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(administradorService.resolverDisputa(disputaId, principal.getUsuario(), request));
    }

    @GetMapping("/obtenerUsuariosRegistrados")
    public ResponseEntity<Page<UsuarioResponse>> obtenerUsuariosRegistrados(Pageable pageable) {
        return ResponseEntity.ok(administradorService.obtenerUsuariosRegistrados(pageable));
    }
}