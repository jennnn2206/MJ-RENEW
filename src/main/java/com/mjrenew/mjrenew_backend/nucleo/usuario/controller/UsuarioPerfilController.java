package com.mjrenew.mjrenew_backend.nucleo.usuario.controller;

import com.mjrenew.mjrenew_backend.nucleo.security.UsuarioPrincipal;
import com.mjrenew.mjrenew_backend.nucleo.usuario.dto.ActualizarDomicilioRequest;
import com.mjrenew.mjrenew_backend.nucleo.usuario.dto.DomicilioUsuarioResponse;
import com.mjrenew.mjrenew_backend.nucleo.usuario.service.UsuarioPerfilService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuario")
@PreAuthorize("hasAnyRole('PROPIETARIO', 'COMPRADOR')")
public class UsuarioPerfilController {
    private final UsuarioPerfilService perfilService;

    public UsuarioPerfilController(UsuarioPerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping("/obtenerMiDomicilio")
    public ResponseEntity<DomicilioUsuarioResponse> obtenerMiDomicilio(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(perfilService.obtenerMiDomicilio(
                principal.getUsuario().getUsuariosId()));
    }

    @PostMapping("/actualizarMiDomicilio")
    public ResponseEntity<DomicilioUsuarioResponse> actualizarMiDomicilio(
            @Valid @RequestBody ActualizarDomicilioRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(perfilService.actualizarMiDomicilio(
                principal.getUsuario().getUsuariosId(), request));
    }
}
