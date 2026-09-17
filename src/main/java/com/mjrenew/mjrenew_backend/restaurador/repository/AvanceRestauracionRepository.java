package com.mjrenew.mjrenew_backend.restaurador.repository;

import com.mjrenew.mjrenew_backend.restaurador.entity.AvanceRestauracion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AvanceRestauracionRepository extends JpaRepository<AvanceRestauracion, UUID> {
}
