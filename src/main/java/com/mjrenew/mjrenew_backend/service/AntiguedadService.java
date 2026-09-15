package com.mjrenew.mjrenew_backend.service;

import com.mjrenew.mjrenew_backend.dto.antiguedad.AntiguedadCreateRequest;
import com.mjrenew.mjrenew_backend.dto.antiguedad.AntiguedadResponse;
import com.mjrenew.mjrenew_backend.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.entity.Usuario;
import com.mjrenew.mjrenew_backend.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.exception.TransicionEstadoInvalidaException;
import com.mjrenew.mjrenew_backend.mapper.AntiguedadMapper;
import com.mjrenew.mjrenew_backend.repository.AntiguedadRepository;
import com.mjrenew.mjrenew_backend.state.EstadoAntiguedadBehaviorResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AntiguedadService {

    private final AntiguedadRepository antiguedadRepository;
    private final EstadoAntiguedadBehaviorResolver estadoBehaviorResolver;

    public AntiguedadService(AntiguedadRepository antiguedadRepository,
                              EstadoAntiguedadBehaviorResolver estadoBehaviorResolver) {
        this.antiguedadRepository = antiguedadRepository;
        this.estadoBehaviorResolver = estadoBehaviorResolver;
    }

    @Transactional
    public AntiguedadResponse crear(AntiguedadCreateRequest request, Usuario propietario) {
        Antiguedad antiguedad = AntiguedadMapper.toEntity(request);
        antiguedad.setPropietario(propietario);
        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.PIEZA_CAPTURADA);
        antiguedad.setRegistradaEnAntiguedad(OffsetDateTime.now());

        Antiguedad guardada = antiguedadRepository.save(antiguedad);
        return AntiguedadMapper.toResponse(guardada);
    }

    public List<AntiguedadResponse> listarMias(UUID propietarioId) {
        return antiguedadRepository.findByPropietario_UsuariosId(propietarioId).stream()
                .map(AntiguedadMapper::toResponse)
                .toList();
    }

    public AntiguedadResponse obtenerPorId(UUID id) {
        return AntiguedadMapper.toResponse(buscarOFallar(id));
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
        return AntiguedadMapper.toResponse(actualizada);
    }

    private Antiguedad buscarOFallar(UUID id) {
        return antiguedadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una pieza con id " + id));
    }
}
