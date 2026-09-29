package com.mjrenew.mjrenew_backend.transportista.service;

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
import com.mjrenew.mjrenew_backend.transportista.dto.TrasladoAntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.transportista.dto.TrasladoAntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.transportista.entity.TrasladoAntiguedad;
import com.mjrenew.mjrenew_backend.transportista.mapper.TrasladoMapper;
import com.mjrenew.mjrenew_backend.transportista.repository.TrasladoAntiguedadRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TrasladoAntiguedadService {

    //Cantidad de evidencias fotograficas minimas
    private static final int FOTOS_MINIMAS = 4;

    private final TrasladoAntiguedadRepository trasladoRepository;
    private final AntiguedadRepository antiguedadRepository;
    private final FotografiaAntiguedadRepository fotografiaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TrasladoMapper trasladoMapper;

    public TrasladoAntiguedadService(TrasladoAntiguedadRepository trasladoRepository,
                                     AntiguedadRepository antiguedadRepository,
                                     FotografiaAntiguedadRepository fotografiaRepository,
                                     UsuarioRepository usuarioRepository,
                                     TrasladoMapper trasladoMapper) {
        this.trasladoRepository = trasladoRepository;
        this.antiguedadRepository = antiguedadRepository;
        this.fotografiaRepository = fotografiaRepository;
        this.usuarioRepository = usuarioRepository;
        this.trasladoMapper = trasladoMapper;
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
     * ENTREGA_COMPRADOR queda fuera de alcance (ver validarOrigenYObtenerDestino).
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

        if (trasladoRepository.existsByAntiguedad_AntiguedadesIdAndTipoTraslado(
                request.antiguedadId(), request.tipoTraslado())) {

            throw new SolicitudInvalidaException(
                    "Ya existe un traslado de tipo " + request.tipoTraslado() + " para esta antigüedad");
        }

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
        traslado.setNumeroIntentoTraslado((short) 1);
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

            // TODO: falta el flujo de confirmación de pago del comprador (más allá
            // de LINK_PAGO_ENVIADO) del cual dependería este traslado.
            case ENTREGA_COMPRADOR -> throw new SolicitudInvalidaException(
                    "La creación de traslados de entrega a comprador aún no está soportada");
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
    public TrasladoAntiguedadResumenResponse confirmarSalidaRecoleccion(UUID trasladoId, UUID transportistaId, ConfirmarSalidaRequest request) {
        TrasladoAntiguedad traslado = buscarAsignadoOFallar(trasladoId, transportistaId);
        validarTipo(traslado, TipoTraslado.RECOLECCION);
        validarEstadoOrigen(traslado.getAntiguedad(), EstadoAntiguedad.HORARIO_RECOLECCION_AGENDADO);

        guardarFotos(traslado, request.fotos(), TipoFotografia.TRASLADO_SALIDA);
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
    public TrasladoAntiguedadResumenResponse confirmarEntregaPropietario(UUID trasladoId, UUID transportistaId, ConfirmarLlegadaRequest request) {
        TrasladoAntiguedad traslado = buscarAsignadoOFallar(trasladoId, transportistaId);
        validarTipo(traslado, TipoTraslado.ENTREGA_PROPIETARIO);
        validarEstadoOrigen(traslado.getAntiguedad(), EstadoAntiguedad.EN_DEVOLUCION_PROPIETARIO);

        guardarFotos(traslado, request.fotos(), TipoFotografia.TRASLADO_LLEGADA);
        traslado.setResultadoTraslado(ResultadoTraslado.EXITOSO);
        traslado.setEjecutadoEnTraslado(OffsetDateTime.now());
        traslado.getAntiguedad().setEstadoActualAntiguedad(EstadoAntiguedad.ENTREGADO_AL_PROPIETARIO);

        return guardarYResponder(traslado);
    }

    private void guardarFotos(TrasladoAntiguedad traslado, List<String> fotos, TipoFotografia tipoFoto) {
        if (fotos == null || fotos.size() < FOTOS_MINIMAS) {
            throw new SolicitudInvalidaException("Se requieren al menos " + FOTOS_MINIMAS + " fotografías de evidencia");
        }
        for (String url : fotos) {
            FotografiaAntiguedad foto = new FotografiaAntiguedad();
            foto.setAntiguedad(traslado.getAntiguedad());
            foto.setTipoFotografia(tipoFoto);
            foto.setUrlAlmacenFotografia(url);
            foto.setSubidaEnFotografia(OffsetDateTime.now());
            fotografiaRepository.save(foto);
        } // usar builder o check de los atributos de la entidad
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