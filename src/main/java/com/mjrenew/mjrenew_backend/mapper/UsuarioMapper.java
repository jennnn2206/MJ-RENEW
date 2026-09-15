package com.mjrenew.mjrenew_backend.mapper;

import com.mjrenew.mjrenew_backend.dto.usuario.UsuarioRegistroRequest;
import com.mjrenew.mjrenew_backend.dto.usuario.UsuarioResponse;
import com.mjrenew.mjrenew_backend.entity.Usuario;

/**
 * Mapeo manual DTO <-> entidad. No incluye la contraseña: encriptarla es una
 * decisión de seguridad que le corresponde al service, no a un mapper.
 */
public final class UsuarioMapper {

    private UsuarioMapper() {
    }

    public static Usuario toEntity(UsuarioRegistroRequest request) {
        Usuario usuario = new Usuario();
        usuario.setNombreCompletoUsuario(request.nombreCompleto());
        usuario.setCorreoElectronicoUsuario(request.correoElectronico());
        usuario.setTipoUsuario(request.tipoUsuario());
        return usuario;
    }

    public static UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getUsuariosId(),
                usuario.getNombreCompletoUsuario(),
                usuario.getCorreoElectronicoUsuario(),
                usuario.getTipoUsuario()
        );
    }
}
