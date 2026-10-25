package com.mjrenew.mjrenew_backend.administrador.controller;

import com.mjrenew.mjrenew_backend.administrador.dto.DisputaAntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.administrador.dto.DisputaAntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.administrador.dto.ResolverDisputaRequest;
import com.mjrenew.mjrenew_backend.administrador.dto.AsignarTransportistaEntregaRequest;
import com.mjrenew.mjrenew_backend.administrador.service.AdministradorService;
import com.mjrenew.mjrenew_backend.nucleo.security.UsuarioPrincipal;
import com.mjrenew.mjrenew_backend.nucleo.usuario.dto.UsuarioResponse;
import com.mjrenew.mjrenew_backend.restaurador.dto.PerfilRestauradorCompletoResponse;
import com.mjrenew.mjrenew_backend.restaurador.dto.PerfilRestauradorPublicoResponse;
import com.mjrenew.mjrenew_backend.transportista.dto.TrasladoAntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.transportista.service.TrasladoAntiguedadService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final TrasladoAntiguedadService trasladoService;

    public AdminController(AdministradorService administradorService,
                           TrasladoAntiguedadService trasladoService) {
        this.administradorService = administradorService;
        this.trasladoService = trasladoService;
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

    /** Cola administrativa de solicitudes RF-024 aún sin transportista. */
    @GetMapping("/obtenerEntregasCompradorPendientesAsignacion")
    public ResponseEntity<Page<TrasladoAntiguedadDetalleResponse>> obtenerEntregasCompradorPendientesAsignacion(
            Pageable pageable) {
        Pageable paginaSegura = PageRequest.of(Math.max(0, pageable.getPageNumber()),
                Math.max(1, Math.min(pageable.getPageSize(), 50)), Sort.by(Sort.Direction.ASC, "agendadoEnTraslado"));
        return ResponseEntity.ok(trasladoService.obtenerEntregasCompradorPendientesAsignacion(paginaSegura));
    }

    @PostMapping("/asignarTransportistaEntregaComprador/{trasladoId}")
    public ResponseEntity<TrasladoAntiguedadDetalleResponse> asignarTransportistaEntregaComprador(
            @PathVariable UUID trasladoId,
            @Valid @RequestBody AsignarTransportistaEntregaRequest request) {
        return ResponseEntity.ok(trasladoService.asignarTransportistaEntregaComprador(
                trasladoId, request.transportistaId()));
    }

    @GetMapping("/obtenerRestauradoresPendientesAprobacion")
    public ResponseEntity<Page<PerfilRestauradorPublicoResponse>> obtenerRestauradoresPendientesAprobacion(Pageable pageable) {
        return ResponseEntity.ok(administradorService.obtenerRestauradoresPendientesAprobacion(pageable));
    }

    @PostMapping("/aprobarRestaurador/{restauradorId}")
    public ResponseEntity<PerfilRestauradorCompletoResponse> aprobarRestaurador(@PathVariable UUID restauradorId) {
        return ResponseEntity.ok(administradorService.aprobarRestaurador(restauradorId));
    }
}