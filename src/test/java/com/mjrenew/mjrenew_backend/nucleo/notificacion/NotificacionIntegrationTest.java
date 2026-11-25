package com.mjrenew.mjrenew_backend.nucleo.notificacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mjrenew.mjrenew_backend.nucleo.almacenamiento.AlmacenamientoService;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository.AntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.enums.*;
import com.mjrenew.mjrenew_backend.nucleo.notificacion.entity.Notificacion;
import com.mjrenew.mjrenew_backend.nucleo.notificacion.repository.NotificacionRepository;
import com.mjrenew.mjrenew_backend.nucleo.notificacion.service.*;
import com.mjrenew.mjrenew_backend.nucleo.security.UsuarioPrincipal;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.nucleo.usuario.repository.UsuarioRepository;
import com.mjrenew.mjrenew_backend.propietario.dto.SeleccionarRestauradorRequest;
import com.mjrenew.mjrenew_backend.propietario.service.AntiguedadService;
import com.mjrenew.mjrenew_backend.restaurador.dto.*;
import com.mjrenew.mjrenew_backend.restaurador.entity.PerfilRestaurador;
import com.mjrenew.mjrenew_backend.restaurador.repository.PerfilRestauradorRepository;
import com.mjrenew.mjrenew_backend.restaurador.service.RestauradorService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"mjrenew.stripe.transfers-enabled=false", "mjrenew.stripe.fee-reconciliation-enabled=false",
        "mjrenew.stripe.reconciliation-enabled=false", "mjrenew.notificaciones.correo-habilitado=false"})
@AutoConfigureMockMvc
class NotificacionIntegrationTest {
    @Autowired NotificacionRepository notificaciones;
    @Autowired NotificacionService bandeja;
    @Autowired ColaNotificacionService cola;
    @Autowired EntregaNotificacionService entregas;
    @Autowired AntiguedadRepository antiguedades;
    @Autowired UsuarioRepository usuarios;
    @Autowired PerfilRestauradorRepository perfiles;
    @Autowired AntiguedadService propietario;
    @Autowired RestauradorService restaurador;
    @Autowired ApplicationEventPublisher eventos;
    @Autowired TransactionTemplate tx;
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean JavaMailSender correo;
    @MockitoBean AlmacenamientoService almacenamiento;
    @MockitoBean(name = "notificacionesClock") Clock clock;
    // Fecha anterior a la creación del módulo: la cola de esta prueba no reserva avisos reales del desarrollo.
    private final Instant inicio = Instant.parse("2020-01-01T00:00:00Z");
    private final List<UUID> ids = new ArrayList<>();
    private final Queue<SimpleMailMessage> enviados = new ConcurrentLinkedQueue<>();
    private UUID propietarioId, restauradorId, otroId;

    @BeforeEach
    void preparar() {
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        avanzar(Duration.ZERO);
        propietarioId = crearUsuario(TipoUsuario.PROPIETARIO);
        restauradorId = crearUsuario(TipoUsuario.RESTAURADOR);
        otroId = crearUsuario(TipoUsuario.PROPIETARIO);
        tx.executeWithoutResult(estado -> {
            var perfil = new PerfilRestaurador();
            perfil.setRestaurador(usuarios.getReferenceById(restauradorId));
            perfil.setEspecialidadRestaurador("Madera"); perfil.setAnosExperienciaRestaurador((short) 5);
            perfil.setDisponibilidadRestaurador(DisponibilidadRestaurador.DISPONIBLE);
            perfil.setAprobadoPorAdminRestaurador(true); perfil.setActualizadoEnRestaurador(OffsetDateTime.now());
            perfiles.save(perfil);
        });
        doAnswer(invocacion -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            enviados.add(new SimpleMailMessage(invocacion.getArgument(0, SimpleMailMessage.class)));
            return null;
        }).when(correo).send(any(SimpleMailMessage.class));
        when(almacenamiento.subirArchivo(any(), anyString())).thenAnswer(invocacion -> "http://almacen.test/" + UUID.randomUUID());
    }

    @AfterEach
    void limpiarSoloDatosDeEstaPrueba() {
        tx.executeWithoutResult(estado -> {
            em.createQuery("delete from FotografiaAntiguedad f where f.antiguedad.antiguedadesId in :ids").setParameter("ids", ids).executeUpdate();
            em.createQuery("delete from AvanceRestauracion a where a.antiguedad.antiguedadesId in :ids").setParameter("ids", ids).executeUpdate();
            em.createQuery("delete from Cotizacion c where c.antiguedad.antiguedadesId in :ids").setParameter("ids", ids).executeUpdate();
            antiguedades.deleteAllByIdInBatch(ids);
            perfiles.findByRestaurador_UsuariosId(restauradorId).ifPresent(perfiles::delete);
            perfiles.flush();
            usuarios.deleteAllByIdInBatch(List.of(propietarioId, restauradorId, otroId));
        });
    }

    @Test
    void cuatroOperacionesRealesNotificanAlDestinatarioCorrecto() {
        UUID asignada = crear(EstadoAntiguedad.PIEZA_CAPTURADA, false);
        propietario.seleccionarRestaurador(asignada, propietarioId, new SeleccionarRestauradorRequest(restauradorId));
        UUID cotizada = crear(EstadoAntiguedad.CALCULANDO_PRESUPUESTO, true);
        restaurador.generarCotizacion(cotizada, restauradorId,
                new GenerarCotizacionRequest("Restaurar madera", new BigDecimal("100"), new BigDecimal("200"), 2));
        UUID restaurada = crear(EstadoAntiguedad.EN_RESTAURACION, true);
        restaurador.publicarAvanceRestauracion(restaurada, restauradorId, new PublicarAvanceRequest("Primer avance"), foto());
        restaurador.guardarCierreRestauracion(restaurada, restauradorId, List.of("foto1", "foto2", "foto3", "foto4"));
        restaurador.guardarCierreRestauracion(restaurada, restauradorId, List.of());
        var delRestaurador = bandeja.obtenerPropias(restauradorId, false, PageRequest.of(0, 10));
        assertEquals(1, delRestaurador.getTotalElements());
        assertEquals(TipoNotificacion.ANTIGUEDAD_ASIGNADA, delRestaurador.getContent().getFirst().tipo());
        var delPropietario = bandeja.obtenerPropias(propietarioId, false, PageRequest.of(0, 10));
        assertEquals(3, delPropietario.getTotalElements());
        assertEquals(Set.of(TipoNotificacion.PRESUPUESTO_PRESENTADO, TipoNotificacion.AVANCE_PUBLICADO,
                TipoNotificacion.RESTAURACION_TERMINADA), new HashSet<>(delPropietario.map(n -> n.tipo()).getContent()));
        assertEquals(0, bandeja.contarNoLeidas(otroId));
        verifyNoInteractions(correo);
    }

    @Test
    void cadaAvanceTieneSuAvisoPropio() {
        UUID id = crear(EstadoAntiguedad.EN_RESTAURACION, true);
        for (int i = 0; i < 2; i++) restaurador.publicarAvanceRestauracion(id, restauradorId, new PublicarAvanceRequest("Avance " + i), foto());
        assertEquals(2, bandeja.contarNoLeidas(propietarioId));
    }

    @Test
    void rollbackDeAsignacionNoConservaCambioNiAviso() {
        UUID id = crear(EstadoAntiguedad.PIEZA_CAPTURADA, false);
        tx.executeWithoutResult(estado -> {
            propietario.seleccionarRestaurador(id, propietarioId, new SeleccionarRestauradorRequest(restauradorId));
            estado.setRollbackOnly();
        });
        assertEquals(EstadoAntiguedad.PIEZA_CAPTURADA, antiguedades.findById(id).orElseThrow().getEstadoActualAntiguedad());
        assertEquals(0, bandeja.contarNoLeidas(restauradorId));
        verifyNoInteractions(correo);
    }

    @Test
    void eventosDuplicadosConcurrentesSoloCreanUnAviso() throws Exception {
        UUID id = crear(EstadoAntiguedad.EN_RESTAURACION, true);
        var evento = evento(id, UUID.randomUUID());
        var barrera = new CyclicBarrier(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Void> publicar = () -> { barrera.await(10, TimeUnit.SECONDS); tx.executeWithoutResult(s -> eventos.publishEvent(evento)); return null; };
            var a = executor.submit(publicar); var b = executor.submit(publicar);
            a.get(15, TimeUnit.SECONDS); b.get(15, TimeUnit.SECONDS);
            assertEquals(1, bandeja.contarNoLeidas(propietarioId));
        } finally { executor.shutdownNow(); assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS)); }
    }

    @Test
    void apiSoloExponeAvisosPropiosYNoIncluyeDatosDeLaCola() throws Exception {
        UUID id = aviso();
        mvc.perform(get("/api/notificaciones/obtenerMisNotificaciones").with(user(principal(propietarioId))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].estadoCorreo").doesNotExist())
                .andExpect(jsonPath("$.content[0].destinatarioId").doesNotExist())
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/api/notificaciones/obtenerResumen").with(user(principal(otroId))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.noLeidas").value(0));
        mvc.perform(post("/api/notificaciones/marcarComoLeida/{id}", id).with(user(principal(otroId))).with(csrf()))
                .andExpect(status().isNotFound());
        assertNull(notificaciones.findById(id).orElseThrow().getLeidoEn());
    }

    @Test
    void marcarLeidaEsIdempotenteYPermiteFiltrarElHistorial() throws Exception {
        UUID id = aviso();
        mvc.perform(post("/api/notificaciones/marcarComoLeida/{id}", id).with(user(principal(propietarioId))).with(csrf()))
                .andExpect(status().isOk());
        var leido = notificaciones.findById(id).orElseThrow().getLeidoEn();
        avanzar(Duration.ofSeconds(2));
        assertEquals(leido.toInstant(), bandeja.marcarLeida(id, propietarioId).leidoEn().toInstant());
        assertEquals(0, bandeja.contarNoLeidas(propietarioId));
        assertEquals(0, bandeja.obtenerPropias(propietarioId, true, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1, bandeja.obtenerPropias(propietarioId, false, PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    void paginacionTieneLimiteYOrdenEstable() {
        UUID antiguedad = crear(EstadoAntiguedad.EN_RESTAURACION, true);
        tx.executeWithoutResult(s -> { for (int i = 0; i < 52; i++) eventos.publishEvent(evento(antiguedad, UUID.randomUUID())); });
        var primera = bandeja.obtenerPropias(propietarioId, false, PageRequest.of(0, 1000));
        var segunda = bandeja.obtenerPropias(propietarioId, false, PageRequest.of(1, 50));
        assertEquals(50, primera.getSize()); assertEquals(52, primera.getTotalElements()); assertEquals(2, segunda.getNumberOfElements());
        var idsPrimera = primera.getContent().stream().map(n -> n.id()).toList();
        assertTrue(segunda.stream().noneMatch(n -> idsPrimera.contains(n.id())));
    }

    @Test
    void apiExigeSesionYCsrf() throws Exception {
        UUID id = aviso();
        mvc.perform(get("/api/notificaciones/obtenerMisNotificaciones")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/notificaciones/marcarComoLeida/{id}", id).with(user(principal(propietarioId))))
                .andExpect(status().isForbidden());
        assertNull(notificaciones.findById(id).orElseThrow().getLeidoEn());
    }

    @Test
    void entregaFueraDeTransaccionSeConfirmaYNoSeRepite() {
        UUID id = aviso();
        entregas.procesarLote(); entregas.procesarLote();
        assertEquals(1, enviados.size());
        assertEquals(usuarios.findById(propietarioId).orElseThrow().getCorreoElectronicoUsuario(), enviados.element().getTo()[0]);
        var n = notificaciones.findById(id).orElseThrow();
        assertEquals(EstadoCorreoNotificacion.ENVIADO, n.getEstadoCorreo());
        assertEquals(1, n.getIntentosCorreo()); assertNotNull(n.getCorreoEnviadoEn());
    }

    @Test
    void falloDeCorreoConservaPendienteYReintentaAlMinuto() {
        UUID id = aviso();
        doThrow(new MailSendException("SMTP no disponible")).doAnswer(invocacion -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            enviados.add(invocacion.getArgument(0, SimpleMailMessage.class)); return null;
        }).when(correo).send(any(SimpleMailMessage.class));
        entregas.procesarLote();
        assertEquals(EstadoCorreoNotificacion.PENDIENTE, notificaciones.findById(id).orElseThrow().getEstadoCorreo());
        avanzar(Duration.ofSeconds(59)); entregas.procesarLote(); assertTrue(enviados.isEmpty());
        avanzar(Duration.ofMinutes(1)); entregas.procesarLote();
        assertEquals(1, enviados.size());
        assertEquals(2, notificaciones.findById(id).orElseThrow().getIntentosCorreo());
    }

    @Test
    void reservaVencidaSeRecuperaYElTrabajadorAntiguoNoPuedeConfirmarla() {
        UUID id = aviso(); var anterior = cola.reservar().orElseThrow();
        avanzar(Duration.ofSeconds(119)); assertTrue(cola.reservar().isEmpty());
        avanzar(Duration.ofMinutes(2)); var nueva = cola.reservar().orElseThrow();
        assertNotEquals(anterior.reservaId(), nueva.reservaId());
        cola.concluir(anterior, true);
        assertEquals(nueva.reservaId(), notificaciones.findById(id).orElseThrow().getReservaId());
        cola.concluir(nueva, true);
        assertEquals(EstadoCorreoNotificacion.ENVIADO, notificaciones.findById(id).orElseThrow().getEstadoCorreo());
    }

    @Test
    void trabajadoresConcurrentesNoReservanElMismoCorreo() throws Exception {
        aviso(); var barrera = new CyclicBarrier(2); var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> reservar = () -> { barrera.await(10, TimeUnit.SECONDS); return cola.reservar().isPresent(); };
            var a = executor.submit(reservar); var b = executor.submit(reservar);
            assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
        } finally { executor.shutdownNow(); assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS)); }
    }

    @Test
    void unaFilaBloqueadaNoImpideReservarLaSiguiente() throws Exception {
        aviso(); aviso(); var bloqueada = new CountDownLatch(1); var liberar = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var retenida = executor.submit(() -> tx.execute(s -> {
                var n = notificaciones.reservarSiguiente(OffsetDateTime.now(clock)).orElseThrow();
                bloqueada.countDown(); esperar(liberar); return n.getId();
            }));
            assertTrue(bloqueada.await(10, TimeUnit.SECONDS));
            var siguiente = executor.submit(() -> cola.reservar().orElseThrow());
            var envio = siguiente.get(3, TimeUnit.SECONDS);
            liberar.countDown();
            assertNotEquals(retenida.get(10, TimeUnit.SECONDS), envio.notificacionId());
        } finally { liberar.countDown(); executor.shutdownNow(); assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS)); }
    }

    @Test
    void envioLentoNoBloqueaLecturaYConservaLaMarcaLeida() throws Exception {
        UUID id = aviso(); var enviando = new CountDownLatch(1); var liberar = new CountDownLatch(1);
        doAnswer(invocacion -> { assertFalse(TransactionSynchronizationManager.isActualTransactionActive()); enviando.countDown(); esperar(liberar); return null; })
                .when(correo).send(any(SimpleMailMessage.class));
        var executor = Executors.newFixedThreadPool(2);
        try {
            var envio = executor.submit(entregas::procesarLote);
            assertTrue(enviando.await(10, TimeUnit.SECONDS));
            assertNotNull(executor.submit(() -> bandeja.marcarLeida(id, propietarioId)).get(3, TimeUnit.SECONDS).leidoEn());
            liberar.countDown(); envio.get(10, TimeUnit.SECONDS);
            assertNotNull(notificaciones.findById(id).orElseThrow().getLeidoEn());
        } finally { liberar.countDown(); executor.shutdownNow(); assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS)); }
    }

    @Test
    void destinatarioSinVerificarEsperaSinEnviarInformacion() {
        UUID id = aviso();
        tx.executeWithoutResult(s -> usuarios.bloquearPorId(propietarioId).orElseThrow().setCorreoVerificadoUsuario(false));
        entregas.procesarLote(); assertTrue(enviados.isEmpty());
        assertEquals(0, notificaciones.findById(id).orElseThrow().getIntentosCorreo());
        tx.executeWithoutResult(s -> usuarios.bloquearPorId(propietarioId).orElseThrow().setCorreoVerificadoUsuario(true));
        avanzar(Duration.ofMinutes(5)); entregas.procesarLote(); assertEquals(1, enviados.size());
    }

    @Test
    void panelesRenderizanLaBandejaConCssSinIncluirDatosPrivados() throws Exception {
        aviso();
        for (String panel : List.of("/propietario", "/restaurador")) {
            mvc.perform(get(panel)).andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("data-notificaciones")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/bandeja-notificaciones.js")));
        }
        mvc.perform(get("/css/styles.css")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(".notificaciones-panel")));
    }

    @Test
    void pendientesDeCuentasSinVerificarNoRetrasanLosDeOtrosDestinatarios() {
        UUID bloqueado = aviso();
        avanzar(Duration.ofSeconds(1));
        UUID asignada = crear(EstadoAntiguedad.PIEZA_CAPTURADA, false);
        propietario.seleccionarRestaurador(asignada, propietarioId, new SeleccionarRestauradorRequest(restauradorId));
        tx.executeWithoutResult(s -> usuarios.bloquearPorId(propietarioId).orElseThrow().setCorreoVerificadoUsuario(false));
        entregas.procesarLote();
        assertEquals(1, enviados.size());
        assertEquals(usuarios.findById(restauradorId).orElseThrow().getCorreoElectronicoUsuario(), enviados.element().getTo()[0]);
        assertEquals(0, notificaciones.findById(bloqueado).orElseThrow().getIntentosCorreo());
    }

    private UUID aviso() {
        UUID id = crear(EstadoAntiguedad.EN_RESTAURACION, true);
        tx.executeWithoutResult(s -> eventos.publishEvent(evento(id, UUID.randomUUID())));
        return bandeja.obtenerPropias(propietarioId, false, PageRequest.of(0, 1)).getContent().getFirst().id();
    }
    private NotificacionRestauracionEmitida evento(UUID id, UUID referencia) {
        return new NotificacionRestauracionEmitida(TipoNotificacion.AVANCE_PUBLICADO, id, referencia, propietarioId);
    }
    private UUID crear(EstadoAntiguedad estado, boolean asignada) {
        return tx.execute(s -> {
            var a = new Antiguedad(); a.setPropietario(usuarios.getReferenceById(propietarioId));
            if (asignada) a.setRestaurador(usuarios.getReferenceById(restauradorId));
            a.setTipoMueble(TipoMueble.MESA); a.setEstadoActualAntiguedad(estado); a.setRegistradaEnAntiguedad(OffsetDateTime.now());
            UUID id = antiguedades.saveAndFlush(a).getAntiguedadesId(); ids.add(id); return id;
        });
    }
    private UUID crearUsuario(TipoUsuario tipo) {
        var u = new Usuario(); u.setNombreCompletoUsuario("Usuario Prueba");
        u.setCorreoElectronicoUsuario("notificaciones-" + UUID.randomUUID() + "@example.invalid");
        u.setContrasenaHashUsuario("hash-sin-login"); u.setTipoUsuario(tipo); u.setActivoUsuario(true);
        u.setCorreoVerificadoUsuario(true); u.setRegistradoEnUsuario(OffsetDateTime.now());
        return usuarios.saveAndFlush(u).getUsuariosId();
    }
    private UsuarioPrincipal principal(UUID id) { return new UsuarioPrincipal(usuarios.findById(id).orElseThrow()); }
    private void avanzar(Duration duracion) { when(clock.instant()).thenReturn(inicio.plus(duracion)); }
    private MockMultipartFile foto() { return new MockMultipartFile("foto", "avance.jpg", "image/jpeg", new byte[]{1}); }
    private static void esperar(CountDownLatch latch) {
        try { assertTrue(latch.await(10, TimeUnit.SECONDS)); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new AssertionError(ex); }
    }
}
