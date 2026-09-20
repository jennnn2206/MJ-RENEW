package com.mjrenew.mjrenew_backend.transportista.repository;

import com.mjrenew.mjrenew_backend.transportista.entity.TrasladoAntiguedad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TrasladoAntiguedadRepository extends JpaRepository<TrasladoAntiguedad, UUID> {

    Page<TrasladoAntiguedad> findByTransportista_UsuariosId(UUID transportistaId, Pageable pageable);
}