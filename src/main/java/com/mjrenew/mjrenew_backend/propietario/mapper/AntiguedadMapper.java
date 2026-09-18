package com.mjrenew.mjrenew_backend.propietario.mapper;

import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadCreateRequest;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadResponse;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapeo DTO <-> entidad generado por MapStruct. No asigna propietario, estado ni fechas:
 * esos valores dependen del usuario autenticado y del flujo del AFD,
 * y son responsabilidad del service, no del mapper.
 */
@Mapper(componentModel = "spring")
public interface AntiguedadMapper {

    @Mapping(target = "antiguedadesId", ignore = true)
    @Mapping(target = "propietario", ignore = true)
    @Mapping(target = "restaurador", ignore = true)
    @Mapping(target = "estadoActualAntiguedad", ignore = true)
    @Mapping(target = "registradaEnAntiguedad", ignore = true)
    @Mapping(target = "restauracionInicioAntiguedad", ignore = true)
    @Mapping(target = "restauracionFinAntiguedad", ignore = true)
    Antiguedad toEntity(AntiguedadCreateRequest request);

    @Mapping(target = "propietarioId", source = "propietario.usuariosId")
    @Mapping(target = "nombrePropietario", source = "propietario.nombreCompletoUsuario")
    @Mapping(target = "restauradorId", source = "restaurador.usuariosId")
    @Mapping(target = "nombreRestaurador", source = "restaurador.nombreCompletoUsuario")
    AntiguedadResponse toResponse(Antiguedad antiguedad);
}