package com.mjrenew.mjrenew_backend.nucleo.auth.service;

import com.mjrenew.mjrenew_backend.nucleo.auth.entity.VerificacionCorreo;
import com.mjrenew.mjrenew_backend.nucleo.auth.repository.VerificacionCorreoRepository;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.nucleo.usuario.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class VerificacionCorreoService {
    private final UsuarioRepository usuarios;
    private final VerificacionCorreoRepository verificaciones;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public VerificacionCorreoService(UsuarioRepository usuarios, VerificacionCorreoRepository verificaciones,
                                    ApplicationEventPublisher eventos,
                                    @Qualifier("verificacionCorreoClock") Clock clock) {
        this.usuarios = usuarios;
        this.verificaciones = verificaciones;
        this.eventos = eventos;
        this.clock = clock;
    }

    /** Participa en la misma transacción que crea la cuenta. */
    @Transactional
    public void emitirParaRegistro(Usuario usuario) {
        emitir(usuario);
    }

    @Transactional
    public void reenviar(String correo) {
        var encontrado = usuarios.buscarIdPorCorreo(correo);
        if (encontrado.isEmpty()) return;
        var usuario = usuarios.bloquearPorId(encontrado.get()).orElseThrow();
        if (!Boolean.TRUE.equals(usuario.getActivoUsuario())
                || Boolean.TRUE.equals(usuario.getCorreoVerificadoUsuario())) return;
        var anterior = verificaciones.findById(usuario.getUsuariosId());
        // Límite técnico contra envíos repetidos, incluso desde solicitudes concurrentes.
        if (anterior.isPresent() && anterior.get().getEmitidoEn().plusMinutes(1).isAfter(ahora())) return;
        emitir(usuario);
    }

    @Transactional
    public void verificar(String token) {
        String hash = hash(token);
        var encontrado = verificaciones.findByTokenHash(hash).orElseThrow(this::enlaceInvalido);
        var usuario = usuarios.bloquearPorId(encontrado.getUsuarioId()).orElseThrow(this::enlaceInvalido);
        // Volver a consultar después del bloqueo: otro hilo pudo reenviar o consumir el enlace.
        var actual = verificaciones.findByTokenHash(hash).orElseThrow(this::enlaceInvalido);
        if (!actual.getExpiraEn().isAfter(ahora()) || !Boolean.TRUE.equals(usuario.getActivoUsuario())
                || Boolean.TRUE.equals(usuario.getCorreoVerificadoUsuario())) throw enlaceInvalido();
        usuario.setCorreoVerificadoUsuario(true);
        verificaciones.delete(actual);
    }

    private void emitir(Usuario usuario) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var registro = verificaciones.findById(usuario.getUsuariosId()).orElseGet(VerificacionCorreo::new);
        registro.setUsuarioId(usuario.getUsuariosId());
        registro.setTokenHash(hash(token));
        registro.setEmitidoEn(ahora());
        registro.setExpiraEn(registro.getEmitidoEn().plusHours(24));
        verificaciones.save(registro);
        eventos.publishEvent(new VerificacionCorreoEmitida(usuario.getCorreoElectronicoUsuario(), token));
    }

    private OffsetDateTime ahora() { return OffsetDateTime.now(clock); }

    private SolicitudInvalidaException enlaceInvalido() {
        return new SolicitudInvalidaException("El enlace no es válido o ya venció. Solicita uno nuevo.");
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible", ex);
        }
    }
}
