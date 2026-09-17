package com.mjrenew.mjrenew_backend.propietario.repository;

import com.mjrenew.mjrenew_backend.propietario.entity.FotografiaAntiguedad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FotografiaAntiguedadRepository extends JpaRepository<FotografiaAntiguedad, UUID> {
}
