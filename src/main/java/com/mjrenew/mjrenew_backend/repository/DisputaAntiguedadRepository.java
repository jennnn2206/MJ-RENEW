package com.mjrenew.mjrenew_backend.repository;

import com.mjrenew.mjrenew_backend.entity.DisputaAntiguedad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DisputaAntiguedadRepository extends JpaRepository<DisputaAntiguedad, UUID> {
}
