//pendiente

package com.mjrenew.mjrenew_backend.restaurador.mapper;

import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.restaurador.dto.AntiguedadResumenResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RestauradorMapper {

    @Mapping(source = "antiguedadesId", target = "antiguedadId") //consistencias
    @Mapping(source = "tipoMueble", target = "tipoMueble")
    @Mapping(source = "estadoActualAntiguedad", target = "estadoActualAntiguedad")
    @Mapping(target = "urlFotoPortada", ignore = true)
    AntiguedadResumenResponse toResumen(Antiguedad antiguedad);
}