package com.mjrenew.mjrenew_backend.repository;

import com.mjrenew.mjrenew_backend.entity.CatalogoAntiguedad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CatalogoAntiguedadRepository extends JpaRepository<CatalogoAntiguedad, UUID> {
}
