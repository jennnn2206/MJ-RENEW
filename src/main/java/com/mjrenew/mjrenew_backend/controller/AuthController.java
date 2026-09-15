package com.mjrenew.mjrenew_backend.controller;

import com.mjrenew.mjrenew_backend.dto.usuario.LoginRequest;
import com.mjrenew.mjrenew_backend.dto.usuario.UsuarioRegistroRequest;
import com.mjrenew.mjrenew_backend.dto.usuario.UsuarioResponse;
import com.mjrenew.mjrenew_backend.mapper.UsuarioMapper;
import com.mjrenew.mjrenew_backend.security.UsuarioPrincipal;
import com.mjrenew.mjrenew_backend.service.AuthService;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody UsuarioRegistroRequest request) {
        UsuarioResponse response = authService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<UsuarioResponse> iniciarSesion(@Valid @RequestBody LoginRequest request,
                                                           HttpServletRequest httpRequest,
                                                           HttpServletResponse httpResponse) {
        UsuarioResponse response = authService.iniciarSesion(request, httpRequest, httpResponse);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> cerrarSesion(HttpServletRequest httpRequest) {
        authService.cerrarSesion(httpRequest);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/yo")
    public ResponseEntity<UsuarioResponse> yo(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(UsuarioMapper.toResponse(principal.getUsuario()));
    }
}
