package com.mjrenew.mjrenew_backend.administrador.repository;

import com.mjrenew.mjrenew_backend.administrador.entity.DisputaAntiguedad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DisputaAntiguedadRepository extends JpaRepository<DisputaAntiguedad, UUID> {
}
