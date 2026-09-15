package com.mjrenew.mjrenew_backend.dto.usuario;

import com.mjrenew.mjrenew_backend.enums.TipoUsuario;

import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        String nombreCompleto,
        String correoElectronico,
        TipoUsuario tipoUsuario
) {
}
