package com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository;

import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AntiguedadRepository extends JpaRepository<Antiguedad, UUID> {

    List<Antiguedad> findByPropietario_UsuariosId(UUID propietarioId);
}
