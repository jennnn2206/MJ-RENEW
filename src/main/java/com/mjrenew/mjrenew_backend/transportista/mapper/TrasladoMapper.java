package com.mjrenew.mjrenew_backend.transportista.mapper;

import com.mjrenew.mjrenew_backend.transportista.dto.TrasladoAntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.transportista.dto.TrasladoAntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.transportista.entity.TrasladoAntiguedad;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.mjrenew.mjrenew_backend.propietario.dto.RecoleccionPropietarioResponse;
import java.util.List;

@Mapper(componentModel = "spring")
public interface TrasladoMapper {

    @Mapping(target = "trasladoId", source = "traslado.trasladosAntiguedadId")
    @Mapping(target = "numeroIntento", source = "traslado.numeroIntentoTraslado")
    @Mapping(target = "resultado", source = "traslado.resultadoTraslado")
    @Mapping(target = "salidaConfirmadaEn", source = "traslado.salidaConfirmadaEnTraslado")
    @Mapping(target = "entregaConfirmadaEn", source = "traslado.recoleccionConfirmadaEn")
    @Mapping(target = "confirmadaPor", source = "traslado.recoleccionConfirmadaPor.usuariosId")
    RecoleccionPropietarioResponse toRecoleccionPropietario(TrasladoAntiguedad traslado, List<String> fotos);

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
    @Mapping(target = "motivoFallo", source = "motivoFalloTraslado")
    TrasladoAntiguedadDetalleResponse toDetalle(TrasladoAntiguedad traslado);
}
