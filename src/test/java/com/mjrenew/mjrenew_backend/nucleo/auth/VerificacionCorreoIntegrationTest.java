package com.mjrenew.mjrenew_backend.nucleo.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mjrenew.mjrenew_backend.nucleo.auth.repository.VerificacionCorreoRepository;
import com.mjrenew.mjrenew_backend.nucleo.auth.service.AuthService;
import com.mjrenew.mjrenew_backend.nucleo.auth.service.VerificacionCorreoService;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoUsuario;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.security.UsuarioPrincipal;
import com.mjrenew.mjrenew_backend.nucleo.usuario.dto.UsuarioRegistroRequest;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.nucleo.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"mjrenew.stripe.transfers-enabled=false",
        "mjrenew.stripe.fee-reconciliation-enabled=false", "mjrenew.stripe.reconciliation-enabled=false",
        "app.base-url=https://mjrenew.example"})
@AutoConfigureMockMvc
class VerificacionCorreoIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired VerificacionCorreoRepository verificaciones;
    @Autowired VerificacionCorreoService servicio;
    @Autowired AuthService auth;
    @Autowired TransactionTemplate tx;
    @MockitoBean JavaMailSender correo;
    @MockitoBean(name = "verificacionCorreoClock") Clock clock;
    private final Queue<SimpleMailMessage> enviados = new ConcurrentLinkedQueue<>();
    private final List<String> correosCreados = new ArrayList<>();
    private final Instant inicio = Instant.parse("2026-11-24T18:00:00Z");
    private String email;

    @BeforeEach
    void preparar() {
        email = "verificacion-" + UUID.randomUUID() + "@example.invalid";
        correosCreados.add(email);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        avanzar(Duration.ZERO);
        doAnswer(invocacion -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                    "SMTP debe ejecutarse sin una transacción SQL abierta");
            enviados.add(new SimpleMailMessage(invocacion.getArgument(0, SimpleMailMessage.class)));
            return null;
        }).when(correo).send(any(SimpleMailMessage.class));
    }

    @AfterEach
    void limpiarSoloLosDatosCreados() {
        tx.executeWithoutResult(estado -> {
            for (String creado : correosCreados) {
                usuarios.buscarIdPorCorreo(creado).ifPresent(id -> {
                    verificaciones.deleteById(id);
                    verificaciones.flush();
                    usuarios.deleteById(id);
                });
            }
        });
    }

    @Test
    void registroEnviaDespuesDelCommitGuardaSoloHashYNoIniciaSesion() throws Exception {
        var respuesta = registrar();
        assertEquals(201, respuesta.getResponse().getStatus());
        var usuario = usuario();
        assertFalse(usuario.getCorreoVerificadoUsuario());
        var verificacion = verificaciones.findById(usuario.getUsuariosId()).orElseThrow();
        assertEquals(64, verificacion.getTokenHash().length());
        assertNotEquals(token(), verificacion.getTokenHash());
        assertEquals(inicio, verificacion.getEmitidoEn().toInstant());
        assertEquals(inicio.plus(Duration.ofHours(24)), verificacion.getExpiraEn().toInstant());
        assertEquals(email, enviados.element().getTo()[0]);
        assertTrue(enviados.element().getText().contains("https://mjrenew.example/verificar-correo#token="));
        assertNull(respuesta.getRequest().getSession(false));
        mvc.perform(get("/api/auth/obtenerUsuarioActual")).andExpect(status().isUnauthorized());
    }

    @Test
    void verificarPermiteLoginYSesionPersistenteYElEnlaceNoSeReutiliza() throws Exception {
        registrar();
        String enlace = token();
        verificar(enlace).andExpect(status().isOk());
        assertTrue(usuario().getCorreoVerificadoUsuario());
        assertFalse(verificaciones.existsById(usuario().getUsuariosId()));
        verificar(enlace).andExpect(status().isBadRequest());
        var login = login("Password123").andExpect(status().isOk()).andReturn();
        var sesion = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(sesion);
        mvc.perform(get("/api/auth/obtenerUsuarioActual").session(sesion))
                .andExpect(status().isOk()).andExpect(jsonPath("$.correoElectronico").value(email));
    }

    @Test
    void pendienteConContrasenaCorrectaOfreceReenvioSinCrearSesion() throws Exception {
        registrar();
        var login = login("Password123").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("CORREO_NO_VERIFICADO")).andReturn();
        var sesion = login.getRequest().getSession(false);
        assertTrue(sesion == null || sesion.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY) == null);
    }

    @Test
    void contrasenaIncorrectaNoRevelaQueLaCuentaEstaPendiente() throws Exception {
        registrar();
        var incorrecta = login("Incorrecta123").andExpect(status().isUnauthorized()).andReturn();
        var desconocida = mvc.perform(post("/api/auth/iniciarSesion").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "correoElectronico", "inexistente@example.invalid", "contrasena", "Incorrecta123"))))
                .andExpect(status().isUnauthorized()).andReturn();
        assertEquals(json.readTree(incorrecta.getResponse().getContentAsString()).get("mensaje"),
                json.readTree(desconocida.getResponse().getContentAsString()).get("mensaje"));
    }

    @Test
    void enlaceVenceExactamenteALasVeinticuatroHoras() throws Exception {
        registrar();
        avanzar(Duration.ofHours(24));
        verificar(token()).andExpect(status().isBadRequest());
        assertFalse(usuario().getCorreoVerificadoUsuario());
    }

    @Test
    void enlaceFuncionaJustoAntesDelVencimiento() throws Exception {
        registrar();
        avanzar(Duration.ofHours(24).minusSeconds(1));
        verificar(token()).andExpect(status().isOk());
    }

    @Test
    void reenvioLimitaFrecuenciaRenuevaVigenciaEInvalidaElAnterior() throws Exception {
        registrar();
        String anterior = token();
        reenviar(email).andExpect(status().isOk());
        assertEquals(1, enviados.size());
        avanzar(Duration.ofMinutes(1));
        reenviar(email).andExpect(status().isOk());
        assertEquals(2, enviados.size());
        String nuevo = token(enviados.stream().toList().get(1));
        assertNotEquals(anterior, nuevo);
        verificar(anterior).andExpect(status().isBadRequest());
        avanzar(Duration.ofHours(24));
        verificar(nuevo).andExpect(status().isOk());
    }

    @Test
    void reenvioRespondeIgualParaDesconocidoVerificadoEInactivo() throws Exception {
        registrar();
        String desconocido = reenviar("nadie@example.invalid").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        verificar(token()).andExpect(status().isOk());
        assertEquals(desconocido, reenviar(email).andReturn().getResponse().getContentAsString());
        tx.executeWithoutResult(estado -> {
            var u = usuarios.bloquearPorId(usuario().getUsuariosId()).orElseThrow();
            u.setCorreoVerificadoUsuario(false);
            u.setActivoUsuario(false);
        });
        avanzar(Duration.ofMinutes(2));
        assertEquals(desconocido, reenviar(email).andReturn().getResponse().getContentAsString());
        assertEquals(1, enviados.size());
    }

    @Test
    void cuentaInactivaNoSeActivaUsandoUnEnlace() throws Exception {
        registrar();
        tx.executeWithoutResult(estado -> {
            var u = usuarios.bloquearPorId(usuario().getUsuariosId()).orElseThrow();
            u.setActivoUsuario(false);
        });
        verificar(token()).andExpect(status().isBadRequest());
        login("Password123").andExpect(status().isUnauthorized());
        assertFalse(usuario().getCorreoVerificadoUsuario());
    }

    @Test
    void rollbackNoEnviaCorreoNiConservaCuentaOToken() {
        tx.executeWithoutResult(estado -> {
            auth.registrar(solicitud());
            estado.setRollbackOnly();
        });
        assertFalse(usuarios.existsByCorreoElectronicoUsuario(email));
        assertTrue(enviados.isEmpty());
    }

    @Test
    void falloSmtpConservaCuentaPendienteYPermiteReintentar() throws Exception {
        doThrow(new MailSendException("SMTP no disponible")).doAnswer(invocacion -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            enviados.add(invocacion.getArgument(0, SimpleMailMessage.class));
            return null;
        }).when(correo).send(any(SimpleMailMessage.class));
        assertEquals(201, registrar().getResponse().getStatus());
        assertFalse(usuario().getCorreoVerificadoUsuario());
        avanzar(Duration.ofMinutes(1));
        reenviar(email).andExpect(status().isOk());
        verificar(token()).andExpect(status().isOk());
    }

    @Test
    void dosConfirmacionesSimultaneasSoloConsumenElEnlaceUnaVez() throws Exception {
        registrar();
        String token = token();
        var barrera = new CyclicBarrier(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> confirmar = () -> {
                barrera.await(10, TimeUnit.SECONDS);
                try { servicio.verificar(token); return true; }
                catch (SolicitudInvalidaException ex) { return false; }
            };
            var a = executor.submit(confirmar);
            var b = executor.submit(confirmar);
            assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
            assertTrue(usuario().getCorreoVerificadoUsuario());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
        }
    }

    @Test
    void dosReenviosSimultaneosSoloGeneranUnCorreoNuevo() throws Exception {
        registrar();
        avanzar(Duration.ofMinutes(1));
        var barrera = new CyclicBarrier(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Void> reenviar = () -> { barrera.await(10, TimeUnit.SECONDS); servicio.reenviar(email); return null; };
            var a = executor.submit(reenviar);
            var b = executor.submit(reenviar);
            a.get(15, TimeUnit.SECONDS);
            b.get(15, TimeUnit.SECONDS);
            assertEquals(2, enviados.size());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
        }
    }

    @ParameterizedTest
    @EnumSource(TipoUsuario.class)
    void ningunaSesionAntiguaSinVerificarPuedeUsarFuncionesProtegidas(TipoUsuario rol) throws Exception {
        var usuario = new Usuario();
        usuario.setTipoUsuario(rol);
        usuario.setCorreoVerificadoUsuario(false);
        usuario.setActivoUsuario(true);
        usuario.setCorreoElectronicoUsuario(email);
        mvc.perform(get("/api/auth/obtenerUsuarioActual").with(user(new UsuarioPrincipal(usuario))))
                .andExpect(status().isForbidden());
    }

    @Test
    void csrfValidacionYGetNoPermitenConsumirNiReenviarElEnlace() throws Exception {
        registrar();
        String body = json.writeValueAsString(Map.of("token", token()));
        mvc.perform(post("/api/auth/verificarCorreo").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/reenviarVerificacionCorreo").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("correoElectronico", email))))
                .andExpect(status().isForbidden());
        verificar("invalido").andExpect(status().isBadRequest());
        reenviar("invalido").andExpect(status().isBadRequest());
        mvc.perform(get("/api/auth/verificarCorreo").param("token", token()))
                .andExpect(status().isMethodNotAllowed());
        for (String pagina : List.of("/registro", "/login", "/verificar-correo")) {
            mvc.perform(get(pagina)).andExpect(status().isOk());
        }
        assertFalse(usuario().getCorreoVerificadoUsuario());
        assertEquals(1, enviados.size());
    }

    private MvcResult registrar() throws Exception {
        return mvc.perform(post("/api/auth/registrarUsuario").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(solicitud())))
                .andReturn();
    }
    private UsuarioRegistroRequest solicitud() {
        return new UsuarioRegistroRequest("Cliente Prueba", email, "Password123", TipoUsuario.COMPRADOR);
    }
    private Usuario usuario() { return usuarios.findByCorreoElectronicoUsuario(email).orElseThrow(); }
    private String token() { return token(enviados.element()); }
    private String token(SimpleMailMessage mensaje) {
        return mensaje.getText().split("#token=", 2)[1].split("\\s", 2)[0];
    }
    private void avanzar(Duration desdeInicio) { when(clock.instant()).thenReturn(inicio.plus(desdeInicio)); }
    private org.springframework.test.web.servlet.ResultActions verificar(String token) throws Exception {
        return mvc.perform(post("/api/auth/verificarCorreo").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("token", token))));
    }
    private org.springframework.test.web.servlet.ResultActions reenviar(String email) throws Exception {
        return mvc.perform(post("/api/auth/reenviarVerificacionCorreo").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("correoElectronico", email))));
    }
    private org.springframework.test.web.servlet.ResultActions login(String contrasena) throws Exception {
        return mvc.perform(post("/api/auth/iniciarSesion").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("correoElectronico", email, "contrasena", contrasena))));
    }
}
