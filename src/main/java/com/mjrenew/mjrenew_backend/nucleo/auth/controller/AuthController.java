package com.mjrenew.mjrenew_backend.nucleo.auth.controller;

import com.mjrenew.mjrenew_backend.nucleo.usuario.dto.LoginRequest;
import com.mjrenew.mjrenew_backend.nucleo.usuario.dto.UsuarioRegistroRequest;
import com.mjrenew.mjrenew_backend.nucleo.usuario.dto.UsuarioResponse;
import com.mjrenew.mjrenew_backend.nucleo.usuario.mapper.UsuarioMapper;
import com.mjrenew.mjrenew_backend.nucleo.security.UsuarioPrincipal;
import com.mjrenew.mjrenew_backend.nucleo.auth.service.AuthService;
import com.mjrenew.mjrenew_backend.nucleo.auth.service.VerificacionCorreoService;
import com.mjrenew.mjrenew_backend.nucleo.auth.dto.VerificarCorreoRequest;
import com.mjrenew.mjrenew_backend.nucleo.auth.dto.ReenviarVerificacionCorreoRequest;
import com.mjrenew.mjrenew_backend.nucleo.auth.dto.SolicitarRecuperacionContrasenaRequest;
import com.mjrenew.mjrenew_backend.nucleo.auth.dto.RestablecerContrasenaRequest;
import com.mjrenew.mjrenew_backend.nucleo.auth.service.RecuperacionContrasenaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UsuarioMapper usuarioMapper;
    private final VerificacionCorreoService verificacionCorreoService;
    private final RecuperacionContrasenaService recuperacionContrasenaService;

    public AuthController(AuthService authService, UsuarioMapper usuarioMapper,
                          VerificacionCorreoService verificacionCorreoService,
                          RecuperacionContrasenaService recuperacionContrasenaService) {
        this.authService = authService;
        this.usuarioMapper = usuarioMapper;
        this.verificacionCorreoService = verificacionCorreoService;
        this.recuperacionContrasenaService = recuperacionContrasenaService;
    }

    @PostMapping("/solicitarRecuperacionContrasena")
    public ResponseEntity<Map<String, String>> solicitarRecuperacionContrasena(
            @Valid @RequestBody SolicitarRecuperacionContrasenaRequest request) {
        recuperacionContrasenaService.solicitar(request.correoElectronico());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("mensaje",
                "Si la cuenta puede recuperar su contraseña, recibirás un enlace. Revisa también la carpeta de spam. "
                        + "Espera un minuto antes de solicitar otro."));
    }

    @PostMapping("/restablecerContrasena")
    public ResponseEntity<Map<String, String>> restablecerContrasena(
            @Valid @RequestBody RestablecerContrasenaRequest request) {
        recuperacionContrasenaService.restablecer(request.token(), request.nuevaContrasena());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("mensaje",
                "Tu contraseña quedó actualizada. Inicia sesión con la nueva contraseña. "
                        + "Si tu correo sigue pendiente de verificación, tendrás que verificarlo."));
    }

    @PostMapping("/verificarCorreo")
    public ResponseEntity<Map<String, String>> verificarCorreo(@Valid @RequestBody VerificarCorreoRequest request) {
        verificacionCorreoService.verificar(request.token());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Map.of("mensaje", "Tu correo quedó verificado. Ya puedes iniciar sesión."));
    }

    @PostMapping("/reenviarVerificacionCorreo")
    public ResponseEntity<Map<String, String>> reenviarVerificacionCorreo(
            @Valid @RequestBody ReenviarVerificacionCorreoRequest request) {
        verificacionCorreoService.reenviar(request.correoElectronico());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("mensaje",
                "Si la cuenta está pendiente de verificación, recibirás un enlace. Revisa también tu carpeta de spam. "
                        + "Espera un minuto antes de solicitar otro."));
    }

    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> obtenerCsrf(HttpServletRequest httpRequest) {
        CsrfToken token = (CsrfToken) httpRequest.getAttribute(CsrfToken.class.getName());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("headerName", token.getHeaderName(), "token", token.getToken()));
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
