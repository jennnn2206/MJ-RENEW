package com.mjrenew.mjrenew_backend.restaurador.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record CerrarRestauracionRequest(
        @NotNull(message = "Agrega las fotografías finales")
        @Size(min = 4, message = "Se requieren al menos 4 fotografías del estado final")
        List<MultipartFile> fotos
) {}
