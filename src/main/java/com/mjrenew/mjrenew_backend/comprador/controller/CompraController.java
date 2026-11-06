package com.mjrenew.mjrenew_backend.comprador.controller;

import com.mjrenew.mjrenew_backend.comprador.dto.ConfirmarRecepcionCompraResponse;
import com.mjrenew.mjrenew_backend.comprador.dto.CompraResumenResponse;
import com.mjrenew.mjrenew_backend.comprador.dto.ProgramarEntregaCompraRequest;
import com.mjrenew.mjrenew_backend.comprador.dto.ProgramarEntregaCompraResponse;
import com.mjrenew.mjrenew_backend.comprador.service.CompraService;
import com.mjrenew.mjrenew_backend.nucleo.security.UsuarioPrincipal;
import com.mjrenew.mjrenew_backend.transportista.dto.TrasladoAntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.transportista.service.TrasladoAntiguedadService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/comprador")
public class CompraController {
    private final CompraService compras;
    private final TrasladoAntiguedadService traslados;

    public CompraController(CompraService compras, TrasladoAntiguedadService traslados) {
        this.compras = compras;
        this.traslados = traslados;
    }

    /** DTO atómico de "Mis compras"; identidad tomada de la sesión, no de la URL. */
    @GetMapping("/obtenerMisCompras")
    @PreAuthorize("hasRole('COMPRADOR')")
    public ResponseEntity<Page<CompraResumenResponse>> obtenerMisCompras(
            Pageable pageable,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        Pageable paginaSegura = PageRequest.of(Math.max(0, pageable.getPageNumber()),
                Math.max(1, Math.min(pageable.getPageSize(), 50)),
                Sort.by(Sort.Direction.DESC, "completadaEnCatalogo"));
        return ResponseEntity.ok(compras.obtenerMisCompras(
                principal.getUsuario().getUsuariosId(), paginaSegura));
    }

    /**
     * Extensión de RF-024: el comprador propone su ventana; el administrador
     * asigna después un transportista activo. No se modifica el AFD ni se cobra
     * o libera dinero desde este endpoint.
     */
    @PostMapping("/programarEntregaCompra/{catalogoId}")
    @PreAuthorize("hasRole('COMPRADOR')")
    public ResponseEntity<ProgramarEntregaCompraResponse> programarEntregaCompra(
            @PathVariable UUID catalogoId,
            @Valid @RequestBody ProgramarEntregaCompraRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        TrasladoAntiguedadDetalleResponse traslado = traslados.solicitarEntregaComprador(
                catalogoId, principal.getUsuario().getUsuariosId(), request.fechaAcordadaTraslado(),
                request.horaInicioAcordadaTraslado(), request.horaFinAcordadaTraslado());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ProgramarEntregaCompraResponse(catalogoId, traslado.trasladoId(),
                        traslado.fechaAcordada(), traslado.horaInicio(), traslado.horaFin(),
                        traslado.resultadoTraslado(),
                        "Tu ventana quedó registrada. El administrador asignará un transportista."));
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
