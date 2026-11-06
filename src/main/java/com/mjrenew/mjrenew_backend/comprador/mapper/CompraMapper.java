package com.mjrenew.mjrenew_backend.comprador.mapper;

import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import com.mjrenew.mjrenew_backend.comprador.dto.CompraResumenResponse;
import com.mjrenew.mjrenew_backend.transportista.entity.TrasladoAntiguedad;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** MapStruct aprovecha la entidad y el traslado real; no hay campos simulados. */
@Mapper(componentModel = "spring")
public interface CompraMapper {

    @Mapping(target = "catalogoId", source = "catalogo.catalogoAntiguedadId")
    @Mapping(target = "tipoMueble", source = "catalogo.antiguedad.tipoMueble")
    @Mapping(target = "precioMxn", source = "catalogo.precioMxnCatalogo")
    @Mapping(target = "urlFotoPortada", source = "urlFotoPortada")
    @Mapping(target = "pagadaEn", source = "catalogo.completadaEnCatalogo")
    @Mapping(target = "resultadoEntrega", source = "entrega.resultadoTraslado")
    @Mapping(target = "fechaEntregaAcordada", source = "entrega.fechaAcordadaTraslado")
    @Mapping(target = "salidaConfirmadaEn", source = "entrega.salidaConfirmadaEnTraslado")
    @Mapping(target = "entregaRegistradaEn", source = "entrega.ejecutadoEnTraslado")
    @Mapping(target = "recepcionConfirmadaEn", source = "catalogo.recepcionConfirmadaEnCatalogo")
    @Mapping(target = "puedeConfirmarRecepcion", source = "puedeConfirmarRecepcion")
    CompraResumenResponse toResumen(CatalogoAntiguedad catalogo,
                                    TrasladoAntiguedad entrega,
                                    String urlFotoPortada,
                                    boolean puedeConfirmarRecepcion);
}
