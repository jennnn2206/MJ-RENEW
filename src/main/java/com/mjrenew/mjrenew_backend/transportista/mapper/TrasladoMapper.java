package com.mjrenew.mjrenew_backend.transportista.mapper;

import com.mjrenew.mjrenew_backend.transportista.dto.TrasladoAntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.transportista.dto.TrasladoAntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.transportista.entity.TrasladoAntiguedad;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TrasladoMapper {

    @Mapping(target = "trasladoId", source = "trasladosAntiguedadId")
    @Mapping(target = "antiguedadId", source = "antiguedad.antiguedadesId")
    @Mapping(target = "fechaAcordada", source = "fechaAcordadaTraslado")
    @Mapping(target = "horaInicio", source = "horaInicioAcordadaTraslado")
    @Mapping(target = "horaFin", source = "horaFinAcordadaTraslado")
    @Mapping(target = "resultadoTraslado", source = "resultadoTraslado")
    TrasladoAntiguedadResumenResponse toResumen(TrasladoAntiguedad traslado);

    @Mapping(target = "trasladoId", source = "trasladosAntiguedadId")
    @Mapping(target = "antiguedadId", source = "antiguedad.antiguedadesId")
    @Mapping(target = "direccionOrigen", source = "direccionOrigenTraslado")
    @Mapping(target = "direccionDestino", source = "direccionDestinoTraslado")
    @Mapping(target = "fechaAcordada", source = "fechaAcordadaTraslado")
    @Mapping(target = "horaInicio", source = "horaInicioAcordadaTraslado")
    @Mapping(target = "horaFin", source = "horaFinAcordadaTraslado")
    @Mapping(target = "numeroIntento", source = "numeroIntentoTraslado")
    @Mapping(target = "costoEstimado", source = "costoEstimadoMxnTraslado")
    @Mapping(target = "salidaConfirmadaEn", source = "salidaConfirmadaEnTraslado")
    @Mapping(target = "ejecutadoEn", source = "ejecutadoEnTraslado")
    TrasladoAntiguedadDetalleResponse toDetalle(TrasladoAntiguedad traslado);
}