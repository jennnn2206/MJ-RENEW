package com.mjrenew.mjrenew_backend.nucleo.notificacion.controller;

import com.mjrenew.mjrenew_backend.nucleo.notificacion.dto.NotificacionResponse;
import com.mjrenew.mjrenew_backend.nucleo.notificacion.service.NotificacionService;
import com.mjrenew.mjrenew_backend.nucleo.security.UsuarioPrincipal;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notificaciones")
@PreAuthorize("isAuthenticated()")
public class NotificacionController {
    private final NotificacionService servicio;
    public NotificacionController(NotificacionService servicio) { this.servicio = servicio; }

    @GetMapping("/obtenerMisNotificaciones")
    public ResponseEntity<Page<NotificacionResponse>> obtenerMisNotificaciones(@AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(defaultValue = "false") boolean soloNoLeidas, @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(servicio.obtenerPropias(principal.getUsuario().getUsuariosId(), soloNoLeidas, pageable));
    }

    @GetMapping("/obtenerResumen")
    public ResponseEntity<Map<String, Long>> obtenerResumen(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Map.of("noLeidas", servicio.contarNoLeidas(principal.getUsuario().getUsuariosId())));
    }

    @PostMapping("/marcarComoLeida/{notificacionId}")
    public ResponseEntity<NotificacionResponse> marcarComoLeida(@PathVariable UUID notificacionId,
                                                               @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(servicio.marcarLeida(notificacionId, principal.getUsuario().getUsuariosId()));
    }
}
