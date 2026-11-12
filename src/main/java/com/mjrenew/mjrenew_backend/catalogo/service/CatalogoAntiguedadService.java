package com.mjrenew.mjrenew_backend.catalogo.service;

import com.mjrenew.mjrenew_backend.catalogo.dto.CatalogoAntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.catalogo.dto.CatalogoAntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.catalogo.dto.UrlPagoStripeResponse;
import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import com.mjrenew.mjrenew_backend.catalogo.mapper.CatalogoAntiguedadMapper;
import com.mjrenew.mjrenew_backend.catalogo.repository.CatalogoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoFotografia;
import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.propietario.dto.DimensionResponse;
import com.mjrenew.mjrenew_backend.propietario.repository.DimensionRepository;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.transaccion.dto.StripeCheckoutSession;
import com.mjrenew.mjrenew_backend.transaccion.service.StripeCheckoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CatalogoAntiguedadService {
    private static final Logger log = LoggerFactory.getLogger(CatalogoAntiguedadService.class);

    private final CatalogoAntiguedadRepository catalogoRepository;
    private final FotografiaAntiguedadRepository fotografiaRepository;
    private final DimensionRepository dimensionRepository;
    private final CatalogoAntiguedadMapper catalogoMapper;
    private final StripeCheckoutService stripeCheckoutService;
    private final CatalogoCompraReservaService compraReservaService;


    public CatalogoAntiguedadService(
            CatalogoAntiguedadRepository catalogoRepository,
            FotografiaAntiguedadRepository fotografiaRepository,
            DimensionRepository dimensionRepository,
            CatalogoAntiguedadMapper catalogoMapper,
            StripeCheckoutService stripeCheckoutService,
            CatalogoCompraReservaService compraReservaService
    ) {

        this.catalogoRepository = catalogoRepository;
        this.fotografiaRepository = fotografiaRepository;
        this.dimensionRepository = dimensionRepository;
        this.catalogoMapper = catalogoMapper;
        this.stripeCheckoutService = stripeCheckoutService;
        this.compraReservaService = compraReservaService;
    }


    public Page<CatalogoAntiguedadResumenResponse> explorarCatalogo(
            Pageable pageable
    ) {

        return catalogoRepository
                .findByActivaCatalogoTrue(pageable)
                .map(catalogo ->
                        catalogoMapper.toResumen(
                                catalogo,
                                obtenerUrlFotoPortada(
                                        catalogo.getAntiguedad()
                                )
                        )
                );
    }


    public CatalogoAntiguedadDetalleResponse obtenerDetalle(
            UUID catalogoId, UUID compradorActualId
    ) {

        CatalogoAntiguedad catalogo =
                buscarOFallar(catalogoId, compradorActualId);

        String urlFotoPortada =
                obtenerUrlFotoPortada(
                        catalogo.getAntiguedad()
                );

        DimensionResponse dimension =
                dimensionRepository
                        .findByAntiguedad_AntiguedadesId(
                                catalogo
                                        .getAntiguedad()
                                        .getAntiguedadesId()
                        )
                        .map(dim ->
                                new DimensionResponse(
                                        dim.getAltoCmAntiguedad(),
                                        dim.getAnchoCmAntiguedad(),
                                        dim.getProfundidadCmAntiguedad(),
                                        dim.getPesoKgAntiguedad()
                                )
                        )
                        .orElse(null);

        return catalogoMapper.toDetalle(
                catalogo,
                urlFotoPortada,
                dimension
        );
    }


    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UrlPagoStripeResponse iniciarCompra(UUID catalogoId, Usuario comprador) {
        // La reserva y el registro del pago se confirman antes de llamar a Stripe.
        var reserva = compraReservaService.reservar(catalogoId, comprador);
        StripeCheckoutSession checkout;
        try {
            if (reserva.referenciaStripe() != null) {
                checkout = stripeCheckoutService.recuperarSesionAbierta(reserva.referenciaStripe());
                if (checkout == null) {
                    throw new SolicitudInvalidaException("La sesión de pago venció; espera la liberación de la reserva");
                }
                return new UrlPagoStripeResponse(checkout.urlPagoStripe());
            } else {
                // Reintentos del mismo comprador usan el mismo ID e idempotency key.
                checkout = stripeCheckoutService.crearSesionPago(reserva.pago());
            }
        } catch (SolicitudInvalidaException ex) {
            // Un fallo de red no demuestra que Stripe haya omitido crear la sesión.
            log.warn("Checkout sin respuesta definitiva para catalogoId={} transaccionId={}",
                    catalogoId, reserva.pago().getTransaccionId(), ex);
            throw new CheckoutPendienteException(
                    "No se pudo confirmar el enlace de pago; la reserva permanece protegida. Intenta de nuevo", ex);
        }

        // Segunda transacción corta: el webhook puede reintentar hasta que esta referencia sea visible.
        try {
            compraReservaService.vincularCheckout(
                    reserva.pago().getTransaccionId(), catalogoId, comprador.getUsuariosId(), checkout);
        } catch (RuntimeException ex) {
            log.error("Checkout creado sin vínculo local para catalogoId={} transaccionId={} stripeSessionId={}",
                    catalogoId, reserva.pago().getTransaccionId(), checkout.referenciaStripe(), ex);
            throw new CheckoutPendienteException(
                    "El enlace de pago está pendiente de conciliación. Intenta de nuevo", ex);
        }
        return new UrlPagoStripeResponse(checkout.urlPagoStripe());
    }


    private String obtenerUrlFotoPortada(
            Antiguedad antiguedad
    ) {

        return fotografiaRepository
                .findFirstByAntiguedad_AntiguedadesIdAndTipoFotografiaOrderBySubidaEnFotografiaAsc(
                        antiguedad.getAntiguedadesId(),
                        TipoFotografia.ESTADO_INICIAL
                )
                .map(foto ->
                        foto.getUrlAlmacenFotografia()
                )
                .orElse(null);
    }


    private CatalogoAntiguedad buscarOFallar(
            UUID catalogoId, UUID compradorActualId
    ) {
        CatalogoAntiguedad catalogo = catalogoRepository
                .findById(catalogoId)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "No existe una antigüedad en catálogo con id "
                                        + catalogoId
                        )
                );
        boolean reservaPropia = compradorActualId != null
                && catalogo.getComprador() != null
                && compradorActualId.equals(catalogo.getComprador().getUsuariosId())
                && catalogo.getAntiguedad().getEstadoActualAntiguedad() == EstadoAntiguedad.LINK_PAGO_ENVIADO
                && catalogo.getCompletadaEnCatalogo() == null;
        if (!Boolean.TRUE.equals(catalogo.getActivaCatalogo()) && !reservaPropia) {
            throw new RecursoNoEncontradoException(
                    "No existe una antigüedad disponible en catálogo con id " + catalogoId);
        }
        return catalogo;
    }
}
