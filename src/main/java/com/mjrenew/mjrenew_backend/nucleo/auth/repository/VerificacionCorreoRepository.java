package com.mjrenew.mjrenew_backend.nucleo.auth.repository;

import com.mjrenew.mjrenew_backend.nucleo.auth.entity.VerificacionCorreo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface VerificacionCorreoRepository extends JpaRepository<VerificacionCorreo, UUID> {
    Optional<VerificacionCorreo> findByTokenHash(String tokenHash);
}
