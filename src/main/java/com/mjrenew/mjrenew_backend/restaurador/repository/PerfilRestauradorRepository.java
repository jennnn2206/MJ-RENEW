package com.mjrenew.mjrenew_backend.restaurador.repository;

import com.mjrenew.mjrenew_backend.restaurador.entity.PerfilRestaurador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PerfilRestauradorRepository extends JpaRepository<PerfilRestaurador, UUID> {
}
