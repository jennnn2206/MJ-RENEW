package com.mjrenew.mjrenew_backend.propietario.service;

import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadCreateRequest;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadResponse;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.exception.TransicionEstadoInvalidaException;
import com.mjrenew.mjrenew_backend.propietario.mapper.AntiguedadMapper;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository.AntiguedadRepository;
import com.mjrenew.mjrenew_backend.propietario.estado.EstadoAntiguedadBehaviorResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AntiguedadService {

    private final AntiguedadRepository antiguedadRepository;
    private final AntiguedadMapper antiguedadMapper;
    private final EstadoAntiguedadBehaviorResolver estadoBehaviorResolver;

    public AntiguedadService(AntiguedadRepository antiguedadRepository,
                             AntiguedadMapper antiguedadMapper,
                             EstadoAntiguedadBehaviorResolver estadoBehaviorResolver) {
        this.antiguedadRepository = antiguedadRepository;
        this.antiguedadMapper = antiguedadMapper;
        this.estadoBehaviorResolver = estadoBehaviorResolver;
    }

    @Transactional
    public AntiguedadResponse crear(AntiguedadCreateRequest request, Usuario propietario) {
        Antiguedad antiguedad = antiguedadMapper.toEntity(request);
        antiguedad.setPropietario(propietario);
        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.PIEZA_CAPTURADA);
        antiguedad.setRegistradaEnAntiguedad(OffsetDateTime.now());

        Antiguedad guardada = antiguedadRepository.save(antiguedad);
        return antiguedadMapper.toResponse(guardada);
    }

    public List<AntiguedadResponse> listarMias(UUID propietarioId) {
        return antiguedadRepository.findByPropietario_UsuariosId(propietarioId).stream()
                .map(antiguedadMapper::toResponse)
                .toList();
    }

    public AntiguedadResponse obtenerPorId(UUID id) {
        return antiguedadMapper.toResponse(buscarOFallar(id));
    }

    @Transactional
    public AntiguedadResponse cambiarEstado(UUID id, EstadoAntiguedad nuevoEstado) {
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
        return antiguedadMapper.toResponse(actualizada);
    }

    private Antiguedad buscarOFallar(UUID id) {
        return antiguedadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una pieza con id " + id));
    }
}