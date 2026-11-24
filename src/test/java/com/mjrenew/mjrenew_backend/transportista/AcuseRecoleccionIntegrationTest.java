package com.mjrenew.mjrenew_backend.transportista;

import com.mjrenew.mjrenew_backend.nucleo.almacenamiento.AlmacenamientoService;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository.AntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.enums.*;
import com.mjrenew.mjrenew_backend.nucleo.exception.*;
import com.mjrenew.mjrenew_backend.nucleo.security.UsuarioPrincipal;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.nucleo.usuario.repository.UsuarioRepository;
import com.mjrenew.mjrenew_backend.propietario.service.AntiguedadService;
import com.mjrenew.mjrenew_backend.transportista.dto.*;
import com.mjrenew.mjrenew_backend.transportista.repository.TrasladoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.transportista.service.TrasladoAntiguedadService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"mjrenew.stripe.transfers-enabled=false",
        "mjrenew.stripe.fee-reconciliation-enabled=false", "mjrenew.stripe.reconciliation-enabled=false"})
@AutoConfigureMockMvc
class AcuseRecoleccionIntegrationTest {
    @Autowired AntiguedadRepository antiguedades;
    @Autowired UsuarioRepository usuarios;
    @Autowired TrasladoAntiguedadRepository traslados;
    @Autowired TrasladoAntiguedadService servicio;
    @Autowired com.mjrenew.mjrenew_backend.transportista.service.EvidenciaRecoleccionService evidencias;
    @Autowired AntiguedadService propietario;
    @Autowired TransactionTemplate tx;
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;
    @MockitoBean AlmacenamientoService almacenamiento;
    private final List<UUID> usuarioIds = new ArrayList<>();
    private UUID antiguedadId;
    private UUID propietarioId;
    private UUID transportistaId;
    private UUID trasladoId;

    @BeforeEach
    void preparar() {
        tx.executeWithoutResult(estado -> {
            propietarioId = crearUsuario(TipoUsuario.PROPIETARIO);
            transportistaId = crearUsuario(TipoUsuario.TRANSPORTISTA);
            Antiguedad a = new Antiguedad();
            a.setPropietario(usuarios.getReferenceById(propietarioId));
            a.setTipoMueble(TipoMueble.MESA);
            a.setEstadoActualAntiguedad(EstadoAntiguedad.PAGO_EN_ESCROW);
            a.setRegistradaEnAntiguedad(OffsetDateTime.now());
            antiguedadId = antiguedades.saveAndFlush(a).getAntiguedadesId();
        });
        trasladoId = servicio.crearTraslado(request()).trasladoId();
    }

    @AfterEach
    void limpiarSoloLosDatosDeEstaPrueba() {
        tx.executeWithoutResult(estado -> {
            em.createQuery("delete from FotografiaAntiguedad f where f.antiguedad.antiguedadesId = :id")
                    .setParameter("id", antiguedadId).executeUpdate();
            em.createQuery("delete from TrasladoAntiguedad t where t.antiguedad.antiguedadesId = :id")
                    .setParameter("id", antiguedadId).executeUpdate();
            antiguedades.deleteById(antiguedadId);
            antiguedades.flush();
            usuarios.deleteAllByIdInBatch(usuarioIds);
        });
    }

    @Test
    void propietarioVeEvidenciasConfirmaYLuegoSeRegistraLaLlegada() throws Exception {
        recoger();
        assertThrows(TransicionEstadoInvalidaException.class, this::llegar);
        var detalle = propietario.obtenerDetalle(antiguedadId, propietarioId);
        assertEquals(4, detalle.recoleccion().fotos().size());
        assertEquals((short) 1, detalle.recoleccion().numeroIntento());
        assertNull(detalle.recoleccion().entregaConfirmadaEn());

        mvc.perform(post("/api/propietario/confirmarEntregaRecoleccion/{id}", trasladoId)
                        .with(user(principal(propietarioId))).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.confirmadaPor").value(propietarioId.toString()))
                .andExpect(jsonPath("$.entregaConfirmadaEn").exists());
        var fecha = traslados.findById(trasladoId).orElseThrow().getRecoleccionConfirmadaEn();
        llegar();
        propietario.confirmarEntregaRecoleccion(trasladoId, propietarioId);
        assertEquals(fecha, traslados.findById(trasladoId).orElseThrow().getRecoleccionConfirmadaEn());
        assertEquals(EstadoAntiguedad.RECIBIDO_EN_TALLER,
                antiguedades.findById(antiguedadId).orElseThrow().getEstadoActualAntiguedad());
        assertEquals(4, propietario.obtenerDetalle(antiguedadId, propietarioId).recoleccion().fotos().size());
    }

    @Test
    void llegadaConcurrenteEsperaElCommitDelAcuse() throws Exception {
        recoger();
        CountDownLatch confirmado = new CountDownLatch(1);
        CountDownLatch permitirCommit = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var acuse = executor.submit(() -> tx.executeWithoutResult(estado -> {
                servicio.confirmarEntregaRecoleccion(trasladoId, propietarioId);
                confirmado.countDown();
                esperar(permitirCommit);
            }));
            assertTrue(confirmado.await(10, TimeUnit.SECONDS));
            var llegada = executor.submit(this::llegar);
            assertThrows(TimeoutException.class, () -> llegada.get(300, TimeUnit.MILLISECONDS));
            permitirCommit.countDown();
            acuse.get(10, TimeUnit.SECONDS);
            llegada.get(10, TimeUnit.SECONDS);
        } finally {
            permitirCommit.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
        }
        assertEquals(ResultadoTraslado.EXITOSO, traslados.findById(trasladoId).orElseThrow().getResultadoTraslado());
    }

    @Test
    void dosConfirmacionesSimultaneasDevuelvenElMismoAcuse() throws Exception {
        recoger();
        var barrera = new CyclicBarrier(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<com.mjrenew.mjrenew_backend.propietario.dto.RecoleccionPropietarioResponse> confirmar = () -> {
                barrera.await(10, TimeUnit.SECONDS);
                return servicio.confirmarEntregaRecoleccion(trasladoId, propietarioId);
            };
            var primera = executor.submit(confirmar);
            var segunda = executor.submit(confirmar);
            assertEquals(primera.get(15, TimeUnit.SECONDS).entregaConfirmadaEn().toInstant(),
                    segunda.get(15, TimeUnit.SECONDS).entregaConfirmadaEn().toInstant());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
        }
    }

    @Test
    void laSalidaFuncionaDesdeElEndpointMultipartYNoSubeDentroDeUnaTransaccion() throws Exception {
        when(almacenamiento.subirArchivo(any(), eq("traslados"))).thenAnswer(invocacion -> {
            assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());
            return "http://almacenamiento.test/recoleccion/" + UUID.randomUUID();
        });
        var peticion = multipart("/api/transportista/confirmarSalidaRecoleccion/{id}", trasladoId);
        for (int i = 0; i < 4; i++) {
            peticion.file(new MockMultipartFile("fotos", "foto-" + i + ".jpg", "image/jpeg", new byte[]{1}));
        }
        mvc.perform(peticion.with(user(principal(transportistaId))).with(csrf())).andExpect(status().isOk());
        assertEquals(4, propietario.obtenerDetalle(antiguedadId, propietarioId).recoleccion().fotos().size());
    }

    @Test
    void otroPropietarioNoVeNiConfirmaYTransportistaNoSuplantaElAcuse() throws Exception {
        recoger();
        UUID ajeno = tx.execute(estado -> crearUsuario(TipoUsuario.PROPIETARIO));
        String ruta = "/api/propietario/confirmarEntregaRecoleccion/" + trasladoId;
        mvc.perform(post(ruta).with(user(principal(ajeno))).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/api/propietario/obtenerDetalleAntiguedad/{id}", antiguedadId)
                        .with(user(principal(ajeno)))).andExpect(status().isNotFound());
        mvc.perform(post(ruta).with(user(principal(transportistaId))).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post(ruta).with(user(principal(propietarioId)))).andExpect(status().isForbidden());
        assertNull(traslados.findById(trasladoId).orElseThrow().getRecoleccionConfirmadaEn());
    }

    @Test
    void rollbackNoDejaUnAcuseParcial() {
        recoger();
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(estado -> {
            servicio.confirmarEntregaRecoleccion(trasladoId, propietarioId);
            throw new IllegalStateException("Fallo antes del commit");
        }));
        assertNull(traslados.findById(trasladoId).orElseThrow().getRecoleccionConfirmadaEn());
        assertThrows(TransicionEstadoInvalidaException.class, this::llegar);
    }

    @Test
    void evidenciasDeOtroIntentoNoAutorizanElAcuseActual() {
        recoger();
        UUID anterior = trasladoId;
        tx.executeWithoutResult(estado -> {
            traslados.findById(anterior).orElseThrow().setResultadoTraslado(ResultadoTraslado.NADIE_EN_CASA);
            antiguedades.findById(antiguedadId).orElseThrow().setEstadoActualAntiguedad(EstadoAntiguedad.PAGO_EN_ESCROW);
        });
        trasladoId = servicio.crearTraslado(request()).trasladoId();
        tx.executeWithoutResult(estado -> {
            traslados.findById(trasladoId).orElseThrow().setSalidaConfirmadaEnTraslado(OffsetDateTime.now());
            antiguedades.findById(antiguedadId).orElseThrow().setEstadoActualAntiguedad(EstadoAntiguedad.EN_RECOLECCION);
        });
        assertTrue(propietario.obtenerDetalle(antiguedadId, propietarioId).recoleccion().fotos().isEmpty());
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.confirmarEntregaRecoleccion(trasladoId, propietarioId));
        assertNull(traslados.findById(trasladoId).orElseThrow().getRecoleccionConfirmadaEn());
        // Un registro anterior aún en curso puede completar sus evidencias mediante la misma acción.
        recoger();
        servicio.confirmarEntregaRecoleccion(trasladoId, propietarioId);
        assertEquals(4, propietario.obtenerDetalle(antiguedadId, propietarioId).recoleccion().fotos().size());
    }

    @Test
    void tercerIntentoFallidoCancelaRecoleccionYAdministracionVeElConteo() throws Exception {
        for (int intento = 1; intento <= 3; intento++) {
            servicio.registrarIntentoFallido(trasladoId, transportistaId, new RegistrarIntentoFallidoRequest("No había nadie"), null);
            if (intento < 3) trasladoId = servicio.crearTraslado(request()).trasladoId();
        }
        assertEquals(EstadoAntiguedad.CANCELADA_RECOLECCION,
                antiguedades.findById(antiguedadId).orElseThrow().getEstadoActualAntiguedad());
        assertThrows(TransicionEstadoInvalidaException.class, () -> servicio.crearTraslado(request()));
        var historial = servicio.obtenerRecolecciones(PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "agendadoEnTraslado")));
        assertEquals(3, historial.getContent().stream().filter(t -> t.antiguedadId().equals(antiguedadId)).count());
        UUID admin = tx.execute(estado -> crearUsuario(TipoUsuario.ADMINISTRADOR));
        mvc.perform(get("/api/administrador/obtenerRecolecciones").with(user(principal(admin))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].numeroIntento").value(3));
        mvc.perform(get("/api/administrador/obtenerRecolecciones").with(user(principal(propietarioId))))
                .andExpect(status().isForbidden());
    }

    private void recoger() {
        when(almacenamiento.subirArchivo(any(), eq("traslados")))
                .thenAnswer(invocacion -> {
                    assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());
                    return "http://almacenamiento.test/recoleccion/" + UUID.randomUUID();
                });
        List<MultipartFile> fotos = IntStream.range(0, 4).mapToObj(i -> (MultipartFile)
                new MockMultipartFile("fotos", "recoleccion-" + i + ".jpg", "image/jpeg", new byte[]{1})).toList();
        evidencias.confirmarSalida(trasladoId, transportistaId, fotos);
    }
    private void llegar() { servicio.confirmarLlegadaTaller(trasladoId, transportistaId, new ConfirmarLlegadaRequest(List.of())); }
    private CrearTrasladoRequest request() {
        return new CrearTrasladoRequest(antiguedadId, transportistaId, TipoTraslado.RECOLECCION,
                "Domicilio", "Taller", LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0), null);
    }
    private UUID crearUsuario(TipoUsuario rol) {
        Usuario u = new Usuario();
        u.setNombreCompletoUsuario("Prueba de recolección");
        u.setCorreoElectronicoUsuario("acuse-" + UUID.randomUUID() + "@example.invalid");
        u.setContrasenaHashUsuario("hash-sin-login");
        u.setTipoUsuario(rol);
        u.setActivoUsuario(true);
        u.setCorreoVerificadoUsuario(true);
        u.setRegistradoEnUsuario(OffsetDateTime.now());
        UUID id = usuarios.saveAndFlush(u).getUsuariosId();
        usuarioIds.add(id);
        return id;
    }
    private UsuarioPrincipal principal(UUID id) { return new UsuarioPrincipal(usuarios.findById(id).orElseThrow()); }
    private static void esperar(CountDownLatch latch) {
        try { if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("Tiempo agotado"); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new AssertionError(ex); }
    }
}
