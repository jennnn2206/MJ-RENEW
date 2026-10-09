package com.mjrenew.mjrenew_backend.catalogo.repository;

import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import java.util.UUID;

public interface CatalogoAntiguedadRepository extends JpaRepository<CatalogoAntiguedad, UUID> {

    Page<CatalogoAntiguedad> findByActivaCatalogoTrue(Pageable pageable);

    boolean existsByAntiguedad_AntiguedadesId(UUID antiguedadId);

    // MJRENEW-STRIPE: solo un comprador puede reservar una antigüedad a la vez.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CatalogoAntiguedad c where c.catalogoAntiguedadId = :catalogoId")
    Optional<CatalogoAntiguedad> buscarParaCompra(@Param("catalogoId") UUID catalogoId);
}