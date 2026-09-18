package com.mjrenew.mjrenew_backend.nucleo.usuario.mapper;

import com.mjrenew.mjrenew_backend.nucleo.usuario.dto.UsuarioRegistroRequest;
import com.mjrenew.mjrenew_backend.nucleo.usuario.dto.UsuarioResponse;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapeo DTO <-> entidad generado por MapStruct. No incluye la contraseña:
 * encriptarla es una decisión de seguridad que le corresponde al service, no a un mapper.
 */
@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "usuariosId", ignore = true)
    @Mapping(target = "contrasenaHashUsuario", ignore = true)
    @Mapping(target = "telefonoUsuario", ignore = true)
    @Mapping(target = "direccionTextoUsuario", ignore = true)
    @Mapping(target = "latitudUsuario", ignore = true)
    @Mapping(target = "longitudUsuario", ignore = true)
    @Mapping(target = "correoVerificadoUsuario", ignore = true)
    @Mapping(target = "activoUsuario", ignore = true)
    @Mapping(target = "registradoEnUsuario", ignore = true)
    Usuario toEntity(UsuarioRegistroRequest request);

    UsuarioResponse toResponse(Usuario usuario);
}