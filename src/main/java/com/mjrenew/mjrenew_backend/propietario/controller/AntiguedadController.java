package com.mjrenew.mjrenew_backend.propietario.controller;

import com.mjrenew.mjrenew_backend.catalogo.dto.CatalogoAntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.catalogo.dto.UrlPagoStripeResponse;
import com.mjrenew.mjrenew_backend.nucleo.security.UsuarioPrincipal;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadCreateRequest;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.propietario.dto.PublicarEnCatalogoRequest;
import com.mjrenew.mjrenew_backend.propietario.dto.RechazarCotizacionRequest;
import com.mjrenew.mjrenew_backend.propietario.dto.SeleccionarRestauradorRequest;
import com.mjrenew.mjrenew_backend.propietario.service.AntiguedadService;
import com.mjrenew.mjrenew_backend.restaurador.dto.AvanceRestauracionResumenResponse;
import com.mjrenew.mjrenew_backend.restaurador.dto.CotizacionResumenResponse;
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

    @GetMapping("/obtenerCotizacion/{antiguedadId}")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<CotizacionResumenResponse>
    obtenerCotizacion(
            @PathVariable UUID antiguedadId,
            @AuthenticationPrincipal UsuarioPrincipal principal
    ) {

        return ResponseEntity.ok(
                antiguedadService.obtenerCotizacion(
                        antiguedadId,
                        principal.getUsuario().getUsuariosId()
                )
        );
    }

    @PostMapping("/aceptarCotizacion/{antiguedadId}")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<UrlPagoStripeResponse>
    aceptarCotizacion(
            @PathVariable UUID antiguedadId,
            @AuthenticationPrincipal UsuarioPrincipal principal
    ) {

        return ResponseEntity.ok(
                antiguedadService.aceptarCotizacion(
                        antiguedadId,
                        principal.getUsuario().getUsuariosId()
                )
        );
    }

    @PostMapping("/rechazarCotizacion/{antiguedadId}")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<AntiguedadDetalleResponse>
    rechazarCotizacion(
            @PathVariable UUID antiguedadId,
            @Valid @RequestBody RechazarCotizacionRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal
    ) {

        return ResponseEntity.ok(
                antiguedadService.rechazarCotizacion(
                        antiguedadId,
                        principal.getUsuario().getUsuariosId(),
                        request
                )
        );
    }

    @GetMapping("/obtenerAvancesRestauracion/{antiguedadId}")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<Page<AvanceRestauracionResumenResponse>>
    obtenerAvancesRestauracion(
            @PathVariable UUID antiguedadId,
            Pageable pageable,
            @AuthenticationPrincipal UsuarioPrincipal principal
    ) {

        return ResponseEntity.ok(
                antiguedadService.obtenerAvancesRestauracion(
                        antiguedadId,
                        principal.getUsuario().getUsuariosId(),
                        pageable
                )
        );
    }

    @PostMapping("/publicarEnCatalogo/{antiguedadId}")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<CatalogoAntiguedadResumenResponse>
    publicarEnCatalogo(
            @PathVariable UUID antiguedadId,
            @Valid @RequestBody PublicarEnCatalogoRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal
    ) {

        return ResponseEntity.ok(
                antiguedadService.publicarEnCatalogo(
                        antiguedadId,
                        principal.getUsuario().getUsuariosId(),
                        request
                )
        );
    }
}