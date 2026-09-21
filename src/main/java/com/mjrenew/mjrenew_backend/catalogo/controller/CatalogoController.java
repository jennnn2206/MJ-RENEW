package com.mjrenew.mjrenew_backend.catalogo.controller;

import com.mjrenew.mjrenew_backend.catalogo.dto.CatalogoAntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.catalogo.dto.CatalogoAntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.catalogo.dto.UrlPagoStripeResponse;
import com.mjrenew.mjrenew_backend.catalogo.service.CatalogoAntiguedadService;
import com.mjrenew.mjrenew_backend.nucleo.security.UsuarioPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/catalogo")
public class CatalogoController {

    private final CatalogoAntiguedadService catalogoService;

    public CatalogoController(CatalogoAntiguedadService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/explorarCatalogo")
    public ResponseEntity<Page<CatalogoAntiguedadResumenResponse>> explorarCatalogo(Pageable pageable) {
        return ResponseEntity.ok(catalogoService.explorarCatalogo(pageable));
    }

    @GetMapping("/obtenerDetallePiezaCatalogo/{catalogoId}")
    public ResponseEntity<CatalogoAntiguedadDetalleResponse> obtenerDetallePiezaCatalogo(@PathVariable UUID catalogoId) {
        return ResponseEntity.ok(catalogoService.obtenerDetalle(catalogoId));
    }

    @PostMapping("/iniciarCompraPiezaCatalogo/{catalogoId}")
    @PreAuthorize("hasRole('COMPRADOR')")
    public ResponseEntity<UrlPagoStripeResponse> iniciarCompraPiezaCatalogo(@PathVariable UUID catalogoId,
                                                                            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(catalogoService.iniciarCompra(catalogoId, principal.getUsuario()));
    }
}