package com.mjrenew.mjrenew_backend.transportista.repository;

import com.mjrenew.mjrenew_backend.nucleo.enums.ResultadoTraslado;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTraslado;
import com.mjrenew.mjrenew_backend.transportista.entity.TrasladoAntiguedad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrasladoAntiguedadRepository extends JpaRepository<TrasladoAntiguedad, UUID> {

    Page<TrasladoAntiguedad> findByTransportista_UsuariosId(UUID transportistaId, Pageable pageable);

    /*
     * Evita agendar dos traslados del mismo tipo para la misma antigüedad
     * (p. ej. dos RECOLECCION) mientras uno siga PENDIENTE o ya sea EXITOSO.
     * Un intento cerrado como NADIE_EN_CASA no bloquea: se puede reagendar.
     */
    boolean existsByAntiguedad_AntiguedadesIdAndTipoTrasladoAndResultadoTrasladoIn(
            UUID antiguedadId,
            TipoTraslado tipoTraslado,
            Collection<ResultadoTraslado> resultados
    );

    // Consulta por lote para obtener la entrega más reciente de cada compra del listado.
    List<TrasladoAntiguedad> findByAntiguedad_AntiguedadesIdInAndTipoTrasladoOrderByAgendadoEnTrasladoDesc(
            Collection<UUID> antiguedadesId, TipoTraslado tipoTraslado);

    // Intentos previos del mismo tipo, para numerar el siguiente (#2, #3...).
    long countByAntiguedad_AntiguedadesIdAndTipoTraslado(
            UUID antiguedadId,
            TipoTraslado tipoTraslado
    );

    // Se acredita la entrega real de una venta mediante su traslado exitoso.
    Optional<TrasladoAntiguedad> findFirstByAntiguedad_AntiguedadesIdAndTipoTrasladoAndResultadoTrasladoOrderByEjecutadoEnTrasladoDesc(
            UUID antiguedadId, TipoTraslado tipoTraslado, ResultadoTraslado resultadoTraslado);

    // Último traslado agendado de la pieza (para avisar al propietario si falló).
    Optional<TrasladoAntiguedad> findFirstByAntiguedad_AntiguedadesIdOrderByAgendadoEnTrasladoDesc(UUID antiguedadId);
}