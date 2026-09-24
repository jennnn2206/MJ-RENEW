// pendiente

package com.mjrenew.mjrenew_backend.restaurador.service;

import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository.AntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoFotografia;
import com.mjrenew.mjrenew_backend.propietario.entity.FotografiaAntiguedad;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.restaurador.dto.AntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.restaurador.mapper.RestauradorMapper;
import com.mjrenew.mjrenew_backend.restaurador.repository.PerfilRestauradorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.mjrenew.mjrenew_backend.nucleo.enums.DisponibilidadRestaurador;
import com.mjrenew.mjrenew_backend.restaurador.dto.PerfilRestauradorPublicoResponse;
import com.mjrenew.mjrenew_backend.restaurador.repository.PerfilRestauradorRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RestauradorService {

    private final AntiguedadRepository antiguedadRepository;
    private final FotografiaAntiguedadRepository fotografiaAntiguedadRepository;
    private final RestauradorMapper restauradorMapper;
    private final PerfilRestauradorRepository perfilRestauradorRepository;

    public RestauradorService(
            AntiguedadRepository antiguedadRepository,
            FotografiaAntiguedadRepository fotografiaAntiguedadRepository,
            RestauradorMapper restauradorMapper,
            PerfilRestauradorRepository perfilRestauradorRepository
    ) {
        this.antiguedadRepository = antiguedadRepository;
        this.fotografiaAntiguedadRepository = fotografiaAntiguedadRepository;
        this.restauradorMapper = restauradorMapper;
        this.perfilRestauradorRepository = perfilRestauradorRepository;
    }

    public Page<AntiguedadResumenResponse> obtenerSolicitudesAsignadas(
            UUID restauradorId,
            Pageable pageable
    ) {
        return antiguedadRepository
                .findByRestaurador_UsuariosIdAndEstadoActualAntiguedad(
                        restauradorId,
                        EstadoAntiguedad.EN_EVALUACION,
                        pageable
                )
                .map(this::convertirAResumen);
    }

    private AntiguedadResumenResponse convertirAResumen(Antiguedad antiguedad) {

        AntiguedadResumenResponse resumen =
                restauradorMapper.toResumen(antiguedad);

        String urlFotoPortada = fotografiaAntiguedadRepository
                .findFirstByAntiguedad_AntiguedadesIdAndTipoFotografiaOrderBySubidaEnFotografiaAsc(
                        antiguedad.getAntiguedadesId(),
                        TipoFotografia.ESTADO_INICIAL
                )
                .map(FotografiaAntiguedad::getUrlAlmacenFotografia)
                .orElse(null);

        return new AntiguedadResumenResponse(
                resumen.antiguedadId(),
                resumen.tipoMueble(),
                resumen.estadoActualAntiguedad(),
                urlFotoPortada
        );
    }

    @Transactional(readOnly = true)
    public Page<PerfilRestauradorPublicoResponse> buscarRestauradoresDisponibles(
            Pageable pageable
    ) {

        return perfilRestauradorRepository
                .findByDisponibilidadRestauradorAndAprobadoPorAdminRestauradorTrueAndRestaurador_ActivoUsuarioTrue(
                        DisponibilidadRestaurador.DISPONIBLE,
                        pageable
                )
                .map(restauradorMapper::toPerfilPublico);
    }
}