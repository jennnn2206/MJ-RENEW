package com.mjrenew.mjrenew_backend.nucleo.security;

import com.mjrenew.mjrenew_backend.nucleo.usuario.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** Revoca en la siguiente petición API las sesiones que conservan una contraseña anterior. */
public class SesionCredencialesFilter extends OncePerRequestFilter {
    private final UsuarioRepository usuarios;

    public SesionCredencialesFilter(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        return !ruta.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UsuarioPrincipal principal
                && Boolean.TRUE.equals(principal.getUsuario().getCorreoVerificadoUsuario())
                && !usuarios.conservaCredencialesActivas(principal.getUsuario().getUsuariosId(), principal.getPassword())) {
            SecurityContextHolder.clearContext();
            var sesion = request.getSession(false);
            if (sesion != null) sesion.invalidate();
        }
        chain.doFilter(request, response);
    }
}
