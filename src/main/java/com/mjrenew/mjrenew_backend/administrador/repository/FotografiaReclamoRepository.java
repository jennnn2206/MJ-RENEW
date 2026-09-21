package com.mjrenew.mjrenew_backend.administrador.repository;

import com.mjrenew.mjrenew_backend.administrador.entity.FotografiaReclamo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FotografiaReclamoRepository extends JpaRepository<FotografiaReclamo, UUID> {

    List<FotografiaReclamo> findByDisputaAntiguedad_DisputasAntiguedadId(UUID disputaId);
}
