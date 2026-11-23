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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/** Verifica los bloqueos con transacciones y conexiones reales de PostgreSQL. */
@SpringBootTest(properties = {
        "mjrenew.stripe.transfers-enabled=false",
        "mjrenew.stripe.fee-reconciliation-enabled=false",
        "mjrenew.stripe.reconciliation-enabled=false"
})
class CapacidadRestauradorIntegrationTest {
    @Autowired RestauradorService servicio;
    @Autowired AntiguedadRepository antiguedades;
    @Autowired UsuarioRepository usuarios;
    @Autowired TransactionTemplate tx;
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
        servicio.marcarRestauracionLista(terminada, restauradorId);
        servicio.iniciarRestauracion(siguiente, restauradorId);

        assertEquals(2, contarActivas());
        assertEquals(EstadoAntiguedad.RESTAURACION_LISTA,
                antiguedades.findById(terminada).orElseThrow().getEstadoActualAntiguedad());
        assertNotNull(antiguedades.findById(siguiente).orElseThrow().getRestauracionInicioAntiguedad());
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
