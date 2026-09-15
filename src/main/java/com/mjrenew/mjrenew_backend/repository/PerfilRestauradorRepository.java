package com.mjrenew.mjrenew_backend.repository;

import com.mjrenew.mjrenew_backend.entity.PerfilRestaurador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PerfilRestauradorRepository extends JpaRepository<PerfilRestaurador, UUID> {
}
