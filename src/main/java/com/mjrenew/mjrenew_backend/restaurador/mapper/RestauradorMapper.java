package com.mjrenew.mjrenew_backend.restaurador.mapper;

import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.DisponibilidadRestaurador;
import com.mjrenew.mjrenew_backend.propietario.entity.Dimension;
import com.mjrenew.mjrenew_backend.restaurador.dto.AntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.restaurador.dto.EvaluarAntiguedadRequest;
import com.mjrenew.mjrenew_backend.restaurador.dto.PerfilRestauradorPublicoResponse;
import com.mjrenew.mjrenew_backend.restaurador.entity.PerfilRestaurador;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RestauradorMapper {

    /*
     * ============================================================
     * ANTIGÜEDAD -> RESUMEN
     * ============================================================
     *
     * Se utiliza para las solicitudes asignadas al restaurador.
     * La foto de portada se obtiene por separado y se pasa
     * como segundo parámetro.
     */

    @Mapping(
            target = "antiguedadId",
            source = "antiguedad.antiguedadesId"
    )
    @Mapping(
            target = "tipoMueble",
            source = "antiguedad.tipoMueble"
    )
    @Mapping(
            target = "estadoActualAntiguedad",
            source = "antiguedad.estadoActualAntiguedad"
    )
    @Mapping(
            target = "urlFotoPortada",
            source = "urlFotoPortada"
    )
    AntiguedadResumenResponse toResumen(
            Antiguedad antiguedad,
            String urlFotoPortada
    );


    /*
     * ============================================================
     * PERFIL RESTAURADOR -> PERFIL PÚBLICO
     * ============================================================
     *
     * Se utiliza en:
     *
     * GET /api/propietario/buscarRestauradores
     */

    @Mapping(
            target = "restauradorId",
            source = "restaurador.usuariosId"
    )
    @Mapping(
            target = "especialidadRestaurador",
            source = "especialidadRestaurador"
    )
    @Mapping(
            target = "anosExperienciaRestaurador",
            source = "anosExperienciaRestaurador"
    )
    @Mapping(
            target = "descripcionBioRestaurador",
            source = "descripcionBioRestaurador"
    )
    @Mapping(
            target = "disponibilidadRestaurador",
            source = "disponibilidadRestaurador"
    )
    PerfilRestauradorPublicoResponse toPerfilPublico(
            PerfilRestaurador perfilRestaurador
    );


    /*
     * ============================================================
     * EVALUACIÓN -> DIMENSIONES
     * ============================================================
     *
     * Convierte los datos recibidos en
     * EvaluarAntiguedadRequest a la entidad Dimension.
     *
     * Los campos que dependen del backend se asignan posteriormente
     * en RestauradorService:
     *
     * - dimensionesId -> generado por JPA
     * - antiguedad -> antigüedad autenticada/asignada
     * - registradasEnDimensiones -> fecha del servidor
     */

    @Mapping(
            target = "dimensionesId",
            ignore = true
    )
    @Mapping(
            target = "antiguedad",
            ignore = true
    )
    @Mapping(
            target = "altoCmAntiguedad",
            source = "altoCm"
    )
    @Mapping(
            target = "anchoCmAntiguedad",
            source = "anchoCm"
    )
    @Mapping(
            target = "profundidadCmAntiguedad",
            source = "profundidadCm"
    )
    @Mapping(
            target = "pesoKgAntiguedad",
            source = "pesoKg"
    )
    @Mapping(
            target = "registradasEnDimensiones",
            ignore = true
    )
    Dimension toDimension(
            EvaluarAntiguedadRequest request
    );


    /*
     * ============================================================
     * ENUM -> STRING
     * ============================================================
     *
     * PerfilRestaurador usa DisponibilidadRestaurador como enum,
     * mientras que PerfilRestauradorPublicoResponse lo devuelve
     * como String.
     */

    default String mapDisponibilidad(
            DisponibilidadRestaurador disponibilidad
    ) {

        return disponibilidad != null
                ? disponibilidad.name()
                : null;
    }
}