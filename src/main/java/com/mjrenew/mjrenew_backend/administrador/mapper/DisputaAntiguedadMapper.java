package com.mjrenew.mjrenew_backend.administrador.mapper;

import com.mjrenew.mjrenew_backend.administrador.dto.DisputaAntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.administrador.dto.DisputaAntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.administrador.entity.DisputaAntiguedad;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DisputaAntiguedadMapper {

    @Mapping(target = "disputaId", source = "disputasAntiguedadId")
    @Mapping(target = "antiguedadId", source = "antiguedad.antiguedadesId")
    @Mapping(target = "nombreAbiertaPor", source = "abiertaPor.nombreCompletoUsuario")
    @Mapping(target = "abiertaEn", source = "abiertaEnDisputa")
    DisputaAntiguedadResumenResponse toResumen(DisputaAntiguedad disputa);

    @Mapping(target = "disputaId", source = "disputa.disputasAntiguedadId")
    @Mapping(target = "antiguedadId", source = "disputa.antiguedad.antiguedadesId")
    @Mapping(target = "nombreAbiertaPor", source = "disputa.abiertaPor.nombreCompletoUsuario")
    @Mapping(target = "nombreAdminAsignado", source = "disputa.adminAsignado.nombreCompletoUsuario")
    @Mapping(target = "abiertaEn", source = "disputa.abiertaEnDisputa")
    @Mapping(target = "resueltaEn", source = "disputa.resueltaEnDisputa")
    @Mapping(target = "fotos", source = "fotos")
    DisputaAntiguedadDetalleResponse toDetalle(DisputaAntiguedad disputa, List<String> fotos);
}