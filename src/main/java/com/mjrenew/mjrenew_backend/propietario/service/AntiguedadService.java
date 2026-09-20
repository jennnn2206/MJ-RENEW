package com.mjrenew.mjrenew_backend.propietario.service;

import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository.AntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoFotografia;
import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.exception.TransicionEstadoInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadCreateRequest;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.propietario.dto.DimensionResponse;
import com.mjrenew.mjrenew_backend.propietario.estado.EstadoAntiguedadBehaviorResolver;
import com.mjrenew.mjrenew_backend.propietario.mapper.AntiguedadMapper;
import com.mjrenew.mjrenew_backend.propietario.repository.DimensionRepository;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
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
    private final AntiguedadMapper antiguedadMapper;
    private final EstadoAntiguedadBehaviorResolver estadoBehaviorResolver;

    public AntiguedadService(AntiguedadRepository antiguedadRepository,
                             FotografiaAntiguedadRepository fotografiaRepository,
                             DimensionRepository dimensionRepository,
                             AntiguedadMapper antiguedadMapper,
                             EstadoAntiguedadBehaviorResolver estadoBehaviorResolver) {
        this.antiguedadRepository = antiguedadRepository;
        this.fotografiaRepository = fotografiaRepository;
        this.dimensionRepository = dimensionRepository;
        this.antiguedadMapper = antiguedadMapper;
        this.estadoBehaviorResolver = estadoBehaviorResolver;
    }

    @Transactional
    public AntiguedadDetalleResponse crear(AntiguedadCreateRequest request, Usuario propietario) {
        Antiguedad antiguedad = antiguedadMapper.toEntity(request);
        antiguedad.setPropietario(propietario);
        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.PIEZA_CAPTURADA);
        antiguedad.setRegistradaEnAntiguedad(OffsetDateTime.now());

        Antiguedad guardada = antiguedadRepository.save(antiguedad);
        return construirDetalle(guardada);
    }

    public Page<AntiguedadResumenResponse> listarMias(UUID propietarioId, Pageable pageable) {
        return antiguedadRepository.findByPropietario_UsuariosId(propietarioId, pageable)
                .map(this::construirResumen);
    }

    public AntiguedadDetalleResponse obtenerDetalle(UUID id) {
        return construirDetalle(buscarOFallar(id));
    }

    @Transactional
    public AntiguedadDetalleResponse cambiarEstado(UUID id, EstadoAntiguedad nuevoEstado) {
        Antiguedad antiguedad = buscarOFallar(id);

        boolean estadoActualEsTerminal = estadoBehaviorResolver
                .resolver(antiguedad.getEstadoActualAntiguedad())
                .esTerminal();
        if (estadoActualEsTerminal) {
            throw new TransicionEstadoInvalidaException(
                    "La pieza está en un estado terminal (" + antiguedad.getEstadoActualAntiguedad() + ") y no admite más transiciones"
            );
        }

        antiguedad.setEstadoActualAntiguedad(nuevoEstado);
        Antiguedad actualizada = antiguedadRepository.save(antiguedad);
        return construirDetalle(actualizada);
    }

    private AntiguedadResumenResponse construirResumen(Antiguedad antiguedad) {
        String urlFotoPortada = fotografiaRepository
                .findFirstByAntiguedad_AntiguedadesIdAndTipoFotografiaOrderBySubidaEnFotografiaAsc(
                        antiguedad.getAntiguedadesId(), TipoFotografia.ESTADO_INICIAL)
                .map(foto -> foto.getUrlAlmacenFotografia())
                .orElse(null);
        return antiguedadMapper.toResumen(antiguedad, urlFotoPortada);
    }

    private AntiguedadDetalleResponse construirDetalle(Antiguedad antiguedad) {
        DimensionResponse dimension = dimensionRepository
                .findByAntiguedad_AntiguedadesId(antiguedad.getAntiguedadesId())
                .map(antiguedadMapper::toDimensionResponse)
                .orElse(null);
        return antiguedadMapper.toDetalle(antiguedad, dimension);
    }

    private Antiguedad buscarOFallar(UUID id) {
        return antiguedadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una pieza con id " + id));
    }
}