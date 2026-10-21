package com.mjrenew.mjrenew_backend.transportista.service;

import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import com.mjrenew.mjrenew_backend.catalogo.repository.CatalogoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.almacenamiento.AlmacenamientoService;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository.AntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.ResultadoTraslado;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoFotografia;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTraslado;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoUsuario;
import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.exception.TransicionEstadoInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.nucleo.usuario.repository.UsuarioRepository;
import com.mjrenew.mjrenew_backend.propietario.entity.FotografiaAntiguedad;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.transportista.dto.ConfirmarLlegadaRequest;
import com.mjrenew.mjrenew_backend.transportista.dto.ConfirmarSalidaRequest;
import com.mjrenew.mjrenew_backend.transportista.dto.CrearTrasladoRequest;
import com.mjrenew.mjrenew_backend.transportista.dto.RegistrarIntentoFallidoRequest;
import com.mjrenew.mjrenew_backend.transportista.dto.TrasladoAntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.transportista.dto.TrasladoAntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.transportista.entity.TrasladoAntiguedad;
import com.mjrenew.mjrenew_backend.transportista.mapper.TrasladoMapper;
import com.mjrenew.mjrenew_backend.transportista.repository.TrasladoAntiguedadRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TrasladoAntiguedadService {

    //Cantidad de evidencias fotograficas minimas
    private static final int FOTOS_MINIMAS = 4;

    private static final Set<String> TIPOS_FOTO_PERMITIDOS = Set.of("image/jpeg", "image/png", "image/webp");

    private final TrasladoAntiguedadRepository trasladoRepository;
    private final AntiguedadRepository antiguedadRepository;
    private final CatalogoAntiguedadRepository catalogoRepository;
    private final FotografiaAntiguedadRepository fotografiaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TrasladoMapper trasladoMapper;
    private final AlmacenamientoService almacenamientoService;

    public TrasladoAntiguedadService(TrasladoAntiguedadRepository trasladoRepository,
                                     AntiguedadRepository antiguedadRepository,
                                     CatalogoAntiguedadRepository catalogoRepository,
                                     FotografiaAntiguedadRepository fotografiaRepository,
                                     UsuarioRepository usuarioRepository,
                                     TrasladoMapper trasladoMapper,
                                     AlmacenamientoService almacenamientoService) {
        this.trasladoRepository = trasladoRepository;
        this.antiguedadRepository = antiguedadRepository;
        this.catalogoRepository = catalogoRepository;
        this.fotografiaRepository = fotografiaRepository;
        this.usuarioRepository = usuarioRepository;
        this.trasladoMapper = trasladoMapper;
        this.almacenamientoService = almacenamientoService;
    }

    /*
     * ============================================================
     * CREAR TRASLADO (agendar recolección o entrega al propietario)
     * ============================================================
     *
     * Acción de logística/despacho: no hay un catálogo de disponibilidad de
     * transportistas (a diferencia de PerfilRestaurador), así que quien
     * coordina y asigna el transportista es un administrador.
     *
     * RECOLECCION:         PAGO_EN_ESCROW      -> HORARIO_RECOLECCION_AGENDADO
     * ENTREGA_PROPIETARIO: RESTAURACION_LISTA  -> HORARIO_ENTREGA_AGENDADO
     *
     * ENTREGA_COMPRADOR: VENTA_COMPLETADA -> VENTA_COMPLETADA.
     * La venta conserva su estado terminal; el seguimiento se guarda en traslado.
     *
     * Si un intento anterior del mismo tipo terminó en NADIE_EN_CASA, se
     * permite agendar otro y se numera como el siguiente intento.
     */
    @Transactional
    public TrasladoAntiguedadDetalleResponse crearTraslado(CrearTrasladoRequest request) {

        Antiguedad antiguedad = antiguedadRepository.findById(request.antiguedadId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una antigüedad con id " + request.antiguedadId()));

        Usuario transportista = usuarioRepository.findById(request.transportistaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un usuario con id " + request.transportistaId()));

        if (transportista.getTipoUsuario() != TipoUsuario.TRANSPORTISTA
                || !Boolean.TRUE.equals(transportista.getActivoUsuario())) {

            throw new SolicitudInvalidaException(
                    "El usuario seleccionado no es un transportista activo");
        }

        if (trasladoRepository.existsByAntiguedad_AntiguedadesIdAndTipoTrasladoAndResultadoTrasladoIn(
                request.antiguedadId(), request.tipoTraslado(),
                List.of(ResultadoTraslado.PENDIENTE, ResultadoTraslado.EXITOSO))) {

            throw new SolicitudInvalidaException(
                    "Ya existe un traslado de tipo " + request.tipoTraslado() + " pendiente o completado para esta antigüedad");
        }

        short numeroIntento = (short) (trasladoRepository.countByAntiguedad_AntiguedadesIdAndTipoTraslado(
                request.antiguedadId(), request.tipoTraslado()) + 1);

        EstadoAntiguedad estadoDestino = validarOrigenYObtenerDestino(antiguedad, request.tipoTraslado());

        TrasladoAntiguedad traslado = new TrasladoAntiguedad();
        traslado.setAntiguedad(antiguedad);
        traslado.setTransportista(transportista);
        traslado.setTipoTraslado(request.tipoTraslado());
        traslado.setDireccionOrigenTraslado(request.direccionOrigenTraslado());
        traslado.setDireccionDestinoTraslado(request.direccionDestinoTraslado());
        traslado.setFechaAcordadaTraslado(request.fechaAcordadaTraslado());
        traslado.setHoraInicioAcordadaTraslado(request.horaInicioAcordadaTraslado());
        traslado.setHoraFinAcordadaTraslado(request.horaFinAcordadaTraslado());
        traslado.setCostoEstimadoMxnTraslado(request.costoEstimadoMxnTraslado());
        traslado.setNumeroIntentoTraslado(numeroIntento);
        traslado.setResultadoTraslado(ResultadoTraslado.PENDIENTE);
        traslado.setAgendadoEnTraslado(OffsetDateTime.now());

        TrasladoAntiguedad guardado = trasladoRepository.save(traslado);

        antiguedad.setEstadoActualAntiguedad(estadoDestino);
        antiguedadRepository.save(antiguedad);

        return trasladoMapper.toDetalle(guardado);
    }

    private EstadoAntiguedad validarOrigenYObtenerDestino(Antiguedad antiguedad, TipoTraslado tipoTraslado) {

        EstadoAntiguedad actual = antiguedad.getEstadoActualAntiguedad();

        return switch (tipoTraslado) {

            case RECOLECCION -> {
                if (actual != EstadoAntiguedad.PAGO_EN_ESCROW) {
                    throw new TransicionEstadoInvalidaException(
                            "La antigüedad debe estar en PAGO_EN_ESCROW para agendar la recolección, y está en " + actual);
                }
                yield EstadoAntiguedad.HORARIO_RECOLECCION_AGENDADO;
            }

            case ENTREGA_PROPIETARIO -> {
                if (actual != EstadoAntiguedad.RESTAURACION_LISTA) {
                    throw new TransicionEstadoInvalidaException(
                            "La antigüedad debe estar en RESTAURACION_LISTA para agendar la entrega al propietario, y está en " + actual);
                }
                yield EstadoAntiguedad.HORARIO_ENTREGA_AGENDADO;
            }

            case ENTREGA_COMPRADOR -> {
                validarVentaPagada(antiguedad);
                yield EstadoAntiguedad.VENTA_COMPLETADA; // No cambiar el AFD terminal.
            }
        };
    }

    public Page<TrasladoAntiguedadResumenResponse> listarMisTraslados(UUID transportistaId, Pageable pageable) {
        return trasladoRepository.findByTransportista_UsuariosId(transportistaId, pageable)
                .map(trasladoMapper::toResumen);
    }

    public TrasladoAntiguedadDetalleResponse obtenerDetalle(UUID trasladoId, UUID transportistaId) {
        return trasladoMapper.toDetalle(buscarAsignadoOFallar(trasladoId, transportistaId));
    }

    @Transactional
    public TrasladoAntiguedadResumenResponse confirmarSalidaRecoleccion(UUID trasladoId, UUID transportistaId, List<MultipartFile> fotos) {
        TrasladoAntiguedad traslado = buscarAsignadoOFallar(trasladoId, transportistaId);
        validarTipo(traslado, TipoTraslado.RECOLECCION);
        validarEstadoOrigen(traslado.getAntiguedad(), EstadoAntiguedad.HORARIO_RECOLECCION_AGENDADO);

        guardarFotos(traslado, fotos, TipoFotografia.TRASLADO_SALIDA);
        traslado.setSalidaConfirmadaEnTraslado(OffsetDateTime.now());
        traslado.getAntiguedad().setEstadoActualAntiguedad(EstadoAntiguedad.EN_RECOLECCION);

        return guardarYResponder(traslado);
    }

    @Transactional
    public TrasladoAntiguedadResumenResponse confirmarLlegadaTaller(UUID trasladoId, UUID transportistaId, ConfirmarLlegadaRequest request) {
        TrasladoAntiguedad traslado = buscarAsignadoOFallar(trasladoId, transportistaId);
        validarTipo(traslado, TipoTraslado.RECOLECCION);
        validarEstadoOrigen(traslado.getAntiguedad(), EstadoAntiguedad.EN_RECOLECCION);

        traslado.setResultadoTraslado(ResultadoTraslado.EXITOSO);
        traslado.setEjecutadoEnTraslado(OffsetDateTime.now());
        traslado.getAntiguedad().setEstadoActualAntiguedad(EstadoAntiguedad.RECIBIDO_EN_TALLER);

        return guardarYResponder(traslado);
    }

    @Transactional
    public TrasladoAntiguedadResumenResponse confirmarSalidaEntregaPropietario(UUID trasladoId, UUID transportistaId, ConfirmarSalidaRequest request) {
        TrasladoAntiguedad traslado = buscarAsignadoOFallar(trasladoId, transportistaId);
        validarTipo(traslado, TipoTraslado.ENTREGA_PROPIETARIO);
        validarEstadoOrigen(traslado.getAntiguedad(), EstadoAntiguedad.HORARIO_ENTREGA_AGENDADO);

        traslado.setSalidaConfirmadaEnTraslado(OffsetDateTime.now());
        traslado.getAntiguedad().setEstadoActualAntiguedad(EstadoAntiguedad.EN_DEVOLUCION_PROPIETARIO);

        return guardarYResponder(traslado);
    }

    @Transactional
    public TrasladoAntiguedadResumenResponse confirmarEntregaPropietario(UUID trasladoId, UUID transportistaId, List<MultipartFile> fotos) {
        TrasladoAntiguedad traslado = buscarAsignadoOFallar(trasladoId, transportistaId);
        validarTipo(traslado, TipoTraslado.ENTREGA_PROPIETARIO);
        validarEstadoOrigen(traslado.getAntiguedad(), EstadoAntiguedad.EN_DEVOLUCION_PROPIETARIO);

        guardarFotos(traslado, fotos, TipoFotografia.TRASLADO_LLEGADA);
        traslado.setResultadoTraslado(ResultadoTraslado.EXITOSO);
        traslado.setEjecutadoEnTraslado(OffsetDateTime.now());
        traslado.getAntiguedad().setEstadoActualAntiguedad(EstadoAntiguedad.ENTREGADO_AL_PROPIETARIO);

        return guardarYResponder(traslado);
    }

    /*
     * MJRENEW-ENTREGA-COMPRADOR: este flujo es logístico, NO una nueva
     * transición de EstadoAntiguedad ni una orden a Stripe.
     */
    @Transactional
    public TrasladoAntiguedadResumenResponse confirmarSalidaEntregaComprador(UUID trasladoId, UUID transportistaId) {
        TrasladoAntiguedad traslado = buscarAsignadoOFallar(trasladoId, transportistaId);
        validarTipo(traslado, TipoTraslado.ENTREGA_COMPRADOR);
        validarVentaPagada(traslado.getAntiguedad());
        validarPendiente(traslado);
        if (traslado.getSalidaConfirmadaEnTraslado() != null) {
            throw new TransicionEstadoInvalidaException("La salida de este traslado ya fue confirmada");
        }
        traslado.setSalidaConfirmadaEnTraslado(OffsetDateTime.now());
        return guardarYResponder(traslado);
    }

    @Transactional
    public TrasladoAntiguedadResumenResponse confirmarEntregaComprador(UUID trasladoId, UUID transportistaId,
                                                                       List<MultipartFile> fotos) {
        TrasladoAntiguedad traslado = buscarAsignadoOFallar(trasladoId, transportistaId);
        validarTipo(traslado, TipoTraslado.ENTREGA_COMPRADOR);
        validarVentaPagada(traslado.getAntiguedad());
        validarPendiente(traslado);
        if (traslado.getSalidaConfirmadaEnTraslado() == null) {
            throw new TransicionEstadoInvalidaException("Primero confirma la salida hacia el comprador");
        }

        guardarFotos(traslado, fotos, TipoFotografia.TRASLADO_LLEGADA);
        traslado.setResultadoTraslado(ResultadoTraslado.EXITOSO);
        traslado.setEjecutadoEnTraslado(OffsetDateTime.now());
        // El comprador aún debe confirmar personalmente la recepción.
        return guardarYResponder(traslado);
    }

    private void validarVentaPagada(Antiguedad antiguedad) {
        validarEstadoOrigen(antiguedad, EstadoAntiguedad.VENTA_COMPLETADA);
        CatalogoAntiguedad catalogo = catalogoRepository
                .findByAntiguedad_AntiguedadesId(antiguedad.getAntiguedadesId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Esta antigüedad no tiene venta registrada"));
        if (catalogo.getComprador() == null || catalogo.getCompletadaEnCatalogo() == null
                || Boolean.TRUE.equals(catalogo.getActivaCatalogo())) {
            throw new SolicitudInvalidaException("La compra no está confirmada; no se puede entregar");
        }
    }

    private void validarPendiente(TrasladoAntiguedad traslado) {
        if (traslado.getResultadoTraslado() != ResultadoTraslado.PENDIENTE) {
            throw new TransicionEstadoInvalidaException("El traslado ya fue cerrado");
        }
    }

    /*
     * ============================================================
     * INTENTO FALLIDO (el propietario no estaba en casa)
     * ============================================================
     *
     * El traslado se cierra como NADIE_EN_CASA con la nota del transportista
     * y la antigüedad regresa al estado desde el que se agenda, para que el
     * administrador cree un nuevo intento con crearTraslado. El propietario
     * ve el aviso en su lista de piezas (AntiguedadService.construirResumen).
     *
     * RECOLECCION:         HORARIO_RECOLECCION_AGENDADO -> PAGO_EN_ESCROW
     * ENTREGA_PROPIETARIO: EN_DEVOLUCION_PROPIETARIO    -> RESTAURACION_LISTA
     */
    @Transactional
    public TrasladoAntiguedadResumenResponse registrarIntentoFallido(UUID trasladoId, UUID transportistaId,
                                                                     RegistrarIntentoFallidoRequest request,
                                                                     MultipartFile foto) {
        TrasladoAntiguedad traslado = buscarAsignadoOFallar(trasladoId, transportistaId);

        if (traslado.getResultadoTraslado() != ResultadoTraslado.PENDIENTE) {
            throw new TransicionEstadoInvalidaException(
                    "Este traslado ya está cerrado como " + traslado.getResultadoTraslado());
        }

        EstadoAntiguedad estadoDestino = switch (traslado.getTipoTraslado()) {
            case RECOLECCION -> {
                validarEstadoOrigen(traslado.getAntiguedad(), EstadoAntiguedad.HORARIO_RECOLECCION_AGENDADO);
                yield EstadoAntiguedad.PAGO_EN_ESCROW;
            }
            case ENTREGA_PROPIETARIO -> {
                validarEstadoOrigen(traslado.getAntiguedad(), EstadoAntiguedad.EN_DEVOLUCION_PROPIETARIO);
                yield EstadoAntiguedad.RESTAURACION_LISTA;
            }
            case ENTREGA_COMPRADOR -> {
                validarVentaPagada(traslado.getAntiguedad());
                if (traslado.getSalidaConfirmadaEnTraslado() == null) {
                    throw new TransicionEstadoInvalidaException("Primero confirma la salida hacia el comprador");
                }
                yield EstadoAntiguedad.VENTA_COMPLETADA; // Permite un nuevo intento sin alterar el AFD.
            }
        };

        // Misma marca de tiempo para la foto y el traslado: así el aviso al
        // propietario encuentra la foto de este intento y no la de uno anterior.
        OffsetDateTime ahora = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);

        if (foto != null && !foto.isEmpty()) {
            if (!TIPOS_FOTO_PERMITIDOS.contains(foto.getContentType())) {
                throw new SolicitudInvalidaException("La foto debe ser JPG, PNG o WEBP");
            }
            guardarFoto(traslado, foto, TipoFotografia.TRASLADO_INTENTO_FALLIDO, ahora);
        }

        traslado.setResultadoTraslado(ResultadoTraslado.NADIE_EN_CASA);
        traslado.setMotivoFalloTraslado(request.motivoFallo().trim());
        traslado.setEjecutadoEnTraslado(ahora);
        traslado.getAntiguedad().setEstadoActualAntiguedad(estadoDestino);

        return guardarYResponder(traslado);
    }

    private void guardarFotos(TrasladoAntiguedad traslado, List<MultipartFile> fotos, TipoFotografia tipoFoto) {
        if (fotos == null || fotos.size() < FOTOS_MINIMAS) {
            throw new SolicitudInvalidaException("Se requieren al menos " + FOTOS_MINIMAS + " fotografías de evidencia");
        }
        for (MultipartFile archivo : fotos) {
            if (archivo == null || archivo.isEmpty() || !TIPOS_FOTO_PERMITIDOS.contains(archivo.getContentType())) {
                throw new SolicitudInvalidaException("Todas las evidencias deben ser imágenes JPG, PNG o WEBP válidas");
            }
        }
        for (MultipartFile archivo : fotos) {
            guardarFoto(traslado, archivo, tipoFoto, OffsetDateTime.now());
        }
    }

    private void guardarFoto(TrasladoAntiguedad traslado, MultipartFile archivo, TipoFotografia tipoFoto, OffsetDateTime subidaEn) {
        String url = almacenamientoService.subirArchivo(archivo, "traslados");
        FotografiaAntiguedad foto = new FotografiaAntiguedad();
        foto.setAntiguedad(traslado.getAntiguedad());
        foto.setTraslado(traslado);  // Vincula fotos con este intento específico.
        foto.setTipoFotografia(tipoFoto);
        foto.setUrlAlmacenFotografia(url);
        foto.setSubidaEnFotografia(subidaEn);
        fotografiaRepository.save(foto);
    }

    private void validarTipo(TrasladoAntiguedad traslado, TipoTraslado esperado) {
        if (traslado.getTipoTraslado() != esperado) {
            throw new SolicitudInvalidaException("Este traslado no corresponde a una acción de " + esperado);
        }
    }

    private void validarEstadoOrigen(Antiguedad antiguedad, EstadoAntiguedad esperado) {
        if (antiguedad.getEstadoActualAntiguedad() != esperado) {
            throw new TransicionEstadoInvalidaException(
                    "La antigüedad debe estar en " + esperado + " para esta acción, y está en " + antiguedad.getEstadoActualAntiguedad()
            );
        }
    }

    private TrasladoAntiguedadResumenResponse guardarYResponder(TrasladoAntiguedad traslado) {
        antiguedadRepository.save(traslado.getAntiguedad());
        TrasladoAntiguedad actualizado = trasladoRepository.save(traslado);
        return trasladoMapper.toResumen(actualizado);
    }

    private TrasladoAntiguedad buscarAsignadoOFallar(UUID trasladoId, UUID transportistaId) {
        TrasladoAntiguedad traslado = trasladoRepository.findById(trasladoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un traslado con id " + trasladoId));

        if (traslado.getTransportista() == null || !traslado.getTransportista().getUsuariosId().equals(transportistaId)) {
            throw new RecursoNoEncontradoException("No existe un traslado con id " + trasladoId);
        }

        //

        return traslado;
    }
}