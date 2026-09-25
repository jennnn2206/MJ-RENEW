package com.mjrenew.mjrenew_backend.nucleo.usuario.mapper;

import com.mjrenew.mjrenew_backend.nucleo.usuario.dto.UsuarioRegistroRequest;
import com.mjrenew.mjrenew_backend.nucleo.usuario.dto.UsuarioResponse;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapeo DTO <-> entidad generado por MapStruct.
 * La contraseña se procesa en AuthService porque su cifrado
 * pertenece a la lógica de seguridad.
 */
@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(
            target = "nombreCompletoUsuario",
            source = "nombreCompleto"
    )
    @Mapping(
            target = "correoElectronicoUsuario",
            source = "correoElectronico"
    )
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

    @Mapping(
            target = "id",
            source = "usuariosId"
    )
    @Mapping(
            target = "nombreCompleto",
            source = "nombreCompletoUsuario"
    )
    @Mapping(
            target = "correoElectronico",
            source = "correoElectronicoUsuario"
    )
    UsuarioResponse toResponse(Usuario usuario);
}