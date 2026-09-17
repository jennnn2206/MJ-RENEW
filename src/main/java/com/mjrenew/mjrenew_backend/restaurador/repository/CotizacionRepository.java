package com.mjrenew.mjrenew_backend.restaurador.repository;

import com.mjrenew.mjrenew_backend.restaurador.entity.Cotizacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CotizacionRepository extends JpaRepository<Cotizacion, UUID> {
}
