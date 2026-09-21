package com.mjrenew.mjrenew_backend.administrador.service;

import com.mjrenew.mjrenew_backend.administrador.dto.DisputaAntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.administrador.dto.DisputaAntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.administrador.dto.ResolverDisputaRequest;
import com.mjrenew.mjrenew_backend.administrador.entity.DisputaAntiguedad;
import com.mjrenew.mjrenew_backend.administrador.mapper.DisputaAntiguedadMapper;
import com.mjrenew.mjrenew_backend.administrador.repository.DisputaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.administrador.repository.FotografiaReclamoRepository;
import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.dto.UsuarioResponse;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.nucleo.usuario.mapper.UsuarioMapper;
import com.mjrenew.mjrenew_backend.nucleo.usuario.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AdministradorService {

    private final DisputaAntiguedadRepository disputaRepository;
    private final FotografiaReclamoRepository fotografiaReclamoRepository;
    private final DisputaAntiguedadMapper disputaMapper;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;

    public AdministradorService(DisputaAntiguedadRepository disputaRepository,
                                FotografiaReclamoRepository fotografiaReclamoRepository,
                                DisputaAntiguedadMapper disputaMapper,
                                UsuarioRepository usuarioRepository,
                                UsuarioMapper usuarioMapper) {
        this.disputaRepository = disputaRepository;
        this.fotografiaReclamoRepository = fotografiaReclamoRepository;
        this.disputaMapper = disputaMapper;
        this.usuarioRepository = usuarioRepository;
        this.usuarioMapper = usuarioMapper;
    }

    public Page<DisputaAntiguedadResumenResponse> obtenerDisputasAbiertas(Pageable pageable) {
        return disputaRepository.findByResueltaEnDisputaIsNull(pageable)
                .map(disputaMapper::toResumen);
    }

    @Transactional
    public DisputaAntiguedadDetalleResponse resolverDisputa(UUID disputaId, Usuario admin, ResolverDisputaRequest request) {
        DisputaAntiguedad disputa = disputaRepository.findById(disputaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una disputa con id " + disputaId));

        if (disputa.getResueltaEnDisputa() != null) {
            throw new SolicitudInvalidaException("Esta disputa ya fue resuelta anteriormente");
        }

        disputa.setResolucionDisputa(request.resolucionDisputa());
        disputa.setAdminAsignado(admin);
        disputa.setResueltaEnDisputa(OffsetDateTime.now());

        DisputaAntiguedad actualizada = disputaRepository.save(disputa);
        return construirDetalle(actualizada);
    }

    public Page<UsuarioResponse> obtenerUsuariosRegistrados(Pageable pageable) {
        return usuarioRepository.findAll(pageable)
                .map(usuarioMapper::toResponse);
    }

    private DisputaAntiguedadDetalleResponse construirDetalle(DisputaAntiguedad disputa) {
        List<String> fotos = fotografiaReclamoRepository
                .findByDisputaAntiguedad_DisputasAntiguedadId(disputa.getDisputasAntiguedadId())
                .stream()
                .map(foto -> foto.getUrlAlmacenReclamo())
                .toList();
        return disputaMapper.toDetalle(disputa, fotos);
    }
}