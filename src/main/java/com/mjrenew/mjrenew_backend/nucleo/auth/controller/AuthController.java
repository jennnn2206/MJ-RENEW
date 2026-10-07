package com.mjrenew.mjrenew_backend.nucleo.auth.controller;

import com.mjrenew.mjrenew_backend.nucleo.usuario.dto.LoginRequest;
import com.mjrenew.mjrenew_backend.nucleo.usuario.dto.UsuarioRegistroRequest;
import com.mjrenew.mjrenew_backend.nucleo.usuario.dto.UsuarioResponse;
import com.mjrenew.mjrenew_backend.nucleo.usuario.mapper.UsuarioMapper;
import com.mjrenew.mjrenew_backend.nucleo.security.UsuarioPrincipal;
import com.mjrenew.mjrenew_backend.nucleo.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UsuarioMapper usuarioMapper;

    public AuthController(AuthService authService, UsuarioMapper usuarioMapper) {
        this.authService = authService;
        this.usuarioMapper = usuarioMapper;
    }

    @PostMapping("/registrarUsuario")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody UsuarioRegistroRequest request) {
        UsuarioResponse response = authService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/iniciarSesion")
    public ResponseEntity<UsuarioResponse> iniciarSesion(@Valid @RequestBody LoginRequest request,
                                                         HttpServletRequest httpRequest,
                                                         HttpServletResponse httpResponse) {
        UsuarioResponse response = authService.iniciarSesion(request, httpRequest, httpResponse);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cerrarSesion")
    public ResponseEntity<Void> cerrarSesion(HttpServletRequest httpRequest) {
        authService.cerrarSesion(httpRequest);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/obtenerUsuarioActual")
    public ResponseEntity<UsuarioResponse> obtenerUsuarioActual(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(usuarioMapper.toResponse(principal.getUsuario()));
    }
}