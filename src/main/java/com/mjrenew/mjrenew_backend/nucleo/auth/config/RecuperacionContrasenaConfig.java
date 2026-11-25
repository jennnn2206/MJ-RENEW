package com.mjrenew.mjrenew_backend.nucleo.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
@EnableConfigurationProperties(RecuperacionContrasenaProperties.class)
public class RecuperacionContrasenaConfig {
    @Bean
    public Clock recuperacionContrasenaClock() { return Clock.systemUTC(); }
}
