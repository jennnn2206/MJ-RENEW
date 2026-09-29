package com.mjrenew.mjrenew_backend.restaurador.service;

import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository.AntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.enums.DisponibilidadRestaurador;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoFotografia;
import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.exception.TransicionEstadoInvalidaException;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.propietario.dto.DimensionResponse;
import com.mjrenew.mjrenew_backend.propietario.entity.Dimension;
import com.mjrenew.mjrenew_backend.propietario.entity.FotografiaAntiguedad;
import com.mjrenew.mjrenew_backend.propietario.mapper.AntiguedadMapper;
import com.mjrenew.mjrenew_backend.propietario.repository.DimensionRepository;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.restaurador.dto.AntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.restaurador.dto.EvaluarAntiguedadRequest;
import com.mjrenew.mjrenew_backend.restaurador.dto.PerfilRestauradorPublicoResponse;
import com.mjrenew.mjrenew_backend.restaurador.dto.RechazarEvaluacionRequest;
import com.mjrenew.mjrenew_backend.restaurador.mapper.RestauradorMapper;
import com.mjrenew.mjrenew_backend.restaurador.repository.PerfilRestauradorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoCotizacion;
import com.mjrenew.mjrenew_backend.restaurador.dto.CotizacionResumenResponse;
import com.mjrenew.mjrenew_backend.restaurador.dto.GenerarCotizacionRequest;
import com.mjrenew.mjrenew_backend.restaurador.entity.Cotizacion;
import com.mjrenew.mjrenew_backend.restaurador.repository.CotizacionRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class RestauradorService {

    private final AntiguedadRepository antiguedadRepository;
    private final FotografiaAntiguedadRepository fotografiaAntiguedadRepository;
    private final RestauradorMapper restauradorMapper;
    private final PerfilRestauradorRepository perfilRestauradorRepository;
    private final DimensionRepository dimensionRepository;
    private final AntiguedadMapper antiguedadMapper;
    private final CotizacionRepository cotizacionRepository;


    public RestauradorService(
            AntiguedadRepository antiguedadRepository,
            FotografiaAntiguedadRepository fotografiaAntiguedadRepository,
            RestauradorMapper restauradorMapper,
            PerfilRestauradorRepository perfilRestauradorRepository,
            DimensionRepository dimensionRepository,
            AntiguedadMapper antiguedadMapper,
            CotizacionRepository cotizacionRepository
    ) {
        this.antiguedadRepository = antiguedadRepository;
        this.fotografiaAntiguedadRepository = fotografiaAntiguedadRepository;
        this.restauradorMapper = restauradorMapper;
        this.perfilRestauradorRepository = perfilRestauradorRepository;
        this.dimensionRepository = dimensionRepository;
        this.antiguedadMapper = antiguedadMapper;
        this.cotizacionRepository = cotizacionRepository;
    }


    /*
     * ============================================================
     * SOLICITUDES ASIGNADAS AL RESTAURADOR
     * ============================================================
     */

    @Transactional(readOnly = true)
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


    /*
     * ============================================================
     * BÚSQUEDA PÚBLICA DE RESTAURADORES
     * ============================================================
     *
     * Utilizado por el propietario cuando quiere seleccionar
     * un restaurador.
     */

    @Transactional(readOnly = true)
    public Page<PerfilRestauradorPublicoResponse> buscarRestauradores(
            Pageable pageable
    ) {

        return perfilRestauradorRepository
                .findByDisponibilidadRestauradorAndAprobadoPorAdminRestauradorTrueAndRestaurador_ActivoUsuarioTrue(
                        DisponibilidadRestaurador.DISPONIBLE,
                        pageable
                )
                .map(restauradorMapper::toPerfilPublico);
    }


    /*
     * ============================================================
     * RECHAZAR EVALUACIÓN
     * ============================================================
     *
     * EN_EVALUACION
     *      ↓
     * CANCELADA_NO_CUMPLE
     */

    @Transactional
    public AntiguedadDetalleResponse rechazarEvaluacion(
            UUID antiguedadId,
            UUID restauradorId,
            RechazarEvaluacionRequest request
    ) {

        Antiguedad antiguedad =
                buscarAsignadaOFallar(
                        antiguedadId,
                        restauradorId
                );

        validarEstado(
                antiguedad,
                EstadoAntiguedad.EN_EVALUACION
        );

        antiguedad.setMotivoRechazoEvaluacion(
                request.motivoRechazoEvaluacion()
        );

        antiguedad.setEstadoActualAntiguedad(
                EstadoAntiguedad.CANCELADA_NO_CUMPLE
        );

        /*
         * No se llama antiguedadRepository.save().
         *
         * Como la entidad está administrada dentro de una
         * transacción, Hibernate actualizará los cambios
         * mediante dirty checking.
         */

        return construirDetalle(antiguedad);
    }


    /*
     * ============================================================
     * CONFIRMAR EVALUACIÓN
     * ============================================================
     *
     * EN_EVALUACION
     *      ↓
     * CALCULANDO_PRESUPUESTO
     *
     * Durante esta operación se registran también las dimensiones
     * tomadas por el restaurador.
     */

    @Transactional
    public AntiguedadDetalleResponse confirmarEvaluacion(
            UUID antiguedadId,
            UUID restauradorId,
            EvaluarAntiguedadRequest request
    ) {

        Antiguedad antiguedad =
                buscarAsignadaOFallar(
                        antiguedadId,
                        restauradorId
                );

        validarEstado(
                antiguedad,
                EstadoAntiguedad.EN_EVALUACION
        );


        /*
         * Se evita crear dos registros de dimensiones
         * para la misma antigüedad.
         */
        if (dimensionRepository
                .findByAntiguedad_AntiguedadesId(antiguedadId)
                .isPresent()) {

            throw new TransicionEstadoInvalidaException(
                    "La antigüedad ya tiene dimensiones registradas"
            );
        }


        /*
         * MapStruct crea la entidad Dimension a partir
         * del DTO recibido.
         */
        Dimension dimension =
                restauradorMapper.toDimension(request);

        dimension.setAntiguedad(antiguedad);

        dimension.setRegistradasEnDimensiones(
                OffsetDateTime.now()
        );

        Dimension dimensionGuardada =
                dimensionRepository.save(dimension);


        /*
         * Transición del autómata.
         */
        antiguedad.setEstadoActualAntiguedad(
                EstadoAntiguedad.CALCULANDO_PRESUPUESTO
        );


        DimensionResponse dimensionResponse =
                antiguedadMapper.toDimensionResponse(
                        dimensionGuardada
                );


        return antiguedadMapper.toDetalle(
                antiguedad,
                dimensionResponse
        );
    }

    /*
     * ============================================================
     * GENERAR COTIZACIÓN
     * ============================================================
     *
     * CALCULANDO_PRESUPUESTO
     *      ↓
     * PRESUPUESTO_PRESENTADO
     */

    @Transactional
    public CotizacionResumenResponse generarCotizacion(
            UUID antiguedadId,
            UUID restauradorId,
            GenerarCotizacionRequest request
    ) {

        Antiguedad antiguedad =
                buscarAsignadaOFallar(
                        antiguedadId,
                        restauradorId
                );

        validarEstado(
                antiguedad,
                EstadoAntiguedad.CALCULANDO_PRESUPUESTO
        );


        /*
         * Una antigüedad no puede generar dos cotizaciones
         * dentro del flujo actual.
         */
        if (cotizacionRepository
                .existsByAntiguedad_AntiguedadesId(
                        antiguedadId
                )) {

            throw new TransicionEstadoInvalidaException(
                    "La antigüedad ya tiene una cotización registrada"
            );
        }


        /*
         * MapStruct crea la entidad con los datos
         * que provienen del request.
         */
        Cotizacion cotizacion =
                restauradorMapper.toCotizacion(request);


        /*
         * Estos valores NO deben venir del frontend.
         *
         * El servidor conoce:
         * - qué antigüedad se está cotizando;
         * - quién es el restaurador asignado;
         * - cuál es el estado;
         * - cuándo se creó.
         */
        cotizacion.setAntiguedad(antiguedad);

        cotizacion.setRestaurador(
                antiguedad.getRestaurador()
        );

        cotizacion.setEstadoCotizacion(
                EstadoCotizacion.ENVIADA
        );

        cotizacion.setEnviadaEnCotizacion(
                OffsetDateTime.now()
        );


        /*
         * Cotizacion ES una entidad nueva,
         * por eso sí utilizamos save().
         */
        Cotizacion cotizacionGuardada =
                cotizacionRepository.save(cotizacion);


        /*
         * La antigüedad ya está administrada por Hibernate.
         *
         * No necesitamos llamar antiguedadRepository.save().
         * @Transactional + dirty checking harán el UPDATE.
         */
        antiguedad.setEstadoActualAntiguedad(
                EstadoAntiguedad.PRESUPUESTO_PRESENTADO
        );


        return restauradorMapper
                .toCotizacionResumen(
                        cotizacionGuardada
                );
    }

    /*
     * ============================================================
     * CONVERSIÓN PARA LISTADO
     * ============================================================
     */

    private AntiguedadResumenResponse convertirAResumen(
            Antiguedad antiguedad
    ) {

        String urlFotoPortada =
                fotografiaAntiguedadRepository
                        .findFirstByAntiguedad_AntiguedadesIdAndTipoFotografiaOrderBySubidaEnFotografiaAsc(
                                antiguedad.getAntiguedadesId(),
                                TipoFotografia.ESTADO_INICIAL
                        )
                        .map(
                                FotografiaAntiguedad::getUrlAlmacenFotografia
                        )
                        .orElse(null);


        return restauradorMapper.toResumen(
                antiguedad,
                urlFotoPortada
        );
    }


    /*
     * ============================================================
     * BUSCAR ANTIGÜEDAD ASIGNADA
     * ============================================================
     *
     * No basta con buscar por antiguedadId.
     *
     * También se exige que la antigüedad pertenezca al
     * restaurador autenticado.
     */

    private Antiguedad buscarAsignadaOFallar(
            UUID antiguedadId,
            UUID restauradorId
    ) {

        return antiguedadRepository
                .findByAntiguedadesIdAndRestaurador_UsuariosId(
                        antiguedadId,
                        restauradorId
                )
                .orElseThrow(
                        () -> new RecursoNoEncontradoException(
                                "La antigüedad solicitada no está asignada al restaurador autenticado"
                        )
                );
    }


    /*
     * ============================================================
     * VALIDAR ESTADO
     * ============================================================
     */

    private void validarEstado(
            Antiguedad antiguedad,
            EstadoAntiguedad estadoEsperado
    ) {

        if (antiguedad.getEstadoActualAntiguedad()
                != estadoEsperado) {

            throw new TransicionEstadoInvalidaException(
                    "La antigüedad debe estar en estado "
                            + estadoEsperado.name()
            );
        }
    }


    /*
     * ============================================================
     * CONSTRUIR DETALLE
     * ============================================================
     */

    private AntiguedadDetalleResponse construirDetalle(
            Antiguedad antiguedad
    ) {

        DimensionResponse dimensionResponse =
                dimensionRepository
                        .findByAntiguedad_AntiguedadesId(
                                antiguedad.getAntiguedadesId()
                        )
                        .map(
                                antiguedadMapper::toDimensionResponse
                        )
                        .orElse(null);


        return antiguedadMapper.toDetalle(
                antiguedad,
                dimensionResponse
        );
    }
}