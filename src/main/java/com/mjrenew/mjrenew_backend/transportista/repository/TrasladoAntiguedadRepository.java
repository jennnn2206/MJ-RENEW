package com.mjrenew.mjrenew_backend.transportista.repository;

import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTraslado;
import com.mjrenew.mjrenew_backend.transportista.entity.TrasladoAntiguedad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TrasladoAntiguedadRepository extends JpaRepository<TrasladoAntiguedad, UUID> {

    Page<TrasladoAntiguedad> findByTransportista_UsuariosId(UUID transportistaId, Pageable pageable);

    /*
     * Evita agendar dos traslados del mismo tipo para la misma antigüedad
     * (p. ej. dos RECOLECCION). No filtra por resultado porque, mientras no
     * exista un endpoint de "reintentar traslado", un solo traslado por tipo
     * es la única posibilidad válida.
     */
    boolean existsByAntiguedad_AntiguedadesIdAndTipoTraslado(
            UUID antiguedadId,
            TipoTraslado tipoTraslado
    );
}