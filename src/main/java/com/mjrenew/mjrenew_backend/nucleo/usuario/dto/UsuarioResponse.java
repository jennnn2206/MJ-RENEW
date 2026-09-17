package com.mjrenew.mjrenew_backend.nucleo.usuario.dto;

import com.mjrenew.mjrenew_backend.nucleo.enums.TipoUsuario;

import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        String nombreCompleto,
        String correoElectronico,
        TipoUsuario tipoUsuario
) {
}
