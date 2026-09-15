package com.mjrenew.mjrenew_backend.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Las 8 rutas de PageController (vistas Thymeleaf del mockup) y los recursos
 * estáticos quedan explícitamente públicos para no romper el frontend existente,
 * que todavía no consume esta API. Solo /api/** requiere autenticación (salvo
 * registro/login, que son el punto de entrada).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS_VISTAS = {
            "/", "/login", "/registro", "/catalogo", "/pieza",
            "/comprador", "/propietario", "/restaurador",
            "/css/**", "/js/**"
    };

    private static final String[] RUTAS_PUBLICAS_API = {
            "/api/auth/registro", "/api/auth/login"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * Repositorio explícito de sesión: AuthService autentica manualmente dentro de
     * un @RestController (no a través de un filtro de login), así que debe guardar
     * el SecurityContext aquí mismo para que las siguientes peticiones reconozcan
     * al usuario. Sin este paso el login respondería 200 pero la sesión no persistiría.
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository securityContextRepository) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(RUTAS_PUBLICAS_VISTAS).permitAll()
                        .requestMatchers(RUTAS_PUBLICAS_API).permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No autenticado")
                ));

        return http.build();
    }
}
