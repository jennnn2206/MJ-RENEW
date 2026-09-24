package com.mjrenew.mjrenew_backend.propietario.service;

import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository.AntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoFotografia;
import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadCreateRequest;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.propietario.dto.DimensionResponse;
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

    public AntiguedadService(
            AntiguedadRepository antiguedadRepository,
            FotografiaAntiguedadRepository fotografiaRepository,
            DimensionRepository dimensionRepository,
            AntiguedadMapper antiguedadMapper
    ) {
        this.antiguedadRepository = antiguedadRepository;
        this.fotografiaRepository = fotografiaRepository;
        this.dimensionRepository = dimensionRepository;
        this.antiguedadMapper = antiguedadMapper;
    }

    @Transactional
    public AntiguedadDetalleResponse crear(
            AntiguedadCreateRequest request,
            Usuario propietario
    ) {

        Antiguedad antiguedad = antiguedadMapper.toEntity(request);

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
}