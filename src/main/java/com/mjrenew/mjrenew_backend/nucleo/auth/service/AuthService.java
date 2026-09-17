package com.mjrenew.mjrenew_backend.nucleo.auth.service;

import com.mjrenew.mjrenew_backend.nucleo.usuario.dto.LoginRequest;
import com.mjrenew.mjrenew_backend.nucleo.usuario.dto.UsuarioRegistroRequest;
import com.mjrenew.mjrenew_backend.nucleo.usuario.dto.UsuarioResponse;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.nucleo.exception.CorreoYaRegistradoException;
import com.mjrenew.mjrenew_backend.nucleo.exception.CredencialesInvalidasException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.mapper.UsuarioMapper;
import com.mjrenew.mjrenew_backend.nucleo.usuario.repository.UsuarioRepository;
import com.mjrenew.mjrenew_backend.nucleo.security.UsuarioPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthService(UsuarioRepository usuarioRepository,
                        PasswordEncoder passwordEncoder,
                        AuthenticationManager authenticationManager,
                        SecurityContextRepository securityContextRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @Transactional
    public UsuarioResponse registrar(UsuarioRegistroRequest request) {
        if (usuarioRepository.existsByCorreoElectronicoUsuario(request.correoElectronico())) {
            throw new CorreoYaRegistradoException("Ya existe una cuenta registrada con ese correo");
        }

        Usuario usuario = UsuarioMapper.toEntity(request);
        usuario.setContrasenaHashUsuario(passwordEncoder.encode(request.contrasena()));
        usuario.setCorreoVerificadoUsuario(false);
        usuario.setActivoUsuario(true);
        usuario.setRegistradoEnUsuario(OffsetDateTime.now());

        Usuario guardado = usuarioRepository.save(usuario);
        return UsuarioMapper.toResponse(guardado);
    }

    public UsuarioResponse iniciarSesion(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.correoElectronico(), request.contrasena())
            );
        } catch (BadCredentialsException ex) {
            throw new CredencialesInvalidasException("Correo o contraseña incorrectos");
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        Usuario usuario = ((UsuarioPrincipal) authentication.getPrincipal()).getUsuario();
        return UsuarioMapper.toResponse(usuario);
    }

    public void cerrarSesion(HttpServletRequest httpRequest) {
        SecurityContextHolder.clearContext();
        var sesion = httpRequest.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
    }
}
