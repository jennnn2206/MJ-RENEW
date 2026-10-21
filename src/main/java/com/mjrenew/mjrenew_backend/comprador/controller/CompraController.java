package com.mjrenew.mjrenew_backend.comprador.controller;

import com.mjrenew.mjrenew_backend.comprador.dto.ConfirmarRecepcionCompraResponse;
import com.mjrenew.mjrenew_backend.comprador.service.CompraService;
import com.mjrenew.mjrenew_backend.nucleo.security.UsuarioPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/comprador")
public class CompraController {
    private final CompraService compras;

    public CompraController(CompraService compras) {
        this.compras = compras;
    }

    /** Ampliación a API v4, no transferencias financieras. */
    @PostMapping("/confirmarRecepcionCompra/{catalogoId}")
    @PreAuthorize("hasRole('COMPRADOR')")
    public ResponseEntity<ConfirmarRecepcionCompraResponse> confirmarRecepcionCompra(
            @PathVariable UUID catalogoId,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(compras.confirmarRecepcion(catalogoId,
                principal.getUsuario().getUsuariosId()));
    }
}
