package com.mjrenew.mjrenew_backend.restaurador;

import com.mjrenew.mjrenew_backend.nucleo.almacenamiento.AlmacenamientoService;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository.AntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoMueble;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoUsuario;
import com.mjrenew.mjrenew_backend.nucleo.exception.TransicionEstadoInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.nucleo.usuario.repository.UsuarioRepository;
import com.mjrenew.mjrenew_backend.restaurador.service.RestauradorService;
import com.mjrenew.mjrenew_backend.restaurador.service.CierreRestauracionService;
import com.mjrenew.mjrenew_backend.propietario.service.AntiguedadService;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadDetalleResponse;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.CyclicBarrier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Verifica los bloqueos con transacciones y conexiones reales de PostgreSQL. */
@SpringBootTest(properties = {
        "mjrenew.stripe.transfers-enabled=false",
        "mjrenew.stripe.fee-reconciliation-enabled=false",
        "mjrenew.stripe.reconciliation-enabled=false"
})
@AutoConfigureMockMvc
class CapacidadRestauradorIntegrationTest {
    @Autowired RestauradorService servicio;
    @Autowired AntiguedadRepository antiguedades;
    @Autowired UsuarioRepository usuarios;
    @Autowired TransactionTemplate tx;
    @Autowired EntityManager entityManager;
    @Autowired CierreRestauracionService cierres;
    @Autowired AntiguedadService propietario;
    @Autowired MockMvc mvc;
    @MockitoBean AlmacenamientoService almacenamiento;

    private final List<UUID> antiguedadIds = new ArrayList<>();
    private UUID restauradorId;
    private UUID propietarioId;

    @BeforeEach
    void preparar() {
        tx.executeWithoutResult(estado -> {
            restauradorId = crearUsuario(TipoUsuario.RESTAURADOR).getUsuariosId();
            propietarioId = crearUsuario(TipoUsuario.PROPIETARIO).getUsuariosId();
        });
    }

    @AfterEach
    void limpiarSoloDatosDeEstaPrueba() {
        tx.executeWithoutResult(estado -> {
            entityManager.createQuery("delete from FotografiaAntiguedad f where f.antiguedad.antiguedadesId in :ids")
                    .setParameter("ids", antiguedadIds).executeUpdate();
            antiguedades.deleteAllByIdInBatch(antiguedadIds);
            usuarios.deleteAllByIdInBatch(List.of(restauradorId, propietarioId));
        });
    }

    @Test
    void dosIniciosConcurrentesCompitenPorElUltimoLugar() throws Exception {
        crearAntiguedad(EstadoAntiguedad.AVANCE_PUBLICADO);
        UUID primera = crearAntiguedad(EstadoAntiguedad.RECIBIDO_EN_TALLER);
        UUID segunda = crearAntiguedad(EstadoAntiguedad.RECIBIDO_EN_TALLER);
        CountDownLatch inicioSinCommit = new CountDownLatch(1);
        CountDownLatch permitirCommit = new CountDownLatch(1);
        CountDownLatch segundoIntento = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var primerInicio = executor.submit(() -> tx.executeWithoutResult(estado -> {
                servicio.iniciarRestauracion(primera, restauradorId);
                inicioSinCommit.countDown();
                esperar(permitirCommit);
            }));
            assertTrue(inicioSinCommit.await(10, TimeUnit.SECONDS));

            var segundoInicio = executor.submit(() -> {
                segundoIntento.countDown();
                assertThrows(TransicionEstadoInvalidaException.class,
                        () -> servicio.iniciarRestauracion(segunda, restauradorId));
            });
            assertTrue(segundoIntento.await(10, TimeUnit.SECONDS));
            // La segunda conexión espera a que se confirme el inicio de la primera.
            assertThrows(TimeoutException.class, () -> segundoInicio.get(300, TimeUnit.MILLISECONDS));
            permitirCommit.countDown();
            primerInicio.get(10, TimeUnit.SECONDS);
            segundoInicio.get(10, TimeUnit.SECONDS);
        } finally {
            permitirCommit.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
        }

        assertEquals(2, contarActivas());
        Antiguedad pendiente = antiguedades.findById(segunda).orElseThrow();
        assertEquals(EstadoAntiguedad.RECIBIDO_EN_TALLER, pendiente.getEstadoActualAntiguedad());
        assertNull(pendiente.getRestauracionInicioAntiguedad());
    }

    @Test
    void terminarUnaRestauracionPermiteIniciarLaSiguiente() {
        UUID terminada = crearAntiguedad(EstadoAntiguedad.AVANCE_PUBLICADO);
        crearAntiguedad(EstadoAntiguedad.EN_RESTAURACION);
        UUID siguiente = crearAntiguedad(EstadoAntiguedad.RECIBIDO_EN_TALLER);

        assertThrows(TransicionEstadoInvalidaException.class,
                () -> servicio.iniciarRestauracion(siguiente, restauradorId));
        simularAlmacenamiento();
        cierres.cerrar(terminada, restauradorId, CierreRestauracionServiceTest.fotos());
        servicio.iniciarRestauracion(siguiente, restauradorId);

        assertEquals(2, contarActivas());
        assertEquals(EstadoAntiguedad.RESTAURACION_LISTA,
                antiguedades.findById(terminada).orElseThrow().getEstadoActualAntiguedad());
        assertNotNull(antiguedades.findById(siguiente).orElseThrow().getRestauracionInicioAntiguedad());
    }

    @Test
    void fotosYEstadoSonVisiblesAlPropietarioTrasCerrarSinTransaccionDuranteSubida() {
        UUID id = crearAntiguedad(EstadoAntiguedad.EN_RESTAURACION);
        simularAlmacenamiento();
        tx.executeWithoutResult(estado -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            var respuesta = cierres.cerrar(id, restauradorId, CierreRestauracionServiceTest.fotos());
            assertEquals(4, respuesta.fotosEstadoFinal().size());
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
        });
        var detalle = propietario.obtenerDetalle(id, propietarioId);
        assertEquals(EstadoAntiguedad.RESTAURACION_LISTA, detalle.estadoActualAntiguedad());
        assertEquals(4, detalle.fotosEstadoFinal().size());
        var fecha = antiguedades.findById(id).orElseThrow().getRestauracionFinAntiguedad();
        assertEquals(detalle.fotosEstadoFinal(), cierres.cerrar(id, restauradorId,
                CierreRestauracionServiceTest.fotos()).fotosEstadoFinal());
        assertEquals(fecha, antiguedades.findById(id).orElseThrow().getRestauracionFinAntiguedad());
        verify(almacenamiento, times(4)).subirArchivo(any(), eq("fotos-finales"));
        verify(almacenamiento, never()).eliminarArchivo(anyString());
    }

    @Test
    void falloEnTransaccionRevierteLasFotosYElCierre() {
        UUID id = crearAntiguedad(EstadoAntiguedad.AVANCE_PUBLICADO);
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(estado -> {
            servicio.guardarCierreRestauracion(id, restauradorId, List.of("u1", "u2", "u3", "u4"));
            throw new IllegalStateException("Error antes del commit");
        }));
        var detalle = propietario.obtenerDetalle(id, propietarioId);
        assertEquals(EstadoAntiguedad.AVANCE_PUBLICADO, detalle.estadoActualAntiguedad());
        assertTrue(detalle.fotosEstadoFinal().isEmpty());
        assertNull(antiguedades.findById(id).orElseThrow().getRestauracionFinAntiguedad());
    }

    @Test
    void noSePuedeCerrarSinEvidenciaAunqueSeLlameAlServicioDirectamente() {
        UUID id = crearAntiguedad(EstadoAntiguedad.EN_RESTAURACION);
        assertThrows(com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException.class,
                () -> servicio.marcarRestauracionLista(id, restauradorId));
        assertEquals(EstadoAntiguedad.EN_RESTAURACION,
                antiguedades.findById(id).orElseThrow().getEstadoActualAntiguedad());
    }

    @Test
    void dosCierresConcurrentesGuardanUnSoloLoteFinal() throws Exception {
        UUID id = crearAntiguedad(EstadoAntiguedad.EN_RESTAURACION);
        var lotesSubidos = new CyclicBarrier(2);
        when(almacenamiento.subirArchivo(any(), eq("fotos-finales"))).thenAnswer(invocacion -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            org.springframework.web.multipart.MultipartFile foto = invocacion.getArgument(0);
            if (foto.getOriginalFilename().equals("final-3.jpg")) lotesSubidos.await(10, TimeUnit.SECONDS);
            return "http://almacenamiento.test/finales/" + UUID.randomUUID();
        });
        var executor = Executors.newFixedThreadPool(2);
        try {
            var primerCierre = executor.submit(() -> cierres.cerrar(id, restauradorId, CierreRestauracionServiceTest.fotos()));
            var segundoCierre = executor.submit(() -> cierres.cerrar(id, restauradorId, CierreRestauracionServiceTest.fotos()));
            AntiguedadDetalleResponse primero = primerCierre.get(15, TimeUnit.SECONDS);
            assertEquals(primero.fotosEstadoFinal(), segundoCierre.get(15, TimeUnit.SECONDS).fotosEstadoFinal());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
        }
        assertEquals(4, propietario.obtenerDetalle(id, propietarioId).fotosEstadoFinal().size());
        verify(almacenamiento, times(4)).eliminarArchivo(anyString());
    }

    private void simularAlmacenamiento() {
        when(almacenamiento.subirArchivo(any(), eq("fotos-finales"))).thenAnswer(invocacion -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            return "http://almacenamiento.test/finales/" + UUID.randomUUID();
        });
    }

    @Test
    void losPanelesRenderizanElFormularioYLaGaleriaFinal() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/restaurador"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("id=\"cierre-fotos\"")));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/propietario"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Utils.mostrarFotosFinales(cont, a.fotosEstadoFinal)")));
    }

    @Test
    void permiteCompletarUnCierreAnteriorConservandoSuFechaOriginal() {
        UUID id = crearAntiguedad(EstadoAntiguedad.RESTAURACION_LISTA);
        OffsetDateTime fechaOriginal = OffsetDateTime.now().minusHours(1).withNano(0);
        tx.executeWithoutResult(estado -> antiguedades.findById(id).orElseThrow()
                .setRestauracionFinAntiguedad(fechaOriginal));
        simularAlmacenamiento();

        var detalle = cierres.cerrar(id, restauradorId, CierreRestauracionServiceTest.fotos());

        assertEquals(4, detalle.fotosEstadoFinal().size());
        assertEquals(fechaOriginal.toInstant(), antiguedades.findById(id).orElseThrow()
                .getRestauracionFinAntiguedad().toInstant());
    }

    @Test
    void losTrabajosDeOtroRestauradorNoOcupanMisLugares() {
        crearAntiguedad(EstadoAntiguedad.EN_RESTAURACION);
        crearAntiguedad(EstadoAntiguedad.AVANCE_PUBLICADO);
        UUID otroRestaurador = tx.execute(estado -> crearUsuario(TipoUsuario.RESTAURADOR).getUsuariosId());
        try {
            UUID nueva = crearAntiguedad(EstadoAntiguedad.RECIBIDO_EN_TALLER);
            tx.executeWithoutResult(estado -> antiguedades.findById(nueva).orElseThrow()
                    .setRestaurador(usuarios.getReferenceById(otroRestaurador)));

            servicio.iniciarRestauracion(nueva, otroRestaurador);

            assertEquals(2, contarActivas());
            assertEquals(EstadoAntiguedad.EN_RESTAURACION,
                    antiguedades.findById(nueva).orElseThrow().getEstadoActualAntiguedad());
        } finally {
            tx.executeWithoutResult(estado -> {
                antiguedades.deleteAllByIdInBatch(antiguedadIds);
                usuarios.deleteById(otroRestaurador);
            });
        }
    }

    private long contarActivas() {
        return antiguedades.countByRestaurador_UsuariosIdAndEstadoActualAntiguedadIn(restauradorId,
                List.of(EstadoAntiguedad.EN_RESTAURACION, EstadoAntiguedad.AVANCE_PUBLICADO));
    }

    private UUID crearAntiguedad(EstadoAntiguedad estado) {
        UUID id = tx.execute(transaccion -> {
            Antiguedad antiguedad = new Antiguedad();
            antiguedad.setPropietario(usuarios.getReferenceById(propietarioId));
            antiguedad.setRestaurador(usuarios.getReferenceById(restauradorId));
            antiguedad.setTipoMueble(TipoMueble.MESA);
            antiguedad.setEstadoActualAntiguedad(estado);
            antiguedad.setRegistradaEnAntiguedad(OffsetDateTime.now());
            return antiguedades.saveAndFlush(antiguedad).getAntiguedadesId();
        });
        antiguedadIds.add(id);
        return id;
    }

    private Usuario crearUsuario(TipoUsuario rol) {
        Usuario usuario = new Usuario();
        usuario.setNombreCompletoUsuario("Prueba de capacidad");
        usuario.setCorreoElectronicoUsuario("capacidad-" + UUID.randomUUID() + "@example.invalid");
        usuario.setContrasenaHashUsuario("hash-de-prueba-sin-login");
        usuario.setTipoUsuario(rol);
        usuario.setActivoUsuario(true);
        usuario.setCorreoVerificadoUsuario(false);
        usuario.setRegistradoEnUsuario(OffsetDateTime.now());
        return usuarios.saveAndFlush(usuario);
    }

    private static void esperar(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("Tiempo de espera agotado");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError(ex);
        }
    }
}
