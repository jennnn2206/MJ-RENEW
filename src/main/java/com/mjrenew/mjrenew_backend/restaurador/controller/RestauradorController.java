//pendiente

package com.mjrenew.mjrenew_backend.restaurador.controller;

import com.mjrenew.mjrenew_backend.nucleo.security.UsuarioPrincipal;
import com.mjrenew.mjrenew_backend.restaurador.dto.AntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.restaurador.service.RestauradorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurador")
public class RestauradorController {

    private final RestauradorService restauradorService;

    public RestauradorController(RestauradorService restauradorService) {
        this.restauradorService = restauradorService;
    }

    @GetMapping("/obtenerSolicitudesAsignadas")
    @PreAuthorize("hasRole('RESTAURADOR')")
    public ResponseEntity<Page<AntiguedadResumenResponse>> obtenerSolicitudesAsignadas(
            Pageable pageable,
            @AuthenticationPrincipal UsuarioPrincipal principal
    ) {
        return ResponseEntity.ok(
                restauradorService.obtenerSolicitudesAsignadas(
                        principal.getUsuario().getUsuariosId(),
                        pageable
                )
        );
    }
}