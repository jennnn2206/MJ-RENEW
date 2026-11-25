package com.mjrenew.mjrenew_backend.nucleo.notificacion.service;

import com.mjrenew.mjrenew_backend.nucleo.notificacion.EstadoCorreoNotificacion;
import com.mjrenew.mjrenew_backend.nucleo.notificacion.dto.EnvioNotificacion;
import com.mjrenew.mjrenew_backend.nucleo.notificacion.repository.NotificacionRepository;
import com.mjrenew.mjrenew_backend.nucleo.usuario.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class ColaNotificacionService {
    private final NotificacionRepository notificaciones;
    private final UsuarioRepository usuarios;
    private final Clock clock;

    public ColaNotificacionService(NotificacionRepository notificaciones, UsuarioRepository usuarios,
                                   @Qualifier("notificacionesClock") Clock clock) {
        this.notificaciones = notificaciones; this.usuarios = usuarios; this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<EnvioNotificacion> reservar() {
        var ahora = OffsetDateTime.now(clock);
        var candidato = notificaciones.reservarSiguiente(ahora);
        if (candidato.isEmpty()) return Optional.empty();
        var n = candidato.get();
        var destinatario = usuarios.findById(n.getDestinatarioId()).orElseThrow();
        if (!Boolean.TRUE.equals(destinatario.getActivoUsuario())
                || !Boolean.TRUE.equals(destinatario.getCorreoVerificadoUsuario())) {
            n.setEstadoCorreo(EstadoCorreoNotificacion.PENDIENTE);
            n.setReservaId(null); n.setReservadoHasta(null);
            n.setProximoIntentoEn(ahora.plusMinutes(5));
            return Optional.empty();
        }
        n.setEstadoCorreo(EstadoCorreoNotificacion.ENVIANDO);
        n.setReservaId(UUID.randomUUID());
        n.setReservadoHasta(ahora.plusMinutes(2));
        n.setIntentosCorreo(n.getIntentosCorreo() + 1);
        return Optional.of(new EnvioNotificacion(n.getId(), n.getReservaId(), destinatario.getCorreoElectronicoUsuario(),
                n.getTitulo(), n.getMensaje() + "\nAntigüedad: " + n.getAntiguedadId(), n.getTipo().rutaPanel()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void concluir(EnvioNotificacion envio, boolean enviado) {
        var encontrado = notificaciones.bloquearPorId(envio.notificacionId());
        if (encontrado.isEmpty()) return;
        var n = encontrado.get();
        // Un trabajador antiguo no puede sobrescribir una reserva adquirida tras vencer su lease.
        if (n.getEstadoCorreo() != EstadoCorreoNotificacion.ENVIANDO || !envio.reservaId().equals(n.getReservaId())) return;
        var ahora = OffsetDateTime.now(clock);
        n.setReservaId(null); n.setReservadoHasta(null);
        n.setEstadoCorreo(enviado ? EstadoCorreoNotificacion.ENVIADO : EstadoCorreoNotificacion.PENDIENTE);
        if (enviado) n.setCorreoEnviadoEn(ahora);
        else n.setProximoIntentoEn(ahora.plusMinutes(1));
    }
}
