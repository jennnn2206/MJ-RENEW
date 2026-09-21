package com.mjrenew.mjrenew_backend.catalogo.repository;

import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CatalogoAntiguedadRepository extends JpaRepository<CatalogoAntiguedad, UUID> {

    Page<CatalogoAntiguedad> findByActivaCatalogoTrue(Pageable pageable);
}