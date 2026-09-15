package com.mjrenew.mjrenew_backend.repository;

import com.mjrenew.mjrenew_backend.entity.Cotizacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CotizacionRepository extends JpaRepository<Cotizacion, UUID> {
}
