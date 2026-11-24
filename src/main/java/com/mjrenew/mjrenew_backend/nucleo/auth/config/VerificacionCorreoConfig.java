package com.mjrenew.mjrenew_backend.nucleo.auth.config;

import com.mjrenew.mjrenew_backend.nucleo.exception.CorreoNoVerificadoException;
import com.mjrenew.mjrenew_backend.nucleo.security.CustomUserDetailsService;
import com.mjrenew.mjrenew_backend.nucleo.security.UsuarioPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Clock;

@Configuration
public class VerificacionCorreoConfig {
    @Bean
    public Clock verificacionCorreoClock() { return Clock.systemUTC(); }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService usuarios, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(usuarios);
        provider.setPasswordEncoder(encoder);
        // Ejecutar después de comprobar la contraseña, para conservar el error genérico si es incorrecta.
        provider.setPostAuthenticationChecks(detalles -> {
            var principal = (UsuarioPrincipal) detalles;
            if (!Boolean.TRUE.equals(principal.getUsuario().getCorreoVerificadoUsuario())) {
                throw new CorreoNoVerificadoException();
            }
        });
        return provider;
    }
}
