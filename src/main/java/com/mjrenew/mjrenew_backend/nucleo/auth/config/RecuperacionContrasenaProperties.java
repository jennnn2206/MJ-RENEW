package com.mjrenew.mjrenew_backend.nucleo.auth.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mjrenew.auth.recuperacion")
public record RecuperacionContrasenaProperties(
        @DefaultValue("30") @Min(1) @Max(1440) int vigenciaMinutos
) {}
