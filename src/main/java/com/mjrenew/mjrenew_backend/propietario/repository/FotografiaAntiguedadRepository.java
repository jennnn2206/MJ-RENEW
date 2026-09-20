package com.mjrenew.mjrenew_backend.propietario.repository;

import com.mjrenew.mjrenew_backend.nucleo.enums.TipoFotografia;
import com.mjrenew.mjrenew_backend.propietario.entity.FotografiaAntiguedad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FotografiaAntiguedadRepository extends JpaRepository<FotografiaAntiguedad, UUID> {

    Optional<FotografiaAntiguedad> findFirstByAntiguedad_AntiguedadesIdAndTipoFotografiaOrderBySubidaEnFotografiaAsc(
            UUID antiguedadId, TipoFotografia tipoFotografia);
}