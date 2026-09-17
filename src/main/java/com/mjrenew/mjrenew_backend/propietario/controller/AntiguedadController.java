package com.mjrenew.mjrenew_backend.propietario.controller;

import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadCreateRequest;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadResponse;
import com.mjrenew.mjrenew_backend.propietario.dto.CambiarEstadoAntiguedadRequest;
import com.mjrenew.mjrenew_backend.nucleo.security.UsuarioPrincipal;
import com.mjrenew.mjrenew_backend.propietario.service.AntiguedadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/antiguedades")
public class AntiguedadController {

    private final AntiguedadService antiguedadService;

    public AntiguedadController(AntiguedadService antiguedadService) {
        this.antiguedadService = antiguedadService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<AntiguedadResponse> crear(@Valid @RequestBody AntiguedadCreateRequest request,
                                                      @AuthenticationPrincipal UsuarioPrincipal principal) {
        AntiguedadResponse response = antiguedadService.crear(request, principal.getUsuario());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mias")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<List<AntiguedadResponse>> listarMias(@AuthenticationPrincipal UsuarioPrincipal principal) {
        List<AntiguedadResponse> piezas = antiguedadService.listarMias(principal.getUsuario().getUsuariosId());
        return ResponseEntity.ok(piezas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AntiguedadResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(antiguedadService.obtenerPorId(id));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<AntiguedadResponse> cambiarEstado(@PathVariable UUID id,
                                                              @Valid @RequestBody CambiarEstadoAntiguedadRequest request) {
        AntiguedadResponse response = antiguedadService.cambiarEstado(id, request.nuevoEstado());
        return ResponseEntity.ok(response);
    }
}
