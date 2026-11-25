package com.mjrenew.mjrenew_backend.nucleo.notificacion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
public class NotificacionConfig {
    @Bean public Clock notificacionesClock() { return Clock.systemUTC(); }
}
