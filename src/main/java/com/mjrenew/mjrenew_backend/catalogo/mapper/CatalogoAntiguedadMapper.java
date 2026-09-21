package com.mjrenew.mjrenew_backend.catalogo.mapper;

import com.mjrenew.mjrenew_backend.catalogo.dto.CatalogoAntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.catalogo.dto.CatalogoAntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import com.mjrenew.mjrenew_backend.propietario.dto.DimensionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CatalogoAntiguedadMapper {

    @Mapping(target = "catalogoId", source = "catalogo.catalogoAntiguedadId")
    @Mapping(target = "antiguedadId", source = "catalogo.antiguedad.antiguedadesId")
    @Mapping(target = "tipoMueble", source = "catalogo.antiguedad.tipoMueble")
    @Mapping(target = "precioMxn", source = "catalogo.precioMxnCatalogo")
    @Mapping(target = "urlFotoPortada", source = "urlFotoPortada")
    CatalogoAntiguedadResumenResponse toResumen(CatalogoAntiguedad catalogo, String urlFotoPortada);

    @Mapping(target = "catalogoId", source = "catalogo.catalogoAntiguedadId")
    @Mapping(target = "antiguedadId", source = "catalogo.antiguedad.antiguedadesId")
    @Mapping(target = "tipoMueble", source = "catalogo.antiguedad.tipoMueble")
    @Mapping(target = "estiloMueble", source = "catalogo.antiguedad.estiloMueble")
    @Mapping(target = "materialMueble", source = "catalogo.antiguedad.materialMueble")
    @Mapping(target = "descripcionDaniosAntiguedad", source = "catalogo.antiguedad.descripcionDaniosAntiguedad")
    @Mapping(target = "procedenciaMueble", source = "catalogo.antiguedad.procedenciaMueble")
    @Mapping(target = "precioMxn", source = "catalogo.precioMxnCatalogo")
    @Mapping(target = "dimension", source = "dimension")
    @Mapping(target = "urlFotoPortada", source = "urlFotoPortada")
    CatalogoAntiguedadDetalleResponse toDetalle(CatalogoAntiguedad catalogo, String urlFotoPortada, DimensionResponse dimension);
}