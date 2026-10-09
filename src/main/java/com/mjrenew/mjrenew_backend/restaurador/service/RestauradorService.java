package com.mjrenew.mjrenew_backend.restaurador.service;

import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository.AntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.enums.DisponibilidadRestaurador;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoFotografia;
import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.exception.TransicionEstadoInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.propietario.dto.DimensionResponse;
import com.mjrenew.mjrenew_backend.propietario.entity.FotografiaAntiguedad;
import com.mjrenew.mjrenew_backend.propietario.mapper.AntiguedadMapper;
import com.mjrenew.mjrenew_backend.propietario.repository.DimensionRepository;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.restaurador.dto.ActualizarDisponibilidadRequest;
import com.mjrenew.mjrenew_backend.restaurador.dto.ActualizarPerfilRestauradorRequest;
import com.mjrenew.mjrenew_backend.restaurador.dto.AntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.restaurador.dto.AvanceRestauracionResumenResponse;
import com.mjrenew.mjrenew_backend.restaurador.dto.EvaluarAntiguedadRequest;
import com.mjrenew.mjrenew_backend.restaurador.dto.PerfilRestauradorCompletoResponse;
import com.mjrenew.mjrenew_backend.restaurador.dto.PerfilRestauradorPublicoResponse;
import com.mjrenew.mjrenew_backend.restaurador.dto.PublicarAvanceRequest;
import com.mjrenew.mjrenew_backend.restaurador.dto.RechazarEvaluacionRequest;
import com.mjrenew.mjrenew_backend.restaurador.entity.AvanceRestauracion;
import com.mjrenew.mjrenew_backend.restaurador.mapper.RestauradorMapper;
import com.mjrenew.mjrenew_backend.restaurador.repository.AvanceRestauracionRepository;
import com.mjrenew.mjrenew_backend.restaurador.repository.PerfilRestauradorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoCotizacion;
import com.mjrenew.mjrenew_backend.restaurador.dto.CotizacionResumenResponse;
import com.mjrenew.mjrenew_backend.restaurador.dto.GenerarCotizacionRequest;
import com.mjrenew.mjrenew_backend.restaurador.entity.Cotizacion;
import com.mjrenew.mjrenew_backend.restaurador.entity.PerfilRestaurador;
import com.mjrenew.mjrenew_backend.restaurador.repository.CotizacionRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
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
    private final AvanceRestauracionRepository avanceRestauracionRepository;


    public RestauradorService(
            AntiguedadRepository antiguedadRepository,
            FotografiaAntiguedadRepository fotografiaAntiguedadRepository,
            RestauradorMapper restauradorMapper,
            PerfilRestauradorRepository perfilRestauradorRepository,
            DimensionRepository dimensionRepository,
            AntiguedadMapper antiguedadMapper,
            CotizacionRepository cotizacionRepository,
            AvanceRestauracionRepository avanceRestauracionRepository
    ) {
        this.antiguedadRepository = antiguedadRepository;
        this.fotografiaAntiguedadRepository = fotografiaAntiguedadRepository;
        this.restauradorMapper = restauradorMapper;
        this.perfilRestauradorRepository = perfilRestauradorRepository;
        this.dimensionRepository = dimensionRepository;
        this.antiguedadMapper = antiguedadMapper;
        this.cotizacionRepository = cotizacionRepository;
        this.avanceRestauracionRepository = avanceRestauracionRepository;
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
                .findByRestaurador_UsuariosIdAndEstadoActualAntiguedadIn(
                        restauradorId,
                        List.of(EstadoAntiguedad.EN_EVALUACION, EstadoAntiguedad.CALCULANDO_PRESUPUESTO),
                        pageable
                )
                .map(this::convertirAResumen);
    }


    // MJRENEW-FLUJO: extensiones GET propias del restaurador, jamás reutilizar
    // /api/propietario/obtenerDetalleAntiguedad (requiere rol y propietario distintos).
    @Transactional(readOnly = true)
    public Page<AntiguedadResumenResponse> obtenerMisAntiguedades(UUID restauradorId, Pageable pageable) {
        return antiguedadRepository.findByRestaurador_UsuariosId(restauradorId, pageable)
                .map(this::convertirAResumen);
    }

    @Transactional(readOnly = true)
    public AntiguedadDetalleResponse obtenerDetalleAsignado(UUID antiguedadId, UUID restauradorId) {
        return construirDetalle(buscarAsignadaOFallar(antiguedadId, restauradorId));
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
     * Ya no registra dimensiones aquí: el restaurador no tiene la pieza
     * en su poder durante la evaluación (es visual, a partir de las fotos
     * del estado inicial), así que no puede medirla. Las dimensiones las
     * aporta el propietario al registrar la pieza — si las dio, ya están
     * guardadas de antes y se reflejan en construirDetalle() más abajo.
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
        // MJRENEW-FLUJO: sin evidencia fotográfica no procede la evaluación (RF-010).
        if (fotografiaAntiguedadRepository
                .findByAntiguedad_AntiguedadesIdAndTipoFotografiaOrderBySubidaEnFotografiaAsc(
                        antiguedadId, TipoFotografia.ESTADO_INICIAL).size() < 3) {
            throw new SolicitudInvalidaException("La antigüedad requiere al menos 3 fotografías iniciales para evaluar");
        }

        /*
         * Transición del autómata.
         */
        antiguedad.setEstadoActualAntiguedad(
                EstadoAntiguedad.CALCULANDO_PRESUPUESTO
        );

        return construirDetalle(antiguedad);
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
     * INICIAR RESTAURACIÓN
     * ============================================================
     *
     * RECIBIDO_EN_TALLER
     *      ↓
     * EN_RESTAURACION
     */

    @Transactional
    public AntiguedadDetalleResponse iniciarRestauracion(
            UUID antiguedadId,
            UUID restauradorId
    ) {

        Antiguedad antiguedad =
                buscarAsignadaOFallar(
                        antiguedadId,
                        restauradorId
                );

        validarEstado(
                antiguedad,
                EstadoAntiguedad.RECIBIDO_EN_TALLER
        );

        antiguedad.setRestauracionInicioAntiguedad(
                OffsetDateTime.now()
        );

        antiguedad.setEstadoActualAntiguedad(
                EstadoAntiguedad.EN_RESTAURACION
        );

        return construirDetalle(antiguedad);
    }


    /*
     * ============================================================
     * PUBLICAR AVANCE DE RESTAURACIÓN
     * ============================================================
     *
     * EN_RESTAURACION | AVANCE_PUBLICADO
     *      ↓
     * AVANCE_PUBLICADO
     */

    @Transactional
    public AvanceRestauracionResumenResponse publicarAvanceRestauracion(
            UUID antiguedadId,
            UUID restauradorId,
            PublicarAvanceRequest request
    ) {

        Antiguedad antiguedad =
                buscarAsignadaOFallar(
                        antiguedadId,
                        restauradorId
                );

        validarEstadoEnRestauracion(antiguedad);

        if (request.fotos() == null || request.fotos().isEmpty()) {

            throw new SolicitudInvalidaException(
                    "Se requiere al menos una fotografía del avance"
            );
        }

        AvanceRestauracion avance = new AvanceRestauracion();
        avance.setAntiguedad(antiguedad);
        avance.setRestaurador(antiguedad.getRestaurador());
        avance.setDescripcionAvance(request.descripcionAvance());
        avance.setPublicadoEnAvance(OffsetDateTime.now());

        AvanceRestauracion avanceGuardado =
                avanceRestauracionRepository.save(avance);

        List<String> fotosGuardadas = new ArrayList<>();

        for (String url : request.fotos()) {

            FotografiaAntiguedad foto = new FotografiaAntiguedad();
            foto.setAntiguedad(antiguedad);
            foto.setAvance(avanceGuardado);
            foto.setTipoFotografia(TipoFotografia.AVANCE);
            foto.setUrlAlmacenFotografia(url);
            foto.setSubidaEnFotografia(OffsetDateTime.now());

            fotografiaAntiguedadRepository.save(foto);
            fotosGuardadas.add(url);
        }

        antiguedad.setEstadoActualAntiguedad(
                EstadoAntiguedad.AVANCE_PUBLICADO
        );

        return restauradorMapper.toAvanceResumen(
                avanceGuardado,
                fotosGuardadas
        );
    }


    /*
     * ============================================================
     * MARCAR RESTAURACIÓN LISTA
     * ============================================================
     *
     * EN_RESTAURACION | AVANCE_PUBLICADO
     *      ↓
     * RESTAURACION_LISTA
     */

    @Transactional
    public AntiguedadDetalleResponse marcarRestauracionLista(
            UUID antiguedadId,
            UUID restauradorId
    ) {

        Antiguedad antiguedad =
                buscarAsignadaOFallar(
                        antiguedadId,
                        restauradorId
                );

        validarEstadoEnRestauracion(antiguedad);

        antiguedad.setRestauracionFinAntiguedad(
                OffsetDateTime.now()
        );

        antiguedad.setEstadoActualAntiguedad(
                EstadoAntiguedad.RESTAURACION_LISTA
        );

        return construirDetalle(antiguedad);
    }


    /*
     * ============================================================
     * PERFIL DEL RESTAURADOR AUTENTICADO
     * ============================================================
     */

    @Transactional(readOnly = true)
    public PerfilRestauradorCompletoResponse obtenerPerfilPropio(
            UUID restauradorId
    ) {

        PerfilRestaurador perfil =
                perfilRestauradorRepository
                        .findByRestaurador_UsuariosId(restauradorId)
                        .orElseThrow(
                                () -> new RecursoNoEncontradoException(
                                        "Aún no has completado tu perfil de restaurador"
                                )
                        );

        return restauradorMapper.toPerfilCompleto(perfil);
    }


    /*
     * ============================================================
     * ACTUALIZAR PERFIL DEL RESTAURADOR
     * ============================================================
     *
     * Upsert: si el restaurador todavía no tiene perfil (no se crea
     * uno automáticamente al registrarse), se crea aquí con los
     * valores por defecto de una solicitud nueva.
     */

    @Transactional
    public PerfilRestauradorCompletoResponse actualizarPerfilRestaurador(
            Usuario restaurador,
            ActualizarPerfilRestauradorRequest request
    ) {

        PerfilRestaurador perfil =
                perfilRestauradorRepository
                        .findByRestaurador_UsuariosId(
                                restaurador.getUsuariosId()
                        )
                        .orElseGet(() -> {

                            PerfilRestaurador nuevo =
                                    new PerfilRestaurador();

                            nuevo.setRestaurador(restaurador);

                            nuevo.setAprobadoPorAdminRestaurador(
                                    false
                            );

                            nuevo.setDisponibilidadRestaurador(
                                    DisponibilidadRestaurador.NO_DISPONIBLE
                            );

                            return nuevo;
                        });

        perfil.setEspecialidadRestaurador(
                request.especialidadRestaurador()
        );

        perfil.setAnosExperienciaRestaurador(
                request.anosExperienciaRestaurador()
        );

        perfil.setDescripcionBioRestaurador(
                request.descripcionBioRestaurador()
        );

        perfil.setActualizadoEnRestaurador(
                OffsetDateTime.now()
        );

        PerfilRestaurador guardado =
                perfilRestauradorRepository.save(perfil);

        return restauradorMapper.toPerfilCompleto(guardado);
    }


    /*
     * ============================================================
     * ACTUALIZAR DISPONIBILIDAD DEL RESTAURADOR
     * ============================================================
     *
     * Requiere que el perfil ya exista: no tiene sentido publicar
     * disponibilidad sin haber completado antes especialidad/experiencia.
     */

    @Transactional
    public PerfilRestauradorCompletoResponse actualizarDisponibilidad(
            UUID restauradorId,
            ActualizarDisponibilidadRequest request
    ) {

        PerfilRestaurador perfil =
                perfilRestauradorRepository
                        .findByRestaurador_UsuariosId(restauradorId)
                        .orElseThrow(
                                () -> new RecursoNoEncontradoException(
                                        "Debes completar tu perfil de restaurador antes de actualizar tu disponibilidad"
                                )
                        );

        perfil.setDisponibilidadRestaurador(
                request.disponibilidadRestaurador()
        );

        perfil.setActualizadoEnRestaurador(
                OffsetDateTime.now()
        );

        PerfilRestaurador guardado =
                perfilRestauradorRepository.save(perfil);

        return restauradorMapper.toPerfilCompleto(guardado);
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
     * VALIDAR ESTADO EN RESTAURACIÓN
     * ============================================================
     *
     * publicarAvanceRestauracion y marcarRestauracionLista aceptan
     * dos estados de origen: EN_RESTAURACION (primer avance) o
     * AVANCE_PUBLICADO (avances subsecuentes / cierre).
     */

    private void validarEstadoEnRestauracion(
            Antiguedad antiguedad
    ) {

        EstadoAntiguedad estado =
                antiguedad.getEstadoActualAntiguedad();

        if (estado != EstadoAntiguedad.EN_RESTAURACION
                && estado != EstadoAntiguedad.AVANCE_PUBLICADO) {

            throw new TransicionEstadoInvalidaException(
                    "La antigüedad debe estar en EN_RESTAURACION o AVANCE_PUBLICADO para esta acción, y está en "
                            + estado
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

        List<String> fotosEstadoInicial =
                fotografiaAntiguedadRepository
                        .findByAntiguedad_AntiguedadesIdAndTipoFotografiaOrderBySubidaEnFotografiaAsc(
                                antiguedad.getAntiguedadesId(),
                                TipoFotografia.ESTADO_INICIAL
                        )
                        .stream()
                        .map(FotografiaAntiguedad::getUrlAlmacenFotografia)
                        .toList();


        return antiguedadMapper.toDetalle(
                antiguedad,
                dimensionResponse,
                fotosEstadoInicial
        );
    }
}