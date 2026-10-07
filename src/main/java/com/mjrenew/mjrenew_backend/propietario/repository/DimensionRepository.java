package com.mjrenew.mjrenew_backend.propietario.repository;

import com.mjrenew.mjrenew_backend.propietario.entity.Dimension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DimensionRepository extends JpaRepository<Dimension, UUID> {

    Optional<Dimension> findByAntiguedad_AntiguedadesId(UUID antiguedadId);
}