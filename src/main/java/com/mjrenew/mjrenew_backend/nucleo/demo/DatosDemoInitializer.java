package com.mjrenew.mjrenew_backend.nucleo.demo;

import com.mjrenew.mjrenew_backend.nucleo.enums.DisponibilidadRestaurador;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoUsuario;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.nucleo.usuario.repository.UsuarioRepository;
import com.mjrenew.mjrenew_backend.restaurador.entity.PerfilRestaurador;
import com.mjrenew.mjrenew_backend.restaurador.repository.PerfilRestauradorRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * MJRENEW-FLUJO: semillas EXCLUSIVAS del perfil demo. No correr en producción.
 * Idempotente: no crea antigüedades ni modifica registros de terceros.
 * En el perfil demo restablece las credenciales de los tres correos publicados en login.html.
 */
@Component
@Profile("demo")
public class DatosDemoInitializer implements ApplicationRunner {
    private static final String DEMO_PASSWORD = "Test1234";
    private final UsuarioRepository usuarioRepository;
    private final PerfilRestauradorRepository perfilRepository;
    private final PasswordEncoder encoder;

    public DatosDemoInitializer(UsuarioRepository usuarioRepository,
                                PerfilRestauradorRepository perfilRepository,
                                PasswordEncoder encoder) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        crearOActualizarDemo("propietario1@test.mx", "Propietario Demo", TipoUsuario.PROPIETARIO);
        Usuario restaurador = crearOActualizarDemo("restaurador1@test.mx", "Restaurador Demo", TipoUsuario.RESTAURADOR);
        crearOActualizarDemo("comprador1@test.mx", "Comprador Demo", TipoUsuario.COMPRADOR);

        PerfilRestaurador perfil = perfilRepository.findByRestaurador_UsuariosId(restaurador.getUsuariosId())
                .orElseGet(PerfilRestaurador::new);
        perfil.setRestaurador(restaurador);
        if (perfil.getEspecialidadRestaurador() == null || perfil.getEspecialidadRestaurador().isBlank()) {
            perfil.setEspecialidadRestaurador("Restauración de muebles antiguos");
        }
        if (perfil.getAnosExperienciaRestaurador() == null) perfil.setAnosExperienciaRestaurador((short) 8);
        if (perfil.getDescripcionBioRestaurador() == null) {
            perfil.setDescripcionBioRestaurador("Perfil de pruebas para el flujo de restauración MJ Renew.");
        }
        perfil.setAprobadoPorAdminRestaurador(true);
        perfil.setDisponibilidadRestaurador(DisponibilidadRestaurador.DISPONIBLE);
        perfil.setActualizadoEnRestaurador(OffsetDateTime.now());
        perfilRepository.save(perfil);
    }

    private Usuario crearOActualizarDemo(String correo, String nombre, TipoUsuario rol) {
        Usuario usuario = usuarioRepository.findByCorreoElectronicoUsuario(correo).orElseGet(Usuario::new);
        if (usuario.getUsuariosId() != null && usuario.getTipoUsuario() != rol) {
            throw new IllegalStateException("Cuenta demo '" + correo + "' registrada con un rol diferente: "
                    + usuario.getTipoUsuario());
        }
        if (usuario.getUsuariosId() == null) {
            usuario.setCorreoElectronicoUsuario(correo);
            usuario.setNombreCompletoUsuario(nombre);
            usuario.setTipoUsuario(rol);
            usuario.setRegistradoEnUsuario(OffsetDateTime.now());
        }
        // En el perfil demo identificamos claramente los tres usuarios, sin tocar terceros.
        usuario.setNombreCompletoUsuario(nombre);
        // Solo estas direcciones explícitas se restablecen, y SOLO con perfil demo activo.
        usuario.setContrasenaHashUsuario(encoder.encode(DEMO_PASSWORD));
        usuario.setCorreoVerificadoUsuario(true);
        usuario.setActivoUsuario(true);
        return usuarioRepository.save(usuario);
    }
}
