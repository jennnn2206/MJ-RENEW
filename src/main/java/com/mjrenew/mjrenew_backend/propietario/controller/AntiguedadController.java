package com.mjrenew.mjrenew_backend.propietario.controller;

import com.mjrenew.mjrenew_backend.nucleo.security.UsuarioPrincipal;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadCreateRequest;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.propietario.dto.SeleccionarRestauradorRequest;
import com.mjrenew.mjrenew_backend.propietario.service.AntiguedadService;
import com.mjrenew.mjrenew_backend.restaurador.dto.PerfilRestauradorPublicoResponse;
import com.mjrenew.mjrenew_backend.restaurador.service.RestauradorService;
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
    private final RestauradorService restauradorService;

    public AntiguedadController(
            AntiguedadService antiguedadService,
            RestauradorService restauradorService
    ) {
        this.antiguedadService = antiguedadService;
        this.restauradorService = restauradorService;
    }

    @PostMapping("/registrarAntiguedad")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<AntiguedadDetalleResponse> registrarAntiguedad(
            @Valid @RequestBody AntiguedadCreateRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal
    ) {

        AntiguedadDetalleResponse response =
                antiguedadService.crear(
                        request,
                        principal.getUsuario()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/obtenerMisAntiguedades")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<Page<AntiguedadResumenResponse>>
    obtenerMisAntiguedades(
            Pageable pageable,
            @AuthenticationPrincipal UsuarioPrincipal principal
    ) {

        return ResponseEntity.ok(
                antiguedadService.listarMias(
                        principal.getUsuario().getUsuariosId(),
                        pageable
                )
        );
    }

    @GetMapping("/obtenerDetalleAntiguedad/{antiguedadId}")
    public ResponseEntity<AntiguedadDetalleResponse>
    obtenerDetalleAntiguedad(
            @PathVariable UUID antiguedadId
    ) {

        return ResponseEntity.ok(
                antiguedadService.obtenerDetalle(
                        antiguedadId
                )
        );
    }

    @GetMapping("/buscarRestauradores")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<Page<PerfilRestauradorPublicoResponse>>
    buscarRestauradores(
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                restauradorService.buscarRestauradores(
                        pageable
                )
        );
    }

    @PostMapping("/seleccionarRestaurador/{antiguedadId}")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<AntiguedadDetalleResponse>
    seleccionarRestaurador(
            @PathVariable UUID antiguedadId,
            @Valid @RequestBody SeleccionarRestauradorRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal
    ) {

        return ResponseEntity.ok(
                antiguedadService.seleccionarRestaurador(
                        antiguedadId,
                        principal.getUsuario().getUsuariosId(),
                        request
                )
        );
    }
}