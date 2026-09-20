package com.mjrenew.mjrenew_backend.transportista.service;

import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository.AntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.ResultadoTraslado;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoFotografia;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTraslado;
import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.exception.TransicionEstadoInvalidaException;
import com.mjrenew.mjrenew_backend.propietario.entity.FotografiaAntiguedad;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.transportista.dto.ConfirmarLlegadaRequest;
import com.mjrenew.mjrenew_backend.transportista.dto.ConfirmarSalidaRequest;
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

    private static final int FOTOS_MINIMAS = 4;

    private final TrasladoAntiguedadRepository trasladoRepository;
    private final AntiguedadRepository antiguedadRepository;
    private final FotografiaAntiguedadRepository fotografiaRepository;
    private final TrasladoMapper trasladoMapper;

    public TrasladoAntiguedadService(TrasladoAntiguedadRepository trasladoRepository,
                                     AntiguedadRepository antiguedadRepository,
                                     FotografiaAntiguedadRepository fotografiaRepository,
                                     TrasladoMapper trasladoMapper) {
        this.trasladoRepository = trasladoRepository;
        this.antiguedadRepository = antiguedadRepository;
        this.fotografiaRepository = fotografiaRepository;
        this.trasladoMapper = trasladoMapper;
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
        }
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

        return traslado;
    }
}