package com.mjrenew.mjrenew_backend.repository;

import com.mjrenew.mjrenew_backend.entity.FotografiaAntiguedad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FotografiaAntiguedadRepository extends JpaRepository<FotografiaAntiguedad, UUID> {
}
