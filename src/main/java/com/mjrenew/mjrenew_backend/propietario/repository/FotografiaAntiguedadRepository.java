package com.mjrenew.mjrenew_backend.propietario.repository;

import com.mjrenew.mjrenew_backend.nucleo.enums.TipoFotografia;
import com.mjrenew.mjrenew_backend.propietario.entity.FotografiaAntiguedad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FotografiaAntiguedadRepository extends JpaRepository<FotografiaAntiguedad, UUID> {

    // Conteo agrupado de evidencias para habilitar correctamente el botón del comprador.
    interface EvidenciasTraslado {
        UUID getTrasladoId();
        Long getTotal();
    }

    @Query("""
            select f.traslado.trasladosAntiguedadId as trasladoId, count(f) as total
            from FotografiaAntiguedad f
            where f.traslado.trasladosAntiguedadId in :trasladoIds
              and f.tipoFotografia = :tipo
            group by f.traslado.trasladosAntiguedadId
            """)
    List<EvidenciasTraslado> contarEvidenciasPorTraslado(
            @Param("trasladoIds") Collection<UUID> trasladoIds,
            @Param("tipo") TipoFotografia tipo);

    // Fotografía de portada para todas las compras de una página.
    List<FotografiaAntiguedad> findByAntiguedad_AntiguedadesIdInAndTipoFotografiaOrderBySubidaEnFotografiaAsc(
            Collection<UUID> antiguedadesId, TipoFotografia tipoFotografia);

    long countByTraslado_TrasladosAntiguedadIdAndTipoFotografia(UUID trasladoId, TipoFotografia tipoFotografia);

    Optional<FotografiaAntiguedad> findFirstByAntiguedad_AntiguedadesIdAndTipoFotografiaOrderBySubidaEnFotografiaAsc(
            UUID antiguedadId, TipoFotografia tipoFotografia);

    List<FotografiaAntiguedad> findByAntiguedad_AntiguedadesIdAndTipoFotografiaOrderBySubidaEnFotografiaAsc(
            UUID antiguedadId, TipoFotografia tipoFotografia);

    List<FotografiaAntiguedad> findByAvance_AvancesRestauracionId(UUID avanceId);

    // Foto de un intento de traslado concreto: se guarda con la misma marca de tiempo que el traslado.
    Optional<FotografiaAntiguedad> findFirstByAntiguedad_AntiguedadesIdAndTipoFotografiaAndSubidaEnFotografiaGreaterThanEqualOrderBySubidaEnFotografiaAsc(
            UUID antiguedadId, TipoFotografia tipoFotografia, OffsetDateTime desde);
}