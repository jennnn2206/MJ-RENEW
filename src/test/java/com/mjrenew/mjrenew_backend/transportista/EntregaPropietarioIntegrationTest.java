package com.mjrenew.mjrenew_backend.transportista;

import com.mjrenew.mjrenew_backend.nucleo.almacenamiento.AlmacenamientoService;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository.AntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.enums.*;
import com.mjrenew.mjrenew_backend.nucleo.exception.TransicionEstadoInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.security.UsuarioPrincipal;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.nucleo.usuario.repository.UsuarioRepository;
import com.mjrenew.mjrenew_backend.propietario.dto.SolicitarEntregaPropietarioRequest;
import com.mjrenew.mjrenew_backend.propietario.service.AntiguedadService;
import com.mjrenew.mjrenew_backend.restaurador.service.RestauradorService;
import com.mjrenew.mjrenew_backend.transportista.repository.TrasladoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.transportista.service.TrasladoAntiguedadService;
import com.mjrenew.mjrenew_backend.transportista.dto.ConfirmarSalidaRequest;
import com.mjrenew.mjrenew_backend.transportista.dto.RegistrarIntentoFallidoRequest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"mjrenew.stripe.transfers-enabled=false",
        "mjrenew.stripe.fee-reconciliation-enabled=false", "mjrenew.stripe.reconciliation-enabled=false"})
@AutoConfigureMockMvc
class EntregaPropietarioIntegrationTest {
    @Autowired AntiguedadRepository antiguedades;
    @Autowired UsuarioRepository usuarios;
    @Autowired TrasladoAntiguedadRepository traslados;
    @Autowired TrasladoAntiguedadService servicio;
    @Autowired AntiguedadService propietario;
    @Autowired RestauradorService restaurador;
    @Autowired TransactionTemplate tx;
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;
    @MockitoBean AlmacenamientoService almacenamiento;
    private final List<UUID> usuarioIds = new ArrayList<>();
    private UUID antiguedadId;
    private UUID propietarioId;
    private UUID restauradorId;
    private final SolicitarEntregaPropietarioRequest horario = new SolicitarEntregaPropietarioRequest(
            LocalDate.now(ZoneId.of("America/Merida")).plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0));

    @BeforeEach
    void preparar() {
        tx.executeWithoutResult(estado -> {
            propietarioId = crearUsuario(TipoUsuario.PROPIETARIO);
            restauradorId = crearUsuario(TipoUsuario.RESTAURADOR);
            Antiguedad antiguedad = new Antiguedad();
            antiguedad.setPropietario(usuarios.getReferenceById(propietarioId));
            antiguedad.setRestaurador(usuarios.getReferenceById(restauradorId));
            antiguedad.setTipoMueble(TipoMueble.MESA);
            antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.DECISION_EN_TALLER);
            antiguedad.setRegistradaEnAntiguedad(OffsetDateTime.now());
            antiguedadId = antiguedades.saveAndFlush(antiguedad).getAntiguedadesId();
        });
    }

    @AfterEach
    void limpiarDatosDeLaPrueba() {
        tx.executeWithoutResult(estado -> {
            em.createQuery("delete from TrasladoAntiguedad t where t.antiguedad.antiguedadesId = :id")
                    .setParameter("id", antiguedadId).executeUpdate();
            antiguedades.deleteById(antiguedadId);
            antiguedades.flush();
            usuarios.deleteAllByIdInBatch(usuarioIds);
        });
    }

    @Test
    void apiPersisteElHorarioYLoDevuelveSinExponerLaDireccionDelTaller() throws Exception {
        mvc.perform(post("/api/propietario/solicitarDevolucionTaller/{id}", antiguedadId)
                        .with(user(principal(propietarioId))).with(csrf())
                        .contentType("application/json").content(jsonHorario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entregaPropietario.fecha").value(horario.fechaAcordadaTraslado().toString()))
                .andExpect(jsonPath("$.entregaPropietario.transportistaAsignado").value(false))
                .andExpect(jsonPath("$.entregaPropietario.direccionOrigen").doesNotExist());
        assertNull(restaurador.obtenerDetalleAsignado(antiguedadId, restauradorId).entregaPropietario());
        assertEquals(1, traslados.countByAntiguedad_AntiguedadesIdAndTipoTraslado(antiguedadId, TipoTraslado.ENTREGA_PROPIETARIO));
    }

    @Test
    void solicitudesConcurrentesGeneranUnSoloIntento() throws Exception {
        CountDownLatch guardada = new CountDownLatch(1);
        CountDownLatch permitirCommit = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var primera = executor.submit(() -> tx.executeWithoutResult(estado -> {
                solicitar();
                guardada.countDown();
                esperar(permitirCommit);
            }));
            assertTrue(guardada.await(10, TimeUnit.SECONDS));
            var segunda = executor.submit(this::solicitar);
            assertThrows(TimeoutException.class, () -> segunda.get(300, TimeUnit.MILLISECONDS));
            permitirCommit.countDown();
            primera.get(10, TimeUnit.SECONDS);
            segunda.get(10, TimeUnit.SECONDS);
        } finally {
            permitirCommit.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
        }
        assertEquals(1, traslados.countByAntiguedad_AntiguedadesIdAndTipoTraslado(antiguedadId, TipoTraslado.ENTREGA_PROPIETARIO));
    }

    @Test
    void administradoresConcurrentesNoAsignanDosTransportistas() throws Exception {
        solicitar();
        UUID trasladoId = ultimoId();
        UUID primero = tx.execute(estado -> crearUsuario(TipoUsuario.TRANSPORTISTA));
        UUID segundo = tx.execute(estado -> crearUsuario(TipoUsuario.TRANSPORTISTA));
        CountDownLatch asignada = new CountDownLatch(1);
        CountDownLatch permitirCommit = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var primera = executor.submit(() -> tx.executeWithoutResult(estado -> {
                servicio.asignarTransportistaEntregaPropietario(trasladoId, primero);
                asignada.countDown();
                esperar(permitirCommit);
            }));
            assertTrue(asignada.await(10, TimeUnit.SECONDS));
            var segunda = executor.submit(() -> assertThrows(TransicionEstadoInvalidaException.class,
                    () -> servicio.asignarTransportistaEntregaPropietario(trasladoId, segundo)));
            assertThrows(TimeoutException.class, () -> segunda.get(300, TimeUnit.MILLISECONDS));
            permitirCommit.countDown();
            primera.get(10, TimeUnit.SECONDS);
            segunda.get(10, TimeUnit.SECONDS);
        } finally {
            permitirCommit.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
        }
        assertEquals(primero, traslados.findById(trasladoId).orElseThrow().getTransportista().getUsuariosId());
        assertEquals(horario.fechaAcordadaTraslado(), traslados.findById(trasladoId).orElseThrow().getFechaAcordadaTraslado());
    }

    @Test
    void tercerIntentoFallidoCancelaLaEntrega() {
        UUID transportista = tx.execute(estado -> crearUsuario(TipoUsuario.TRANSPORTISTA));
        for (int intento = 1; intento <= 3; intento++) {
            solicitar();
            UUID id = ultimoId();
            assertEquals((short) intento, traslados.findById(id).orElseThrow().getNumeroIntentoTraslado());
            servicio.asignarTransportistaEntregaPropietario(id, transportista);
            servicio.confirmarSalidaEntregaPropietario(id, transportista, new ConfirmarSalidaRequest(List.of()));
            servicio.registrarIntentoFallido(id, transportista, new RegistrarIntentoFallidoRequest("No había nadie"), null);
        }
        assertEquals(EstadoAntiguedad.CANCELADA_ENTREGA,
                antiguedades.findById(antiguedadId).orElseThrow().getEstadoActualAntiguedad());
        assertThrows(TransicionEstadoInvalidaException.class, this::solicitar);
    }

    @Test
    void permisosYBeanValidationProtegenLaProgramacion() throws Exception {
        String ruta = "/api/propietario/solicitarDevolucionTaller/" + antiguedadId;
        mvc.perform(post(ruta).with(user(principal(propietarioId))).with(csrf())
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        UUID ajeno = tx.execute(estado -> crearUsuario(TipoUsuario.PROPIETARIO));
        mvc.perform(post(ruta).with(user(principal(ajeno))).with(csrf())
                        .contentType("application/json").content(jsonHorario()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/administrador/obtenerEntregasPropietarioPendientesAsignacion")
                        .with(user(principal(propietarioId))))
                .andExpect(status().isForbidden());
        mvc.perform(post(ruta).with(user(principal(propietarioId)))
                        .contentType("application/json").content(jsonHorario()))
                .andExpect(status().isForbidden());
    }

    @Test
    void administracionVeLaSolicitudYAsignaDesdeLaApi() throws Exception {
        solicitar();
        UUID admin = tx.execute(estado -> crearUsuario(TipoUsuario.ADMINISTRADOR));
        UUID transportista = tx.execute(estado -> crearUsuario(TipoUsuario.TRANSPORTISTA));
        mvc.perform(get("/api/administrador/obtenerEntregasPropietarioPendientesAsignacion").with(user(principal(admin))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].fechaAcordada").exists());
        mvc.perform(post("/api/administrador/asignarTransportistaEntregaPropietario/{id}", ultimoId())
                        .with(user(principal(admin))).with(csrf()).contentType("application/json")
                        .content("{\"transportistaId\":\"" + transportista + "\"}"))
                .andExpect(status().isOk());
        assertTrue(propietario.obtenerDetalle(antiguedadId, propietarioId).entregaPropietario().transportistaAsignado());
    }

    @Test
    void elPropietarioActualizaUnaVentanaVencidaAntesDeLaAsignacion() {
        solicitar();
        UUID id = ultimoId();
        UUID transportista = tx.execute(estado -> crearUsuario(TipoUsuario.TRANSPORTISTA));
        tx.executeWithoutResult(estado -> traslados.findById(id).orElseThrow()
                .setFechaAcordadaTraslado(horario.fechaAcordadaTraslado().minusDays(2)));
        assertThrows(com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException.class,
                () -> servicio.asignarTransportistaEntregaPropietario(id, transportista));
        solicitar();
        assertEquals(id, ultimoId());
        servicio.asignarTransportistaEntregaPropietario(id, transportista);
        assertEquals(EstadoAntiguedad.HORARIO_ENTREGA_AGENDADO,
                antiguedades.findById(antiguedadId).orElseThrow().getEstadoActualAntiguedad());
    }

    @Test
    void restauradorRegistraSuDireccionPrivadaDesdeSuSesion() throws Exception {
        mvc.perform(post("/api/usuario/actualizarMiDomicilio").with(user(principal(restauradorId))).with(csrf())
                        .contentType("application/json").content("{\"direccionTextoUsuario\":\"Taller actualizado\"}"))
                .andExpect(status().isOk());
        assertEquals("Taller actualizado", usuarios.findById(restauradorId).orElseThrow().getDireccionTextoUsuario());
        assertEquals("Domicilio", usuarios.findById(propietarioId).orElseThrow().getDireccionTextoUsuario());
        mvc.perform(get("/administrador"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("entregas-propietario-list")));
        mvc.perform(get("/propietario"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("se-domicilio")));
    }

    private void solicitar() { propietario.solicitarDevolucionTaller(antiguedadId, propietarioId, horario); }
    private UUID ultimoId() {
        return traslados.findFirstByAntiguedad_AntiguedadesIdAndTipoTrasladoOrderByAgendadoEnTrasladoDesc(
                antiguedadId, TipoTraslado.ENTREGA_PROPIETARIO).orElseThrow().getTrasladosAntiguedadId();
    }
    private UUID crearUsuario(TipoUsuario rol) {
        Usuario u = new Usuario();
        u.setNombreCompletoUsuario("Prueba de entrega");
        u.setCorreoElectronicoUsuario("entrega-" + UUID.randomUUID() + "@example.invalid");
        u.setContrasenaHashUsuario("hash-sin-login");
        u.setTipoUsuario(rol);
        u.setActivoUsuario(true);
        u.setCorreoVerificadoUsuario(true);
        u.setRegistradoEnUsuario(OffsetDateTime.now());
        u.setDireccionTextoUsuario(rol == TipoUsuario.RESTAURADOR ? "Taller" : "Domicilio");
        UUID id = usuarios.saveAndFlush(u).getUsuariosId();
        usuarioIds.add(id);
        return id;
    }
    private UsuarioPrincipal principal(UUID id) { return new UsuarioPrincipal(usuarios.findById(id).orElseThrow()); }
    private String jsonHorario() {
        return "{\"fechaAcordadaTraslado\":\"" + horario.fechaAcordadaTraslado()
                + "\",\"horaInicioAcordadaTraslado\":\"10:00\",\"horaFinAcordadaTraslado\":\"12:00\"}";
    }
    private static void esperar(CountDownLatch latch) {
        try { if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("Tiempo de espera agotado"); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new AssertionError(ex); }
    }
}
