package com.mjrenew.mjrenew_backend.nucleo.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mjrenew.mjrenew_backend.nucleo.auth.repository.RecuperacionContrasenaRepository;
import com.mjrenew.mjrenew_backend.nucleo.auth.repository.VerificacionCorreoRepository;
import com.mjrenew.mjrenew_backend.nucleo.auth.service.*;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoUsuario;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.nucleo.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"mjrenew.stripe.transfers-enabled=false",
        "mjrenew.stripe.fee-reconciliation-enabled=false", "mjrenew.stripe.reconciliation-enabled=false",
        "app.base-url=https://mjrenew.example", "mjrenew.auth.recuperacion.vigencia-minutos=30"})
@AutoConfigureMockMvc
class RecuperacionContrasenaIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired RecuperacionContrasenaRepository recuperaciones;
    @Autowired VerificacionCorreoRepository verificaciones;
    @Autowired RecuperacionContrasenaService servicio;
    @Autowired VerificacionCorreoService verificacion;
    @Autowired TokenCuentaService tokens;
    @Autowired PasswordEncoder encoder;
    @Autowired TransactionTemplate tx;
    @MockitoBean JavaMailSender correo;
    @MockitoBean(name = "recuperacionContrasenaClock") Clock clock;
    private final Queue<SimpleMailMessage> enviados = new ConcurrentLinkedQueue<>();
    private final List<UUID> creados = new ArrayList<>();
    private final Instant inicio = Instant.parse("2026-11-25T15:00:00Z");
    private String email;
    private UUID usuarioId;

    @BeforeEach
    void preparar() {
        email = "recuperacion-" + UUID.randomUUID() + "@example.invalid";
        usuarioId = crearUsuario(email);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        avanzar(Duration.ZERO);
        doAnswer(invocacion -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive(), "SMTP fuera de transacción");
            enviados.add(new SimpleMailMessage(invocacion.getArgument(0, SimpleMailMessage.class)));
            return null;
        }).when(correo).send(any(SimpleMailMessage.class));
    }

    @AfterEach
    void limpiarSoloLosDatosCreados() {
        tx.executeWithoutResult(estado -> {
            for (UUID id : creados) {
                recuperaciones.deleteById(id);
                verificaciones.deleteById(id);
            }
            recuperaciones.flush();
            verificaciones.flush();
            usuarios.deleteAllByIdInBatch(creados);
        });
    }

    @Test
    void solicitudGuardaHashVigenciaLimitadaYEnviaDespuesDelCommitSinAutenticar() throws Exception {
        var respuesta = solicitar(email).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store")).andReturn();
        var registro = recuperaciones.findById(usuarioId).orElseThrow();
        assertEquals(tokens.hash(token()), registro.getTokenHash());
        assertNotEquals(token(), registro.getTokenHash());
        assertEquals(inicio, registro.getEmitidoEn().toInstant());
        assertEquals(inicio.plus(Duration.ofMinutes(30)), registro.getExpiraEn().toInstant());
        var mensaje = enviados.element();
        assertEquals(email, mensaje.getTo()[0]);
        assertTrue(mensaje.getText().contains("https://mjrenew.example/restablecer-contrasena#token="));
        assertTrue(mensaje.getText().contains("30 minutos"));
        assertNull(respuesta.getRequest().getSession(false));
        assertTrue(encoder.matches("Password123", usuario().getContrasenaHashUsuario()));
    }

    @Test
    void restablecerCambiaElHashPermiteNuevoLoginYConsumeElToken() throws Exception {
        solicitar(email).andExpect(status().isOk());
        String enlace = token();
        var respuesta = restablecer(enlace, "NuevaPassword456").andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store")).andReturn();
        assertNull(respuesta.getRequest().getSession(false));
        assertFalse(recuperaciones.existsById(usuarioId));
        assertTrue(encoder.matches("NuevaPassword456", usuario().getContrasenaHashUsuario()));
        assertTrue(usuario().getCorreoVerificadoUsuario());
        assertTrue(usuario().getActivoUsuario());
        restablecer(enlace, "OtraPassword789").andExpect(status().isBadRequest());
        login(email, "Password123").andExpect(status().isUnauthorized());
        var nuevo = login(email, "NuevaPassword456").andExpect(status().isOk()).andReturn();
        mvc.perform(get("/api/auth/obtenerUsuarioActual").session((MockHttpSession) nuevo.getRequest().getSession(false)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.correoElectronico").value(email));
    }

    @Test
    void invalidaTodasLasSesionesAnterioresSinAfectarOtraCuenta() throws Exception {
        var primera = sesion(email);
        var segunda = sesion(email);
        String otroCorreo = "otra-" + UUID.randomUUID() + "@example.invalid";
        crearUsuario(otroCorreo);
        var otra = sesion(otroCorreo);
        mvc.perform(get("/api/auth/obtenerUsuarioActual").session(primera)).andExpect(status().isOk());
        solicitar(email).andExpect(status().isOk());
        restablecer(token(), "NuevaPassword456").andExpect(status().isOk());
        mvc.perform(get("/api/auth/obtenerUsuarioActual").session(primera)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/obtenerUsuarioActual").session(segunda)).andExpect(status().isUnauthorized());
        assertTrue(primera.isInvalid());
        assertTrue(segunda.isInvalid());
        mvc.perform(get("/api/auth/obtenerUsuarioActual").session(otra)).andExpect(status().isOk());
    }

    @Test
    void respuestaGenericaNoDistingueDesconocidoInactivoOPendiente() throws Exception {
        String desconocido = solicitar("inexistente@example.invalid").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(desconocido, solicitar(email).andReturn().getResponse().getContentAsString());
        assertEquals(desconocido, solicitar(email).andReturn().getResponse().getContentAsString());
        tx.executeWithoutResult(estado -> usuarios.bloquearPorId(usuarioId).orElseThrow().setActivoUsuario(false));
        avanzar(Duration.ofMinutes(2));
        assertEquals(desconocido, solicitar(email).andReturn().getResponse().getContentAsString());
        assertEquals(1, enviados.size());
    }

    @Test
    void cuentaSinVerificarRecuperaContrasenaPeroSigueNecesitandoVerificacion() throws Exception {
        tx.executeWithoutResult(estado -> usuarios.bloquearPorId(usuarioId).orElseThrow().setCorreoVerificadoUsuario(false));
        solicitar(email).andExpect(status().isOk());
        restablecer(token(), "NuevaPassword456").andExpect(status().isOk());
        assertFalse(usuario().getCorreoVerificadoUsuario());
        login(email, "NuevaPassword456").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("CORREO_NO_VERIFICADO"));
    }

    @Test
    void cuentaDesactivadaNoSeReactivaMedianteUnEnlaceAnterior() throws Exception {
        solicitar(email).andExpect(status().isOk());
        tx.executeWithoutResult(estado -> usuarios.bloquearPorId(usuarioId).orElseThrow().setActivoUsuario(false));
        restablecer(token(), "NuevaPassword456").andExpect(status().isBadRequest());
        assertFalse(usuario().getActivoUsuario());
        assertTrue(encoder.matches("Password123", usuario().getContrasenaHashUsuario()));
    }

    @Test
    void enlaceVenceExactamenteALosTreintaMinutos() throws Exception {
        solicitar(email).andExpect(status().isOk());
        avanzar(Duration.ofMinutes(30));
        restablecer(token(), "NuevaPassword456").andExpect(status().isBadRequest());
        assertTrue(encoder.matches("Password123", usuario().getContrasenaHashUsuario()));
    }

    @Test
    void enlaceFuncionaJustoAntesDelVencimiento() throws Exception {
        solicitar(email).andExpect(status().isOk());
        avanzar(Duration.ofMinutes(30).minusSeconds(1));
        restablecer(token(), "NuevaPassword456").andExpect(status().isOk());
    }

    @Test
    void nuevaSolicitudRenuevaVigenciaEInvalidaElEnlaceAnterior() throws Exception {
        solicitar(email).andExpect(status().isOk());
        String anterior = token();
        solicitar(email).andExpect(status().isOk());
        assertEquals(1, enviados.size());
        avanzar(Duration.ofMinutes(1));
        solicitar(email).andExpect(status().isOk());
        assertEquals(2, enviados.size());
        String nuevo = token();
        assertNotEquals(anterior, nuevo);
        restablecer(anterior, "NuevaPassword456").andExpect(status().isBadRequest());
        avanzar(Duration.ofMinutes(30));
        restablecer(nuevo, "NuevaPassword456").andExpect(status().isOk());
    }

    @Test
    void tokensDeVerificacionYRecuperacionNoSePuedenIntercambiar() throws Exception {
        tx.executeWithoutResult(estado -> {
            var u = usuarios.bloquearPorId(usuarioId).orElseThrow();
            u.setCorreoVerificadoUsuario(false);
            verificacion.emitirParaRegistro(u);
        });
        String verificacionToken = token(enviados.element());
        solicitar(email).andExpect(status().isOk());
        String recuperacionToken = token();
        restablecer(verificacionToken, "NuevaPassword456").andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/verificarCorreo").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("token", recuperacionToken))))
                .andExpect(status().isBadRequest());
        restablecer(recuperacionToken, "NuevaPassword456").andExpect(status().isOk());
        assertFalse(usuario().getCorreoVerificadoUsuario());
        assertTrue(verificaciones.existsById(usuarioId));
    }

    @Test
    void rollbackDeSolicitudNoEnviaCorreoNiGuardaToken() {
        tx.executeWithoutResult(estado -> { servicio.solicitar(email); estado.setRollbackOnly(); });
        assertTrue(enviados.isEmpty());
        assertFalse(recuperaciones.existsById(usuarioId));
    }

    @Test
    void rollbackDelCambioConservaLaContrasenaYElToken() throws Exception {
        solicitar(email).andExpect(status().isOk());
        String enlace = token();
        tx.executeWithoutResult(estado -> {
            servicio.restablecer(enlace, "NuevaPassword456");
            estado.setRollbackOnly();
        });
        assertTrue(encoder.matches("Password123", usuario().getContrasenaHashUsuario()));
        assertTrue(recuperaciones.existsById(usuarioId));
        restablecer(enlace, "NuevaPassword456").andExpect(status().isOk());
    }

    @Test
    void falloSmtpSeRecuperaConOtraSolicitudYConservaRespuestaGenerica() throws Exception {
        doThrow(new MailSendException("SMTP no disponible")).doAnswer(invocacion -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            enviados.add(invocacion.getArgument(0, SimpleMailMessage.class));
            return null;
        }).when(correo).send(any(SimpleMailMessage.class));
        String respuesta = solicitar(email).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(respuesta, solicitar("inexistente@example.invalid").andReturn().getResponse().getContentAsString());
        assertTrue(recuperaciones.existsById(usuarioId));
        assertTrue(encoder.matches("Password123", usuario().getContrasenaHashUsuario()));
        avanzar(Duration.ofMinutes(1));
        solicitar(email).andExpect(status().isOk());
        restablecer(token(), "NuevaPassword456").andExpect(status().isOk());
    }

    @Test
    void dosCambiosSimultaneosSoloPermitenUnaContrasenaGanadora() throws Exception {
        solicitar(email).andExpect(status().isOk());
        String enlace = token();
        var barrera = new CyclicBarrier(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> cambiarConBarrera(barrera, enlace, "PasswordNuevaUno"));
            var b = executor.submit(() -> cambiarConBarrera(barrera, enlace, "PasswordNuevaDos"));
            String primero = a.get(15, TimeUnit.SECONDS);
            String segundo = b.get(15, TimeUnit.SECONDS);
            assertNotEquals(primero == null, segundo == null);
            assertTrue(encoder.matches(primero != null ? primero : segundo, usuario().getContrasenaHashUsuario()));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
        }
    }

    @Test
    void dosSolicitudesSimultaneasGeneranUnSoloCorreo() throws Exception {
        var barrera = new CyclicBarrier(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Void> solicitud = () -> { barrera.await(10, TimeUnit.SECONDS); servicio.solicitar(email); return null; };
            var a = executor.submit(solicitud);
            var b = executor.submit(solicitud);
            a.get(15, TimeUnit.SECONDS);
            b.get(15, TimeUnit.SECONDS);
            assertEquals(1, enviados.size());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
        }
    }

    @Test
    void validaCorreoTokenContrasenaYLimiteRealDeBytesSinConsumirEnlace() throws Exception {
        solicitar("correo-invalido").andExpect(status().isBadRequest());
        solicitar(email).andExpect(status().isOk());
        restablecer("invalido", "NuevaPassword456").andExpect(status().isBadRequest());
        restablecer(token(), "corta").andExpect(status().isBadRequest());
        restablecer(token(), " ".repeat(8)).andExpect(status().isBadRequest());
        restablecer(token(), "a".repeat(73)).andExpect(status().isBadRequest());
        restablecer(token(), "ñ".repeat(37)).andExpect(status().isBadRequest());
        assertTrue(recuperaciones.existsById(usuarioId));
        restablecer(token(), "ñ".repeat(36)).andExpect(status().isOk());
        assertTrue(encoder.matches("ñ".repeat(36), usuario().getContrasenaHashUsuario()));
    }

    @Test
    void csrfYGetNoPermitenCambiarLaContrasenaNiEnviarCorreo() throws Exception {
        mvc.perform(post("/api/auth/solicitarRecuperacionContrasena").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("correoElectronico", email)))).andExpect(status().isForbidden());
        solicitar(email).andExpect(status().isOk());
        mvc.perform(post("/api/auth/restablecerContrasena").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("token", token(), "nuevaContrasena", "NuevaPassword456"))))
                .andExpect(status().isForbidden());
        for (String ruta : List.of("solicitarRecuperacionContrasena", "restablecerContrasena")) {
            mvc.perform(get("/api/auth/" + ruta)).andExpect(status().isMethodNotAllowed());
        }
        assertEquals(1, enviados.size());
        assertTrue(encoder.matches("Password123", usuario().getContrasenaHashUsuario()));
    }

    @Test
    void vistasPublicasRenderizanElCssYLosFormularios() throws Exception {
        mvc.perform(get("/recuperar-contrasena")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/css/styles.css")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("recuperacion-form")));
        mvc.perform(get("/restablecer-contrasena")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("restablecer-form")));
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/recuperar-contrasena")));
        mvc.perform(get("/css/styles.css")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(".auth-feedback")));
    }

    private String cambiarConBarrera(CyclicBarrier barrera, String token, String contrasena) throws Exception {
        barrera.await(10, TimeUnit.SECONDS);
        try { servicio.restablecer(token, contrasena); return contrasena; }
        catch (SolicitudInvalidaException ex) { return null; }
    }
    private UUID crearUsuario(String correo) {
        var usuario = new Usuario();
        usuario.setNombreCompletoUsuario("Cliente Prueba");
        usuario.setCorreoElectronicoUsuario(correo);
        usuario.setContrasenaHashUsuario(encoder.encode("Password123"));
        usuario.setTipoUsuario(TipoUsuario.COMPRADOR);
        usuario.setActivoUsuario(true);
        usuario.setCorreoVerificadoUsuario(true);
        usuario.setRegistradoEnUsuario(OffsetDateTime.now());
        UUID id = usuarios.saveAndFlush(usuario).getUsuariosId();
        creados.add(id);
        return id;
    }
    private Usuario usuario() { return usuarios.findById(usuarioId).orElseThrow(); }
    private void avanzar(Duration desdeInicio) { when(clock.instant()).thenReturn(inicio.plus(desdeInicio)); }
    private String token() {
        return token(enviados.stream().filter(m -> m.getSubject().startsWith("Recupera")).toList().getLast());
    }
    private String token(SimpleMailMessage mensaje) { return mensaje.getText().split("#token=", 2)[1].split("\\s", 2)[0]; }
    private MockHttpSession sesion(String correo) throws Exception {
        return (MockHttpSession) login(correo, "Password123").andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }
    private ResultActions solicitar(String correo) throws Exception {
        return mvc.perform(post("/api/auth/solicitarRecuperacionContrasena").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("correoElectronico", correo))));
    }
    private ResultActions restablecer(String token, String contrasena) throws Exception {
        return mvc.perform(post("/api/auth/restablecerContrasena").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("token", token, "nuevaContrasena", contrasena))));
    }
    private ResultActions login(String correo, String contrasena) throws Exception {
        return mvc.perform(post("/api/auth/iniciarSesion").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("correoElectronico", correo, "contrasena", contrasena))));
    }
}
