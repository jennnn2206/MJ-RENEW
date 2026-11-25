package com.mjrenew.mjrenew_backend.nucleo.notificacion.service;

import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.notificacion.*;
import com.mjrenew.mjrenew_backend.nucleo.notificacion.dto.NotificacionResponse;
import com.mjrenew.mjrenew_backend.nucleo.notificacion.mapper.NotificacionMapper;
import com.mjrenew.mjrenew_backend.nucleo.notificacion.repository.NotificacionRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class NotificacionService {
    private final NotificacionRepository notificaciones;
    private final NotificacionMapper mapper;
    private final Clock clock;

    public NotificacionService(NotificacionRepository notificaciones, NotificacionMapper mapper,
                               @Qualifier("notificacionesClock") Clock clock) {
        this.notificaciones = notificaciones; this.mapper = mapper; this.clock = clock;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(NotificacionRestauracionEmitida evento) {
        notificaciones.insertarSiFalta(UUID.randomUUID(), evento.destinatarioId(), evento.antiguedadId(),
                evento.referenciaId(), evento.tipo().name(), evento.tipo().titulo(), evento.tipo().mensaje(), OffsetDateTime.now(clock));
    }

    @Transactional(readOnly = true)
    public Page<NotificacionResponse> obtenerPropias(UUID usuarioId, boolean soloNoLeidas, Pageable pageable) {
        var pagina = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 50),
                Sort.by(Sort.Direction.DESC, "creadoEn", "id"));
        return (soloNoLeidas ? notificaciones.findByDestinatarioIdAndLeidoEnIsNull(usuarioId, pagina)
                : notificaciones.findByDestinatarioId(usuarioId, pagina)).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(UUID usuarioId) { return notificaciones.countByDestinatarioIdAndLeidoEnIsNull(usuarioId); }

    @Transactional
    public NotificacionResponse marcarLeida(UUID id, UUID usuarioId) {
        var notificacion = notificaciones.bloquearPropia(id, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la notificación"));
        if (notificacion.getLeidoEn() == null) notificacion.setLeidoEn(OffsetDateTime.now(clock));
        return mapper.toResponse(notificacion);
    }
}
