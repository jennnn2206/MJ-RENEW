package com.mjrenew.mjrenew_backend.propietario.service;

import com.mjrenew.mjrenew_backend.catalogo.dto.CatalogoAntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.catalogo.dto.UrlPagoStripeResponse;
import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import com.mjrenew.mjrenew_backend.catalogo.mapper.CatalogoAntiguedadMapper;
import com.mjrenew.mjrenew_backend.catalogo.repository.CatalogoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository.AntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.enums.DisponibilidadRestaurador;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoCotizacion;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoFotografia;
import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.exception.TransicionEstadoInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadCreateRequest;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.propietario.dto.DimensionResponse;
import com.mjrenew.mjrenew_backend.propietario.dto.PublicarEnCatalogoRequest;
import com.mjrenew.mjrenew_backend.propietario.dto.RechazarCotizacionRequest;
import com.mjrenew.mjrenew_backend.propietario.dto.SeleccionarRestauradorRequest;
import com.mjrenew.mjrenew_backend.propietario.entity.FotografiaAntiguedad;
import com.mjrenew.mjrenew_backend.propietario.mapper.AntiguedadMapper;
import com.mjrenew.mjrenew_backend.propietario.repository.DimensionRepository;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.restaurador.dto.AvanceRestauracionResumenResponse;
import com.mjrenew.mjrenew_backend.restaurador.dto.CotizacionResumenResponse;
import com.mjrenew.mjrenew_backend.restaurador.entity.AvanceRestauracion;
import com.mjrenew.mjrenew_backend.restaurador.entity.Cotizacion;
import com.mjrenew.mjrenew_backend.restaurador.entity.PerfilRestaurador;
import com.mjrenew.mjrenew_backend.restaurador.mapper.RestauradorMapper;
import com.mjrenew.mjrenew_backend.restaurador.repository.AvanceRestauracionRepository;
import com.mjrenew.mjrenew_backend.restaurador.repository.CotizacionRepository;
import com.mjrenew.mjrenew_backend.restaurador.repository.PerfilRestauradorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AntiguedadService {

    private final AntiguedadRepository antiguedadRepository;
    private final FotografiaAntiguedadRepository fotografiaRepository;
    private final DimensionRepository dimensionRepository;
    private final PerfilRestauradorRepository perfilRestauradorRepository;
    private final AntiguedadMapper antiguedadMapper;
    private final CotizacionRepository cotizacionRepository;
    private final RestauradorMapper restauradorMapper;
    private final AvanceRestauracionRepository avanceRestauracionRepository;
    private final CatalogoAntiguedadRepository catalogoAntiguedadRepository;
    private final CatalogoAntiguedadMapper catalogoAntiguedadMapper;

    public AntiguedadService(
            AntiguedadRepository antiguedadRepository,
            FotografiaAntiguedadRepository fotografiaRepository,
            DimensionRepository dimensionRepository,
            PerfilRestauradorRepository perfilRestauradorRepository,
            AntiguedadMapper antiguedadMapper,
            CotizacionRepository cotizacionRepository,
            RestauradorMapper restauradorMapper,
            AvanceRestauracionRepository avanceRestauracionRepository,
            CatalogoAntiguedadRepository catalogoAntiguedadRepository,
            CatalogoAntiguedadMapper catalogoAntiguedadMapper
    ) {
        this.antiguedadRepository = antiguedadRepository;
        this.fotografiaRepository = fotografiaRepository;
        this.dimensionRepository = dimensionRepository;
        this.perfilRestauradorRepository = perfilRestauradorRepository;
        this.antiguedadMapper = antiguedadMapper;
        this.cotizacionRepository = cotizacionRepository;
        this.restauradorMapper = restauradorMapper;
        this.avanceRestauracionRepository = avanceRestauracionRepository;
        this.catalogoAntiguedadRepository = catalogoAntiguedadRepository;
        this.catalogoAntiguedadMapper = catalogoAntiguedadMapper;
    }

    @Transactional
    public AntiguedadDetalleResponse crear(
            AntiguedadCreateRequest request,
            Usuario propietario
    ) {

        Antiguedad antiguedad =
                antiguedadMapper.toEntity(request);

        antiguedad.setPropietario(propietario);

        antiguedad.setEstadoActualAntiguedad(
                EstadoAntiguedad.PIEZA_CAPTURADA
        );

        antiguedad.setRegistradaEnAntiguedad(
                OffsetDateTime.now()
        );

        Antiguedad guardada =
                antiguedadRepository.save(antiguedad);

        return construirDetalle(guardada);
    }

    @Transactional(readOnly = true)
    public Page<AntiguedadResumenResponse> listarMias(
            UUID propietarioId,
            Pageable pageable
    ) {

        return antiguedadRepository
                .findByPropietario_UsuariosId(
                        propietarioId,
                        pageable
                )
                .map(this::construirResumen);
    }

    @Transactional(readOnly = true)
    public AntiguedadDetalleResponse obtenerDetalle(
            UUID antiguedadId
    ) {

        return construirDetalle(
                buscarOFallar(antiguedadId)
        );
    }

    @Transactional
    public AntiguedadDetalleResponse seleccionarRestaurador(
            UUID antiguedadId,
            UUID propietarioId,
            SeleccionarRestauradorRequest request
    ) {

        Antiguedad antiguedad =
                buscarDelPropietarioOFallar(
                        antiguedadId,
                        propietarioId
                );

        if (antiguedad.getEstadoActualAntiguedad()
                != EstadoAntiguedad.PIEZA_CAPTURADA) {

            throw new TransicionEstadoInvalidaException(
                    "La antigüedad debe estar en estado PIEZA_CAPTURADA para seleccionar un restaurador"
            );
        }

        if (antiguedad.getRestaurador() != null) {

            throw new TransicionEstadoInvalidaException(
                    "La antigüedad ya tiene un restaurador asignado"
            );
        }

        PerfilRestaurador perfilRestaurador =
                perfilRestauradorRepository
                        .findByRestaurador_UsuariosIdAndDisponibilidadRestauradorAndAprobadoPorAdminRestauradorTrueAndRestaurador_ActivoUsuarioTrue(
                                request.restauradorId(),
                                DisponibilidadRestaurador.DISPONIBLE
                        )
                        .orElseThrow(() ->
                                new RecursoNoEncontradoException(
                                        "El restaurador seleccionado no está disponible"
                                )
                        );

        antiguedad.setRestaurador(
                perfilRestaurador.getRestaurador()
        );

        antiguedad.setEstadoActualAntiguedad(
                EstadoAntiguedad.EN_EVALUACION
        );

        return construirDetalle(antiguedad);
    }

    /*
     * ============================================================
     * OBTENER COTIZACIÓN
     * ============================================================
     */

    @Transactional(readOnly = true)
    public CotizacionResumenResponse obtenerCotizacion(
            UUID antiguedadId,
            UUID propietarioId
    ) {

        buscarDelPropietarioOFallar(antiguedadId, propietarioId);

        Cotizacion cotizacion = buscarCotizacionOFallar(antiguedadId);

        return restauradorMapper.toCotizacionResumen(cotizacion);
    }

    /*
     * ============================================================
     * ACEPTAR COTIZACIÓN
     * ============================================================
     *
     * PRESUPUESTO_PRESENTADO
     *      ↓
     * PAGO_EN_ESCROW
     *
     * Mock: se omite la integración real con Stripe. Solo se cambia
     * el estado de la cotización/antigüedad y se devuelve una URL
     * de pago simulada.
     */

    @Transactional
    public UrlPagoStripeResponse aceptarCotizacion(
            UUID antiguedadId,
            UUID propietarioId
    ) {

        Antiguedad antiguedad =
                buscarDelPropietarioOFallar(antiguedadId, propietarioId);

        validarEstado(
                antiguedad,
                EstadoAntiguedad.PRESUPUESTO_PRESENTADO
        );

        Cotizacion cotizacion = buscarCotizacionOFallar(antiguedadId);

        cotizacion.setEstadoCotizacion(EstadoCotizacion.ACEPTADA);
        cotizacion.setRespondidaEnCotizacion(OffsetDateTime.now());

        antiguedad.setEstadoActualAntiguedad(
                EstadoAntiguedad.PAGO_EN_ESCROW
        );

        // TODO: reemplazar por la integración real con Stripe Payment Links.
        String idPagoMock = "mock_" + UUID.randomUUID();
        String urlPago = "https://checkout.stripe.com/mock/" + idPagoMock;

        return new UrlPagoStripeResponse(urlPago);
    }

    /*
     * ============================================================
     * RECHAZAR COTIZACIÓN
     * ============================================================
     *
     * PRESUPUESTO_PRESENTADO
     *      ↓
     * CANCELADA_DUENO_DECLINO
     */

    @Transactional
    public AntiguedadDetalleResponse rechazarCotizacion(
            UUID antiguedadId,
            UUID propietarioId,
            RechazarCotizacionRequest request
    ) {

        Antiguedad antiguedad =
                buscarDelPropietarioOFallar(antiguedadId, propietarioId);

        validarEstado(
                antiguedad,
                EstadoAntiguedad.PRESUPUESTO_PRESENTADO
        );

        Cotizacion cotizacion = buscarCotizacionOFallar(antiguedadId);

        cotizacion.setEstadoCotizacion(EstadoCotizacion.RECHAZADA);
        cotizacion.setMotivoRechazoCotizacion(
                request.motivoRechazoCotizacion()
        );
        cotizacion.setRespondidaEnCotizacion(OffsetDateTime.now());

        antiguedad.setEstadoActualAntiguedad(
                EstadoAntiguedad.CANCELADA_DUENO_DECLINO
        );

        return construirDetalle(antiguedad);
    }

    /*
     * ============================================================
     * OBTENER AVANCES DE RESTAURACIÓN
     * ============================================================
     */

    @Transactional(readOnly = true)
    public Page<AvanceRestauracionResumenResponse> obtenerAvancesRestauracion(
            UUID antiguedadId,
            UUID propietarioId,
            Pageable pageable
    ) {

        buscarDelPropietarioOFallar(antiguedadId, propietarioId);

        return avanceRestauracionRepository
                .findByAntiguedad_AntiguedadesIdOrderByPublicadoEnAvanceDesc(
                        antiguedadId,
                        pageable
                )
                .map(this::construirAvanceResumen);
    }

    /*
     * ============================================================
     * PUBLICAR EN CATÁLOGO
     * ============================================================
     *
     * ENTREGADO_AL_PROPIETARIO
     *      ↓
     * EN_CATALOGO
     */

    @Transactional
    public CatalogoAntiguedadResumenResponse publicarEnCatalogo(
            UUID antiguedadId,
            UUID propietarioId,
            PublicarEnCatalogoRequest request
    ) {

        Antiguedad antiguedad =
                buscarDelPropietarioOFallar(antiguedadId, propietarioId);

        validarEstado(
                antiguedad,
                EstadoAntiguedad.ENTREGADO_AL_PROPIETARIO
        );

        if (catalogoAntiguedadRepository
                .existsByAntiguedad_AntiguedadesId(antiguedadId)) {

            throw new TransicionEstadoInvalidaException(
                    "La antigüedad ya fue publicada en el catálogo anteriormente"
            );
        }

        CatalogoAntiguedad catalogo = new CatalogoAntiguedad();
        catalogo.setAntiguedad(antiguedad);
        catalogo.setPrecioMxnCatalogo(request.precioMxnCatalogo());
        catalogo.setPublicadaEnCatalogo(OffsetDateTime.now());
        catalogo.setExpiraEnCatalogo(OffsetDateTime.now().plusDays(90));
        catalogo.setActivaCatalogo(true);

        CatalogoAntiguedad guardado =
                catalogoAntiguedadRepository.save(catalogo);

        antiguedad.setEstadoActualAntiguedad(
                EstadoAntiguedad.EN_CATALOGO
        );

        return catalogoAntiguedadMapper.toResumen(
                guardado,
                obtenerUrlFotoPortada(antiguedad)
        );
    }

    private AntiguedadResumenResponse construirResumen(
            Antiguedad antiguedad
    ) {

        String urlFotoPortada = fotografiaRepository
                .findFirstByAntiguedad_AntiguedadesIdAndTipoFotografiaOrderBySubidaEnFotografiaAsc(
                        antiguedad.getAntiguedadesId(),
                        TipoFotografia.ESTADO_INICIAL
                )
                .map(foto ->
                        foto.getUrlAlmacenFotografia()
                )
                .orElse(null);

        return antiguedadMapper.toResumen(
                antiguedad,
                urlFotoPortada
        );
    }

    private AntiguedadDetalleResponse construirDetalle(
            Antiguedad antiguedad
    ) {

        DimensionResponse dimension = dimensionRepository
                .findByAntiguedad_AntiguedadesId(
                        antiguedad.getAntiguedadesId()
                )
                .map(
                        antiguedadMapper::toDimensionResponse
                )
                .orElse(null);

        return antiguedadMapper.toDetalle(
                antiguedad,
                dimension
        );
    }

    private Antiguedad buscarOFallar(
            UUID antiguedadId
    ) {

        return antiguedadRepository
                .findById(antiguedadId)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "No existe una antigüedad con id "
                                        + antiguedadId
                        )
                );
    }

    private Antiguedad buscarDelPropietarioOFallar(
            UUID antiguedadId,
            UUID propietarioId
    ) {

        return antiguedadRepository
                .findByAntiguedadesIdAndPropietario_UsuariosId(
                        antiguedadId,
                        propietarioId
                )
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "No existe la antigüedad solicitada para el propietario autenticado"
                        )
                );
    }

    private Cotizacion buscarCotizacionOFallar(UUID antiguedadId) {

        return cotizacionRepository
                .findByAntiguedad_AntiguedadesId(antiguedadId)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "La antigüedad no tiene una cotización registrada"
                        )
                );
    }

    private AvanceRestauracionResumenResponse construirAvanceResumen(
            AvanceRestauracion avance
    ) {

        var fotos = fotografiaRepository
                .findByAvance_AvancesRestauracionId(
                        avance.getAvancesRestauracionId()
                )
                .stream()
                .map(FotografiaAntiguedad::getUrlAlmacenFotografia)
                .toList();

        return restauradorMapper.toAvanceResumen(avance, fotos);
    }

    private String obtenerUrlFotoPortada(Antiguedad antiguedad) {

        return fotografiaRepository
                .findFirstByAntiguedad_AntiguedadesIdAndTipoFotografiaOrderBySubidaEnFotografiaAsc(
                        antiguedad.getAntiguedadesId(),
                        TipoFotografia.ESTADO_INICIAL
                )
                .map(FotografiaAntiguedad::getUrlAlmacenFotografia)
                .orElse(null);
    }

    private void validarEstado(
            Antiguedad antiguedad,
            EstadoAntiguedad estadoEsperado
    ) {

        if (antiguedad.getEstadoActualAntiguedad() != estadoEsperado) {

            throw new TransicionEstadoInvalidaException(
                    "La antigüedad debe estar en estado "
                            + estadoEsperado.name()
                            + " para esta acción, y está en "
                            + antiguedad.getEstadoActualAntiguedad()
            );
        }
    }
}