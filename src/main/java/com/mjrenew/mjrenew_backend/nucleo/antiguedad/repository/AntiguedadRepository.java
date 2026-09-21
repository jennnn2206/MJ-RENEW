package com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository;

import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AntiguedadRepository extends JpaRepository<Antiguedad, UUID> {

    Page<Antiguedad> findByPropietario_UsuariosId(UUID propietarioId, Pageable pageable);


    //pendiente
    Page<Antiguedad> findByRestaurador_UsuariosIdAndEstadoActualAntiguedad(
            UUID restauradorId,
            EstadoAntiguedad estado,
            Pageable pageable
    );
}