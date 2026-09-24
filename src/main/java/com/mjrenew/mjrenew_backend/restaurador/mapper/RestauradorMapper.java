package com.mjrenew.mjrenew_backend.restaurador.mapper;

import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.DisponibilidadRestaurador;
import com.mjrenew.mjrenew_backend.restaurador.dto.AntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.restaurador.dto.PerfilRestauradorPublicoResponse;
import com.mjrenew.mjrenew_backend.restaurador.entity.PerfilRestaurador;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RestauradorMapper {

    @Mapping(
            target = "antiguedadId",
            source = "antiguedad.antiguedadesId"
    )
    @Mapping(
            target = "urlFotoPortada",
            source = "urlFotoPortada"
    )
    AntiguedadResumenResponse toResumen(
            Antiguedad antiguedad,
            String urlFotoPortada
    );

    @Mapping(
            target = "restauradorId",
            source = "restaurador.usuariosId"
    )
    PerfilRestauradorPublicoResponse toPerfilPublico(
            PerfilRestaurador perfilRestaurador
    );

    default String mapDisponibilidad(
            DisponibilidadRestaurador disponibilidad
    ) {
        return disponibilidad != null
                ? disponibilidad.name()
                : null;
    }
}