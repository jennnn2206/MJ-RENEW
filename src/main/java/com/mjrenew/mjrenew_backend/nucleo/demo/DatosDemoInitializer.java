package com.mjrenew.mjrenew_backend.nucleo.demo;

import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import com.mjrenew.mjrenew_backend.catalogo.repository.CatalogoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository.AntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.enums.DisponibilidadRestaurador;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstiloMueble;
import com.mjrenew.mjrenew_backend.nucleo.enums.MaterialMueble;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoFotografia;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoMueble;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoUsuario;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.nucleo.usuario.repository.UsuarioRepository;
import com.mjrenew.mjrenew_backend.propietario.entity.Dimension;
import com.mjrenew.mjrenew_backend.propietario.entity.FotografiaAntiguedad;
import com.mjrenew.mjrenew_backend.propietario.repository.DimensionRepository;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.restaurador.entity.PerfilRestaurador;
import com.mjrenew.mjrenew_backend.restaurador.repository.PerfilRestauradorRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * MJRENEW-FLUJO: semillas EXCLUSIVAS del perfil demo. No correr en producción.
 * Idempotente: crea las muestras una sola vez y no modifica registros de terceros.
 * En el perfil demo restablece las credenciales publicadas en login.html y
 * prepara piezas ficticias para recorrer los paneles y el catálogo.
 */
@Component
@Profile("demo")
public class DatosDemoInitializer implements ApplicationRunner {
    private static final String DEMO_PASSWORD = "Test1234";
    private static final String ADMIN_PASSWORD = "Admin1234";
    private final UsuarioRepository usuarioRepository;
    private final PerfilRestauradorRepository perfilRepository;
    private final AntiguedadRepository antiguedadRepository;
    private final CatalogoAntiguedadRepository catalogoRepository;
    private final FotografiaAntiguedadRepository fotografiaRepository;
    private final DimensionRepository dimensionRepository;
    private final PasswordEncoder encoder;

    public DatosDemoInitializer(UsuarioRepository usuarioRepository,
                                PerfilRestauradorRepository perfilRepository,
                                AntiguedadRepository antiguedadRepository,
                                CatalogoAntiguedadRepository catalogoRepository,
                                FotografiaAntiguedadRepository fotografiaRepository,
                                DimensionRepository dimensionRepository,
                                PasswordEncoder encoder) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.antiguedadRepository = antiguedadRepository;
        this.catalogoRepository = catalogoRepository;
        this.fotografiaRepository = fotografiaRepository;
        this.dimensionRepository = dimensionRepository;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Usuario propietario = crearOActualizarDemo("propietario1@test.mx", "Propietario Demo", TipoUsuario.PROPIETARIO);
        Usuario restaurador = crearOActualizarDemo("restaurador1@test.mx", "Restaurador Demo", TipoUsuario.RESTAURADOR);
        crearOActualizarDemo("comprador1@test.mx", "Comprador Demo", TipoUsuario.COMPRADOR);
        crearOActualizarDemo("transportista1@test.mx", "Transportista Demo", TipoUsuario.TRANSPORTISTA);
        crearOActualizarDemo("admin@mjrenew.mx", "Administrador Demo", TipoUsuario.ADMINISTRADOR, ADMIN_PASSWORD);

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

        Antiguedad escritorio = crearPiezaDemo(propietario, null,
                "DEMO: escritorio colonial", TipoMueble.ESCRITORIO,
                EstiloMueble.COLONIAL, MaterialMueble.CEDRO,
                EstadoAntiguedad.EN_CATALOGO,
                "Pieza ficticia de demostración con herrajes de bronce y acabado restaurado.",
                "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=600&q=80",
                140, 70, 80);
        publicarEnCatalogoDemo(escritorio, BigDecimal.valueOf(38000));

        Antiguedad vitrina = crearPiezaDemo(propietario, null,
                "DEMO: vitrina victoriana", TipoMueble.VITRINA,
                EstiloMueble.VICTORIANO, MaterialMueble.NOGAL,
                EstadoAntiguedad.EN_CATALOGO,
                "Pieza ficticia de demostración en nogal con vidrio biselado.",
                "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=600&q=80",
                90, 45, 195);
        publicarEnCatalogoDemo(vitrina, BigDecimal.valueOf(52000));

        crearPiezaDemo(propietario, restaurador,
                "DEMO: sillón en evaluación", TipoMueble.SILLON,
                EstiloMueble.LUIS_XV, MaterialMueble.NOGAL,
                EstadoAntiguedad.EN_EVALUACION,
                "Pieza ficticia de demostración pendiente de evaluación por el restaurador.",
                "https://images.unsplash.com/photo-1567538096630-e0c55bd6374c?w=600&q=80",
                70, 65, 95);
    }

    private Antiguedad crearPiezaDemo(Usuario propietario, Usuario restaurador,
                                     String procedencia, TipoMueble tipo,
                                     EstiloMueble estilo, MaterialMueble material,
                                     EstadoAntiguedad estado, String descripcion,
                                     String fotoUrl, int alto, int ancho, int profundidad) {
        return antiguedadRepository
                .findByPropietario_UsuariosIdAndProcedenciaMueble(propietario.getUsuariosId(), procedencia)
                .orElseGet(() -> {
                    OffsetDateTime ahora = OffsetDateTime.now();
                    Antiguedad pieza = new Antiguedad();
                    pieza.setPropietario(propietario);
                    pieza.setRestaurador(restaurador);
                    pieza.setProcedenciaMueble(procedencia);
                    pieza.setTipoMueble(tipo);
                    pieza.setEstiloMueble(estilo);
                    pieza.setMaterialMueble(material);
                    pieza.setEstadoActualAntiguedad(estado);
                    pieza.setDescripcionDaniosAntiguedad(descripcion);
                    pieza.setRegistradaEnAntiguedad(ahora);
                    Antiguedad guardada = antiguedadRepository.save(pieza);

                    FotografiaAntiguedad fotografia = new FotografiaAntiguedad();
                    fotografia.setAntiguedad(guardada);
                    fotografia.setTipoFotografia(TipoFotografia.ESTADO_INICIAL);
                    fotografia.setUrlAlmacenFotografia(fotoUrl);
                    fotografia.setSubidaEnFotografia(ahora);
                    fotografiaRepository.save(fotografia);

                    Dimension dimension = new Dimension();
                    dimension.setAntiguedad(guardada);
                    dimension.setAltoCmAntiguedad(BigDecimal.valueOf(alto));
                    dimension.setAnchoCmAntiguedad(BigDecimal.valueOf(ancho));
                    dimension.setProfundidadCmAntiguedad(BigDecimal.valueOf(profundidad));
                    dimension.setRegistradasEnDimensiones(ahora);
                    dimensionRepository.save(dimension);
                    return guardada;
                });
    }

    private void publicarEnCatalogoDemo(Antiguedad pieza, BigDecimal precio) {
        UUID antiguedadId = pieza.getAntiguedadesId();
        if (catalogoRepository.existsByAntiguedad_AntiguedadesId(antiguedadId)) return;

        CatalogoAntiguedad catalogo = new CatalogoAntiguedad();
        catalogo.setAntiguedad(pieza);
        catalogo.setPrecioMxnCatalogo(precio);
        catalogo.setPublicadaEnCatalogo(OffsetDateTime.now());
        catalogo.setExpiraEnCatalogo(OffsetDateTime.now().plusDays(90));
        catalogo.setActivaCatalogo(true);
        catalogoRepository.save(catalogo);
    }

    private Usuario crearOActualizarDemo(String correo, String nombre, TipoUsuario rol) {
        return crearOActualizarDemo(correo, nombre, rol, DEMO_PASSWORD);
    }

    private Usuario crearOActualizarDemo(String correo, String nombre, TipoUsuario rol, String contrasena) {
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
        usuario.setContrasenaHashUsuario(encoder.encode(contrasena));
        usuario.setCorreoVerificadoUsuario(true);
        usuario.setActivoUsuario(true);
        return usuarioRepository.save(usuario);
    }
}
