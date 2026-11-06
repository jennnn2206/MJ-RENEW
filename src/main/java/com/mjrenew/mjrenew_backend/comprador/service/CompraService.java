package com.mjrenew.mjrenew_backend.comprador.service;

import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import com.mjrenew.mjrenew_backend.catalogo.repository.CatalogoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.comprador.dto.ConfirmarRecepcionCompraResponse;
import com.mjrenew.mjrenew_backend.comprador.dto.CompraResumenResponse;
import com.mjrenew.mjrenew_backend.comprador.mapper.CompraMapper;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.ResultadoTraslado;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoFotografia;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTraslado;
import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.exception.TransicionEstadoInvalidaException;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.transportista.entity.TrasladoAntiguedad;
import com.mjrenew.mjrenew_backend.transportista.repository.TrasladoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.propietario.entity.FotografiaAntiguedad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CompraService {
    private static final int FOTOS_MINIMAS_ENTREGA = 4;

    private final CatalogoAntiguedadRepository catalogos;
    private final TrasladoAntiguedadRepository traslados;
    private final FotografiaAntiguedadRepository fotografias;
    private final CompraMapper mapper;

    public CompraService(CatalogoAntiguedadRepository catalogos,
                         TrasladoAntiguedadRepository traslados,
                         FotografiaAntiguedadRepository fotografias,
                         CompraMapper mapper) {
        this.catalogos = catalogos;
        this.traslados = traslados;
        this.fotografias = fotografias;
        this.mapper = mapper;
    }

    /**
     * Solo ventas pagadas mediante el webhook de Stripe. Consultas por página/lote
     * para evitar una consulta por compra (problema N+1).
     */
    @Transactional(readOnly = true)
    public Page<CompraResumenResponse> obtenerMisCompras(UUID compradorId, Pageable pageable) {
        Page<CatalogoAntiguedad> pagina = catalogos
                .findByComprador_UsuariosIdAndCompletadaEnCatalogoIsNotNull(compradorId, pageable);
        if (pagina.isEmpty()) {
            return pagina.map(c -> mapper.toResumen(c, null, null, false));
        }

        Set<UUID> idsAntiguedades = new HashSet<>();
        for (CatalogoAntiguedad catalogo : pagina.getContent()) {
            idsAntiguedades.add(catalogo.getAntiguedad().getAntiguedadesId());
        }

        Map<UUID, TrasladoAntiguedad> ultimaEntregaPorAntiguedad = new HashMap<>();
        for (TrasladoAntiguedad traslado : traslados
                .findByAntiguedad_AntiguedadesIdInAndTipoTrasladoOrderByAgendadoEnTrasladoDesc(
                        idsAntiguedades, TipoTraslado.ENTREGA_COMPRADOR)) {
            // Los traslados vienen del más reciente al más antiguo.
            ultimaEntregaPorAntiguedad.putIfAbsent(
                    traslado.getAntiguedad().getAntiguedadesId(), traslado);
        }

        Map<UUID, String> portadas = new HashMap<>();
        for (FotografiaAntiguedad foto : fotografias
                .findByAntiguedad_AntiguedadesIdInAndTipoFotografiaOrderBySubidaEnFotografiaAsc(
                        idsAntiguedades, TipoFotografia.ESTADO_INICIAL)) {
            portadas.putIfAbsent(foto.getAntiguedad().getAntiguedadesId(), foto.getUrlAlmacenFotografia());
        }

        Set<UUID> entregasExitosas = new HashSet<>();
        for (TrasladoAntiguedad entrega : ultimaEntregaPorAntiguedad.values()) {
            if (entrega.getResultadoTraslado() == ResultadoTraslado.EXITOSO) {
                entregasExitosas.add(entrega.getTrasladosAntiguedadId());
            }
        }

        Map<UUID, Long> fotosPorEntrega = new HashMap<>();
        if (!entregasExitosas.isEmpty()) {
            for (FotografiaAntiguedadRepository.EvidenciasTraslado conteo :
                    fotografias.contarEvidenciasPorTraslado(entregasExitosas, TipoFotografia.TRASLADO_LLEGADA)) {
                fotosPorEntrega.put(conteo.getTrasladoId(), conteo.getTotal());
            }
        }

        return pagina.map(catalogo -> {
            UUID antiguedadId = catalogo.getAntiguedad().getAntiguedadesId();
            TrasladoAntiguedad entrega = ultimaEntregaPorAntiguedad.get(antiguedadId);
            boolean habilitarAcuse = catalogo.getRecepcionConfirmadaEnCatalogo() == null
                    && entrega != null
                    && entrega.getResultadoTraslado() == ResultadoTraslado.EXITOSO
                    && fotosPorEntrega.getOrDefault(entrega.getTrasladosAntiguedadId(), 0L) >= FOTOS_MINIMAS_ENTREGA;
            return mapper.toResumen(catalogo, entrega, portadas.get(antiguedadId), habilitarAcuse);
        });
    }

    /** Únicamente acuse del comprador: no llama Stripe ni crea LIBERACION_* . */
    @Transactional
    public ConfirmarRecepcionCompraResponse confirmarRecepcion(UUID catalogoId, UUID compradorId) {
        // Bloqueo pesimista: impide que dos clics concurrentes escriban dos confirmaciones.
        CatalogoAntiguedad catalogo = catalogos.buscarParaCompra(catalogoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe esta compra"));

        if (catalogo.getComprador() == null
                || !catalogo.getComprador().getUsuariosId().equals(compradorId)) {
            // No filtrar datos de compras ajenas.
            throw new RecursoNoEncontradoException("No existe esta compra para el comprador autenticado");
        }

        if (catalogo.getAntiguedad().getEstadoActualAntiguedad() != EstadoAntiguedad.VENTA_COMPLETADA
                || catalogo.getCompletadaEnCatalogo() == null) {
            throw new TransicionEstadoInvalidaException("La compra todavía no tiene pago confirmado");
        }

        UUID antiguedadId = catalogo.getAntiguedad().getAntiguedadesId();
        TrasladoAntiguedad traslado = traslados
                .findFirstByAntiguedad_AntiguedadesIdAndTipoTrasladoAndResultadoTrasladoOrderByEjecutadoEnTrasladoDesc(
                        antiguedadId, TipoTraslado.ENTREGA_COMPRADOR, ResultadoTraslado.EXITOSO)
                .orElseThrow(() -> new TransicionEstadoInvalidaException(
                        "El transportista todavía no ha confirmado la entrega al comprador"));

        long numeroFotos = fotografias.countByTraslado_TrasladosAntiguedadIdAndTipoFotografia(
                traslado.getTrasladosAntiguedadId(), TipoFotografia.TRASLADO_LLEGADA);
        if (numeroFotos < FOTOS_MINIMAS_ENTREGA) {
            throw new TransicionEstadoInvalidaException("La entrega no tiene las cuatro fotografías exigidas");
        }

        // Reintentar la petición produce el mismo acuse, no un segundo evento.
        if (catalogo.getRecepcionConfirmadaEnCatalogo() == null) {
            catalogo.setRecepcionConfirmadaEnCatalogo(OffsetDateTime.now());
        }

        return new ConfirmarRecepcionCompraResponse(catalogoId,
                traslado.getTrasladosAntiguedadId(), catalogo.getRecepcionConfirmadaEnCatalogo());
    }
}
