package com.mjrenew.mjrenew_backend.propietario.mapper;

import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadCreateRequest;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.propietario.dto.DimensionResponse;
import com.mjrenew.mjrenew_backend.propietario.entity.Dimension;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapeo DTO <-> entidad generado por MapStruct. urlFotoPortada y dimension no
 * son propiedades de Antiguedad (viven en tablas separadas), así que se
 * reciben como parámetros aparte en vez de intentar sacarlos del entity.
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
    @Mapping(target = "motivoRechazoEvaluacion", ignore = true)
    Antiguedad toEntity(AntiguedadCreateRequest request);

    @Mapping(target = "antiguedadId", source = "antiguedad.antiguedadesId")
    @Mapping(target = "nombreRestaurador", source = "antiguedad.restaurador.nombreCompletoUsuario")
    @Mapping(target = "urlFotoPortada", source = "urlFotoPortada")
    AntiguedadResumenResponse toResumen(Antiguedad antiguedad, String urlFotoPortada);

    @Mapping(target = "antiguedadId", source = "antiguedad.antiguedadesId")
    @Mapping(target = "nombreCompletoRestaurador", source = "antiguedad.restaurador.nombreCompletoUsuario")
    @Mapping(target = "dimension", source = "dimension")
    @Mapping(target = "fotosEstadoInicial", source = "fotosEstadoInicial")
    AntiguedadDetalleResponse toDetalle(Antiguedad antiguedad, DimensionResponse dimension, List<String> fotosEstadoInicial);

    @Mapping(target = "altoCm", source = "altoCmAntiguedad")
    @Mapping(target = "anchoCm", source = "anchoCmAntiguedad")
    @Mapping(target = "profundidadCm", source = "profundidadCmAntiguedad")
    @Mapping(target = "pesoKg", source = "pesoKgAntiguedad")
    DimensionResponse toDimensionResponse(Dimension dimension);

    /*
     * ============================================================
     * REGISTRO -> DIMENSIONES
     * ============================================================
     *
     * A diferencia de la evaluación del restaurador (que ya no mide la
     * pieza), las dimensiones ahora se capturan aquí, al momento en que
     * el propietario registra su antigüedad. Los nombres de campo son
     * idénticos entre AntiguedadCreateRequest y Dimension, así que
     * MapStruct los mapea automáticamente sin @Mapping explícito.
     *
     * - dimensionesId -> generado por JPA
     * - antiguedad -> antigüedad recién creada (se asigna en el service)
     * - registradasEnDimensiones -> fecha del servidor
     */

    @Mapping(target = "dimensionesId", ignore = true)
    @Mapping(target = "antiguedad", ignore = true)
    @Mapping(target = "registradasEnDimensiones", ignore = true)
    Dimension toDimension(AntiguedadCreateRequest request);
}