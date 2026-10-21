package com.mjrenew.mjrenew_backend.comprador.service;

import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import com.mjrenew.mjrenew_backend.catalogo.repository.CatalogoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.comprador.dto.ConfirmarRecepcionCompraResponse;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.ResultadoTraslado;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoFotografia;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTraslado;
import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.exception.TransicionEstadoInvalidaException;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.transportista.entity.TrasladoAntiguedad;
import com.mjrenew.mjrenew_backend.transportista.repository.TrasladoAntiguedadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class CompraService {
    private static final int FOTOS_MINIMAS_ENTREGA = 4;

    private final CatalogoAntiguedadRepository catalogos;
    private final TrasladoAntiguedadRepository traslados;
    private final FotografiaAntiguedadRepository fotografias;

    public CompraService(CatalogoAntiguedadRepository catalogos,
                         TrasladoAntiguedadRepository traslados,
                         FotografiaAntiguedadRepository fotografias) {
        this.catalogos = catalogos;
        this.traslados = traslados;
        this.fotografias = fotografias;
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
