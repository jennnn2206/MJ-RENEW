package com.mjrenew.mjrenew_backend.nucleo.auth.service;

import com.mjrenew.mjrenew_backend.nucleo.auth.config.RecuperacionContrasenaProperties;
import com.mjrenew.mjrenew_backend.nucleo.auth.entity.RecuperacionContrasena;
import com.mjrenew.mjrenew_backend.nucleo.auth.repository.RecuperacionContrasenaRepository;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.OffsetDateTime;

@Service
public class RecuperacionContrasenaService {
    private final UsuarioRepository usuarios;
    private final RecuperacionContrasenaRepository recuperaciones;
    private final TokenCuentaService tokens;
    private final PasswordEncoder encoder;
    private final ApplicationEventPublisher eventos;
    private final RecuperacionContrasenaProperties properties;
    private final Clock clock;

    public RecuperacionContrasenaService(UsuarioRepository usuarios, RecuperacionContrasenaRepository recuperaciones,
                                        TokenCuentaService tokens, PasswordEncoder encoder,
                                        ApplicationEventPublisher eventos, RecuperacionContrasenaProperties properties,
                                        @Qualifier("recuperacionContrasenaClock") Clock clock) {
        this.usuarios = usuarios;
        this.recuperaciones = recuperaciones;
        this.tokens = tokens;
        this.encoder = encoder;
        this.eventos = eventos;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public void solicitar(String correo) {
        var id = usuarios.buscarIdPorCorreo(correo);
        if (id.isEmpty()) return;
        // El mismo bloqueo ordena solicitud, reenvío y consumo para este usuario.
        var usuario = usuarios.bloquearPorId(id.get()).orElseThrow();
        if (!Boolean.TRUE.equals(usuario.getActivoUsuario())) return;
        var anterior = recuperaciones.findById(id.get());
        var ahora = OffsetDateTime.now(clock);
        if (anterior.isPresent() && anterior.get().getEmitidoEn().plusMinutes(1).isAfter(ahora)) return;
        String token = tokens.generar();
        var registro = anterior.orElseGet(RecuperacionContrasena::new);
        registro.setUsuarioId(id.get());
        registro.setTokenHash(tokens.hash(token));
        registro.setEmitidoEn(ahora);
        registro.setExpiraEn(ahora.plusMinutes(properties.vigenciaMinutos()));
        recuperaciones.save(registro);
        eventos.publishEvent(new RecuperacionContrasenaEmitida(correo, token, properties.vigenciaMinutos()));
    }

    @Transactional
    public void restablecer(String token, String nuevaContrasena) {
        // BCrypt solo admite 72 bytes; evitar truncamiento o un error inesperado del encoder.
        if (nuevaContrasena.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new SolicitudInvalidaException("La contraseña es demasiado larga. Usa una más corta.");
        }
        String hash = tokens.hash(token);
        var encontrado = recuperaciones.findByTokenHash(hash).orElseThrow(this::enlaceInvalido);
        var usuario = usuarios.bloquearPorId(encontrado.getUsuarioId()).orElseThrow(this::enlaceInvalido);
        // Consultar después del bloqueo evita aceptar un token reemplazado o consumido mientras esperábamos.
        var actual = recuperaciones.findByTokenHash(hash).orElseThrow(this::enlaceInvalido);
        if (!actual.getExpiraEn().isAfter(OffsetDateTime.now(clock))
                || !Boolean.TRUE.equals(usuario.getActivoUsuario())) throw enlaceInvalido();
        usuario.setContrasenaHashUsuario(encoder.encode(nuevaContrasena));
        recuperaciones.delete(actual);
        // Los indicadores de cuenta activa y correo verificado conservan su valor.
    }

    private SolicitudInvalidaException enlaceInvalido() {
        return new SolicitudInvalidaException("El enlace no es válido o ya venció. Solicita uno nuevo.");
    }
}
