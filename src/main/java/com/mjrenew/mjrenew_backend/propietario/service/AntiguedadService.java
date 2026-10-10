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
// MJRENEW-STRIPE: tipos propios del flujo bancario; no cadenas mágicas.
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.enums.FlujoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.transaccion.dto.StripeCheckoutSession;
import com.mjrenew.mjrenew_backend.transaccion.entity.TransaccionBancaria;
import com.mjrenew.mjrenew_backend.transaccion.repository.TransaccionBancariaRepository;
import com.mjrenew.mjrenew_backend.transaccion.service.StripeCheckoutService;
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
import com.mjrenew.mjrenew_backend.propietario.entity.Dimension;
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
import java.util.List;
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
    // MJRENEW-STRIPE: integración de pago real de restauración.
    private final TransaccionBancariaRepository transaccionRepository;
    private final StripeCheckoutService stripeCheckoutService;

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
            CatalogoAntiguedadMapper catalogoAntiguedadMapper,
            TransaccionBancariaRepository transaccionRepository,
            StripeCheckoutService stripeCheckoutService
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
        this.transaccionRepository = transaccionRepository;
        this.stripeCheckoutService = stripeCheckoutService;
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

        /*
         * Dimensiones: las aporta el propietario aquí, al registrar la
         * pieza (no el restaurador durante la evaluación visual). Solo
         * se crea el registro si vino al menos un valor.
         */
        if (request.altoCmAntiguedad() != null
                || request.anchoCmAntiguedad() != null
                || request.profundidadCmAntiguedad() != null
                || request.pesoKgAntiguedad() != null) {

            Dimension dimension =
                    antiguedadMapper.toDimension(request);

            dimension.setAntiguedad(guardada);

            dimension.setRegistradasEnDimensiones(
                    OffsetDateTime.now()
            );

            dimensionRepository.save(dimension);
        }

        /*
         * Fotos del estado inicial: tampoco hay servicio de almacenamiento
         * (S3/Cloudinary) todavía, así que se reciben URLs externas ya
         * subidas por el propietario, no archivos.
         */
        if (request.urlsFotografiasIniciales() != null) {

            for (String url : request.urlsFotografiasIniciales()) {

                FotografiaAntiguedad foto = new FotografiaAntiguedad();
                foto.setAntiguedad(guardada);
                foto.setTipoFotografia(TipoFotografia.ESTADO_INICIAL);
                foto.setUrlAlmacenFotografia(url);
                foto.setSubidaEnFotografia(OffsetDateTime.now());

                fotografiaRepository.save(foto);
            }
        }

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

    /*
    @Transactional(readOnly = true)
    public AntiguedadDetalleResponse obtenerDetalle(
            UUID antiguedadId
    ) {

        return construirDetalle(
                buscarOFallar(antiguedadId)
        );
    } */

    @Transactional(readOnly = true)
    public AntiguedadDetalleResponse obtenerDetalle(
            UUID antiguedadId,
            UUID propietarioId
    ) {

        return construirDetalle(
                buscarDelPropietarioOFallar(
                        antiguedadId,
                        propietarioId
                )
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
     * MJRENEW-STRIPE: aceptar la cotización SOLO prepara Checkout.
     * PRESUPUESTO_PRESENTADO -> PAGO_EN_ESCROW ocurre únicamente en el webhook.
     * La cotización establece un rango; para el prototipo se cobra su límite
     * superior. Falta aprobar un precio cerrado y el desglose de logística,
     * comisión y costos de Stripe antes de considerarlo listo para producción.
     */
    @Transactional
    public UrlPagoStripeResponse aceptarCotizacion(
            UUID antiguedadId,
            UUID propietarioId
    ) {
        Antiguedad antiguedad = antiguedadRepository.bloquearParaCrearPago(antiguedadId, propietarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La antigüedad no pertenece al propietario"));
        validarEstado(antiguedad, EstadoAntiguedad.PRESUPUESTO_PRESENTADO);
        Cotizacion cotizacion = buscarCotizacionOFallar(antiguedadId);

        if (cotizacion.getEstadoCotizacion() == EstadoCotizacion.RECHAZADA) {
            throw new SolicitudInvalidaException("La cotización ya fue rechazada");
        }
        if (antiguedad.getRestaurador() == null) {
            throw new SolicitudInvalidaException("La antigüedad no tiene restaurador asignado");
        }
        if (cotizacion.getCostoMaximoMxnCotizacion() == null
                || cotizacion.getCostoMaximoMxnCotizacion().signum() <= 0) {
            throw new SolicitudInvalidaException("La cotización no contiene un monto de pago válido");
        }

        // MJRENEW-STRIPE: permite reabrir la misma sesión si ya hay una pendiente;
        // si Stripe informa que expiró, deja constancia y genera otra sesión.
        var anterior = transaccionRepository
                .findFirstByAntiguedad_AntiguedadesIdAndTipoTransaccionAndEstadoTransaccionOrderByCreadaEnTransaccionDesc(
                        antiguedadId, TipoTransaccion.PAGO_RESTAURACION, EstadoTransaccion.PENDIENTE);
        if (anterior.isPresent()) {
            TransaccionBancaria pendiente = anterior.get();
            if (pendiente.getReferenciaStripeTransaccion() == null) {
                throw new SolicitudInvalidaException("Existe un intento de pago todavía en preparación");
            }
            StripeCheckoutSession vigente = stripeCheckoutService
                    .recuperarSesionAbierta(pendiente.getReferenciaStripeTransaccion());
            if (vigente != null) {
                return new UrlPagoStripeResponse(vigente.urlPagoStripe());
            }
            pendiente.setEstadoTransaccion(EstadoTransaccion.FALLIDA);
            pendiente.setProcesadaEnTransaccion(OffsetDateTime.now());
        }

        TransaccionBancaria transaccion = new TransaccionBancaria();
        transaccion.setTipoTransaccion(TipoTransaccion.PAGO_RESTAURACION);
        transaccion.setFlujoMjrenew(FlujoTransaccion.INGRESO);
        transaccion.setOriginator(antiguedad.getPropietario());
        transaccion.setBeneficiario(antiguedad.getRestaurador());
        transaccion.setAntiguedad(antiguedad);
        transaccion.setMontoBrutoMxnTransaccion(cotizacion.getCostoMaximoMxnCotizacion());
        transaccion.setEstadoTransaccion(EstadoTransaccion.PENDIENTE);
        transaccion.setCreadaEnTransaccion(OffsetDateTime.now());
        transaccionRepository.saveAndFlush(transaccion);

        StripeCheckoutSession checkout = stripeCheckoutService.crearSesionPago(transaccion);
        transaccion.setReferenciaStripeTransaccion(checkout.referenciaStripe());
        cotizacion.setEstadoCotizacion(EstadoCotizacion.ACEPTADA);
        cotizacion.setRespondidaEnCotizacion(OffsetDateTime.now());

        // No modificar estadoActualAntiguedad: Stripe aún no ha confirmado pago.
        return new UrlPagoStripeResponse(checkout.urlPagoStripe());
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
        // MJRENEW-STRIPE: no cancelar una cotización con Checkout iniciado:
        // Stripe podría confirmar el cargo mientras se procesa el rechazo.
        if (cotizacion.getEstadoCotizacion() == EstadoCotizacion.ACEPTADA) {
            throw new SolicitudInvalidaException(
                    "La cotización ya fue aceptada. Espera la confirmación del pago");
        }

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
                dimension,
                obtenerFotosEstadoInicial(antiguedad)
        );
    }

    private List<String> obtenerFotosEstadoInicial(Antiguedad antiguedad) {

        return fotografiaRepository
                .findByAntiguedad_AntiguedadesIdAndTipoFotografiaOrderBySubidaEnFotografiaAsc(
                        antiguedad.getAntiguedadesId(),
                        TipoFotografia.ESTADO_INICIAL
                )
                .stream()
                .map(FotografiaAntiguedad::getUrlAlmacenFotografia)
                .toList();
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