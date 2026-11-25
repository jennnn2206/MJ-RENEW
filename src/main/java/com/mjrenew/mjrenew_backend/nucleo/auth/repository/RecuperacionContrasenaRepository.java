package com.mjrenew.mjrenew_backend.nucleo.auth.repository;

import com.mjrenew.mjrenew_backend.nucleo.auth.entity.RecuperacionContrasena;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface RecuperacionContrasenaRepository extends JpaRepository<RecuperacionContrasena, UUID> {
    Optional<RecuperacionContrasena> findByTokenHash(String tokenHash);
}
