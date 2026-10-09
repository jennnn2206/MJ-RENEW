package com.mjrenew.mjrenew_backend.restaurador.controller;

import com.mjrenew.mjrenew_backend.nucleo.security.UsuarioPrincipal;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.restaurador.dto.ActualizarDisponibilidadRequest;
import com.mjrenew.mjrenew_backend.restaurador.dto.ActualizarPerfilRestauradorRequest;
import com.mjrenew.mjrenew_backend.restaurador.dto.AntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.restaurador.dto.AvanceRestauracionResumenResponse;
import com.mjrenew.mjrenew_backend.restaurador.dto.EvaluarAntiguedadRequest;
import com.mjrenew.mjrenew_backend.restaurador.dto.PerfilRestauradorCompletoResponse;
import com.mjrenew.mjrenew_backend.restaurador.dto.PublicarAvanceRequest;
import com.mjrenew.mjrenew_backend.restaurador.dto.RechazarEvaluacionRequest;
import com.mjrenew.mjrenew_backend.restaurador.service.RestauradorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.mjrenew.mjrenew_backend.restaurador.dto.CotizacionResumenResponse;
import com.mjrenew.mjrenew_backend.restaurador.dto.GenerarCotizacionRequest;


import java.util.UUID;

@RestController
@RequestMapping("/api/restaurador")
public class RestauradorController {

    private final RestauradorService restauradorService;


    public RestauradorController(
            RestauradorService restauradorService
    ) {
        this.restauradorService = restauradorService;
    }


    /*
     * ============================================================
     * OBTENER SOLICITUDES ASIGNADAS
     * ============================================================
     *
     * Devuelve las antigüedades que se encuentran en
     * EN_EVALUACION y que pertenecen al restaurador autenticado.
     *
     * GET /api/restaurador/obtenerSolicitudesAsignadas
     */

    @GetMapping("/obtenerSolicitudesAsignadas")
    @PreAuthorize("hasRole('RESTAURADOR')")
    public ResponseEntity<Page<AntiguedadResumenResponse>>
    obtenerSolicitudesAsignadas(

            Pageable pageable,

            @AuthenticationPrincipal
            UsuarioPrincipal principal
    ) {

        UUID restauradorId =
                principal
                        .getUsuario()
                        .getUsuariosId();


        return ResponseEntity.ok(
                restauradorService
                        .obtenerSolicitudesAsignadas(
                                restauradorId,
                                pageable
                        )
        );
    }


    // MJRENEW-FLUJO: consultas por rol para no consultar un expediente con la cuenta de otro usuario.
    // Extensiones de lectura al diseño v4 (no son transiciones de estado).
    @GetMapping("/obtenerMisAntiguedades")
    @PreAuthorize("hasRole('RESTAURADOR')")
    public ResponseEntity<Page<AntiguedadResumenResponse>> obtenerMisAntiguedades(
            Pageable pageable, @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(restauradorService.obtenerMisAntiguedades(
                principal.getUsuario().getUsuariosId(), pageable));
    }

    @GetMapping("/obtenerDetalleAntiguedadAsignada/{antiguedadId}")
    @PreAuthorize("hasRole('RESTAURADOR')")
    public ResponseEntity<AntiguedadDetalleResponse> obtenerDetalleAntiguedadAsignada(
            @PathVariable UUID antiguedadId, @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(restauradorService.obtenerDetalleAsignado(
                antiguedadId, principal.getUsuario().getUsuariosId()));
    }

    /*
     * ============================================================
     * RECHAZAR EVALUACIÓN
     * ============================================================
     *
     * Transición:
     *
     * EN_EVALUACION
     *      ↓
     * CANCELADA_NO_CUMPLE
     *
     * POST
     * /api/restaurador/rechazarEvaluacionAntiguedad/{antiguedadId}
     */

    @PostMapping(
            "/rechazarEvaluacionAntiguedad/{antiguedadId}"
    )
    @PreAuthorize("hasRole('RESTAURADOR')")
    public ResponseEntity<AntiguedadDetalleResponse>
    rechazarEvaluacionAntiguedad(

            @PathVariable
            UUID antiguedadId,

            @Valid
            @RequestBody
            RechazarEvaluacionRequest request,

            @AuthenticationPrincipal
            UsuarioPrincipal principal
    ) {

        UUID restauradorId =
                principal
                        .getUsuario()
                        .getUsuariosId();


        return ResponseEntity.ok(
                restauradorService
                        .rechazarEvaluacion(
                                antiguedadId,
                                restauradorId,
                                request
                        )
        );
    }


    /*
     * ============================================================
     * CONFIRMAR EVALUACIÓN
     * ============================================================
     *
     * Transición:
     *
     * EN_EVALUACION
     *      ↓
     * CALCULANDO_PRESUPUESTO
     *
     * Durante esta operación se registran las dimensiones
     * proporcionadas por el restaurador.
     *
     * POST
     * /api/restaurador/confirmarEvaluacionAntiguedad/{antiguedadId}
     */

    @PostMapping(
            "/confirmarEvaluacionAntiguedad/{antiguedadId}"
    )
    @PreAuthorize("hasRole('RESTAURADOR')")
    public ResponseEntity<AntiguedadDetalleResponse>
    confirmarEvaluacionAntiguedad(

            @PathVariable
            UUID antiguedadId,

            @Valid
            @RequestBody
            EvaluarAntiguedadRequest request,

            @AuthenticationPrincipal
            UsuarioPrincipal principal
    ) {

        UUID restauradorId =
                principal
                        .getUsuario()
                        .getUsuariosId();


        return ResponseEntity.ok(
                restauradorService
                        .confirmarEvaluacion(
                                antiguedadId,
                                restauradorId,
                                request
                        )
        );
    }

    /*
     * ============================================================
     * GENERAR COTIZACIÓN
     * ============================================================
     *
     * Transición:
     *
     * CALCULANDO_PRESUPUESTO
     *      ↓
     * PRESUPUESTO_PRESENTADO
     *
     * POST
     * /api/restaurador/generarCotizacion/{antiguedadId}
     */

    @PostMapping(
            "/generarCotizacion/{antiguedadId}"
    )
    @PreAuthorize("hasRole('RESTAURADOR')")
    public ResponseEntity<CotizacionResumenResponse>
    generarCotizacion(

            @PathVariable
            UUID antiguedadId,

            @Valid
            @RequestBody
            GenerarCotizacionRequest request,

            @AuthenticationPrincipal
            UsuarioPrincipal principal
    ) {

        UUID restauradorId =
                principal
                        .getUsuario()
                        .getUsuariosId();


        return ResponseEntity.ok(
                restauradorService
                        .generarCotizacion(
                                antiguedadId,
                                restauradorId,
                                request
                        )
        );
    }


    /*
     * ============================================================
     * INICIAR RESTAURACIÓN
     * ============================================================
     *
     * Transición:
     *
     * RECIBIDO_EN_TALLER
     *      ↓
     * EN_RESTAURACION
     *
     * POST /api/restaurador/iniciarRestauracion/{antiguedadId}
     */

    @PostMapping("/iniciarRestauracion/{antiguedadId}")
    @PreAuthorize("hasRole('RESTAURADOR')")
    public ResponseEntity<AntiguedadDetalleResponse>
    iniciarRestauracion(

            @PathVariable
            UUID antiguedadId,

            @AuthenticationPrincipal
            UsuarioPrincipal principal
    ) {

        return ResponseEntity.ok(
                restauradorService.iniciarRestauracion(
                        antiguedadId,
                        principal.getUsuario().getUsuariosId()
                )
        );
    }


    /*
     * ============================================================
     * PUBLICAR AVANCE DE RESTAURACIÓN
     * ============================================================
     *
     * Transición:
     *
     * EN_RESTAURACION | AVANCE_PUBLICADO
     *      ↓
     * AVANCE_PUBLICADO
     *
     * POST /api/restaurador/publicarAvanceRestauracion/{antiguedadId}
     */

    @PostMapping("/publicarAvanceRestauracion/{antiguedadId}")
    @PreAuthorize("hasRole('RESTAURADOR')")
    public ResponseEntity<AvanceRestauracionResumenResponse>
    publicarAvanceRestauracion(

            @PathVariable
            UUID antiguedadId,

            @Valid
            @RequestBody
            PublicarAvanceRequest request,

            @AuthenticationPrincipal
            UsuarioPrincipal principal
    ) {

        return ResponseEntity.ok(
                restauradorService.publicarAvanceRestauracion(
                        antiguedadId,
                        principal.getUsuario().getUsuariosId(),
                        request
                )
        );
    }


    /*
     * ============================================================
     * MARCAR RESTAURACIÓN LISTA
     * ============================================================
     *
     * Transición:
     *
     * EN_RESTAURACION | AVANCE_PUBLICADO
     *      ↓
     * RESTAURACION_LISTA
     *
     * POST /api/restaurador/marcarRestauracionLista/{antiguedadId}
     */

    @PostMapping("/marcarRestauracionLista/{antiguedadId}")
    @PreAuthorize("hasRole('RESTAURADOR')")
    public ResponseEntity<AntiguedadDetalleResponse>
    marcarRestauracionLista(

            @PathVariable
            UUID antiguedadId,

            @AuthenticationPrincipal
            UsuarioPrincipal principal
    ) {

        return ResponseEntity.ok(
                restauradorService.marcarRestauracionLista(
                        antiguedadId,
                        principal.getUsuario().getUsuariosId()
                )
        );
    }


    /*
     * ============================================================
     * PERFIL DEL RESTAURADOR AUTENTICADO
     * ============================================================
     *
     * GET /api/restaurador/obtenerPerfilRestaurador
     */

    @GetMapping("/obtenerPerfilRestaurador")
    @PreAuthorize("hasRole('RESTAURADOR')")
    public ResponseEntity<PerfilRestauradorCompletoResponse>
    obtenerPerfilRestaurador(

            @AuthenticationPrincipal
            UsuarioPrincipal principal
    ) {

        return ResponseEntity.ok(
                restauradorService.obtenerPerfilPropio(
                        principal.getUsuario().getUsuariosId()
                )
        );
    }


    /*
     * ============================================================
     * ACTUALIZAR PERFIL DEL RESTAURADOR
     * ============================================================
     *
     * POST /api/restaurador/actualizarPerfilRestaurador
     */

    @PostMapping("/actualizarPerfilRestaurador")
    @PreAuthorize("hasRole('RESTAURADOR')")
    public ResponseEntity<PerfilRestauradorCompletoResponse>
    actualizarPerfilRestaurador(

            @Valid
            @RequestBody
            ActualizarPerfilRestauradorRequest request,

            @AuthenticationPrincipal
            UsuarioPrincipal principal
    ) {

        return ResponseEntity.ok(
                restauradorService.actualizarPerfilRestaurador(
                        principal.getUsuario(),
                        request
                )
        );
    }


    /*
     * ============================================================
     * ACTUALIZAR DISPONIBILIDAD DEL RESTAURADOR
     * ============================================================
     *
     * POST /api/restaurador/actualizarDisponibilidadRestaurador
     */

    @PostMapping("/actualizarDisponibilidadRestaurador")
    @PreAuthorize("hasRole('RESTAURADOR')")
    public ResponseEntity<PerfilRestauradorCompletoResponse>
    actualizarDisponibilidadRestaurador(

            @Valid
            @RequestBody
            ActualizarDisponibilidadRequest request,

            @AuthenticationPrincipal
            UsuarioPrincipal principal
    ) {

        return ResponseEntity.ok(
                restauradorService.actualizarDisponibilidad(
                        principal.getUsuario().getUsuariosId(),
                        request
                )
        );
    }
}