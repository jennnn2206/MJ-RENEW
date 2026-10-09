package com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository;

import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AntiguedadRepository
        extends JpaRepository<Antiguedad, UUID> {

    Page<Antiguedad> findByPropietario_UsuariosId(
            UUID propietarioId,
            Pageable pageable
    );

    Optional<Antiguedad>
    findByAntiguedadesIdAndPropietario_UsuariosId(
            UUID antiguedadId,
            UUID propietarioId
    );

    Page<Antiguedad>
    findByRestaurador_UsuariosIdAndEstadoActualAntiguedad(
            UUID restauradorId,
            EstadoAntiguedad estado,
            Pageable pageable
    );

    // MJRENEW-FLUJO: permite al restaurador ver q1 y q2 tras evaluar,
    // sin perder el expediente si la cotización falla.
    Page<Antiguedad> findByRestaurador_UsuariosIdAndEstadoActualAntiguedadIn(
            UUID restauradorId, java.util.Collection<EstadoAntiguedad> estados, Pageable pageable);

    Page<Antiguedad> findByRestaurador_UsuariosId(UUID restauradorId, Pageable pageable);

    Optional<Antiguedad> findByAntiguedadesIdAndRestaurador_UsuariosId(
            UUID antiguedadId,
            UUID restauradorId
    );
}