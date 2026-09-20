package com.mjrenew.mjrenew_backend.propietario.controller;

import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.security.UsuarioPrincipal;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadCreateRequest;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.propietario.dto.CambiarEstadoAntiguedadRequest;
import com.mjrenew.mjrenew_backend.propietario.service.AntiguedadService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/propietario")
public class AntiguedadController {

    private final AntiguedadService antiguedadService;

    public AntiguedadController(AntiguedadService antiguedadService) {
        this.antiguedadService = antiguedadService;
    }

    @PostMapping("/registrarAntiguedad")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<AntiguedadDetalleResponse> registrarAntiguedad(@Valid @RequestBody AntiguedadCreateRequest request,
                                                                         @AuthenticationPrincipal UsuarioPrincipal principal) {
        AntiguedadDetalleResponse response = antiguedadService.crear(request, principal.getUsuario());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/obtenerMisAntiguedades")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<Page<AntiguedadResumenResponse>> obtenerMisAntiguedades(Pageable pageable,
                                                                                  @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(antiguedadService.listarMias(principal.getUsuario().getUsuariosId(), pageable));
    }

    @GetMapping("/obtenerDetalleAntiguedad/{antiguedadId}")
    public ResponseEntity<AntiguedadDetalleResponse> obtenerDetalleAntiguedad(@PathVariable UUID antiguedadId) {
        return ResponseEntity.ok(antiguedadService.obtenerDetalle(antiguedadId));
    }

    // Temporal: el diseño de API reemplaza esto por endpoints atómicos por transición
    // (seleccionarRestaurador, confirmarEvaluacionAntiguedad, etc.), todavía "mock pendiente".
    // Lo dejo mientras esos no existen, para no perder la única forma de mover el AFD en pruebas.
    @PatchMapping("/{antiguedadId}/estado")
    public ResponseEntity<AntiguedadDetalleResponse> cambiarEstado(@PathVariable UUID antiguedadId,
                                                                   @Valid @RequestBody CambiarEstadoAntiguedadRequest request) {
        return ResponseEntity.ok(antiguedadService.cambiarEstado(antiguedadId, request.nuevoEstado()));
    }
}