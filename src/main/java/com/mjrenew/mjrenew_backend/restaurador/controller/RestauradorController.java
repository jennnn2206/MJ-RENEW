package com.mjrenew.mjrenew_backend.restaurador.controller;

import com.mjrenew.mjrenew_backend.nucleo.security.UsuarioPrincipal;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.restaurador.dto.AntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.restaurador.dto.EvaluarAntiguedadRequest;
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
}