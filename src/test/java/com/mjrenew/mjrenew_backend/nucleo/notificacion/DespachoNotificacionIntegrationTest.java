package com.mjrenew.mjrenew_backend.nucleo.notificacion;

import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository.AntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.enums.*;
import com.mjrenew.mjrenew_backend.nucleo.notificacion.service.EntregaNotificacionService;
import com.mjrenew.mjrenew_backend.nucleo.notificacion.repository.NotificacionRepository;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.nucleo.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Comprueba los listeners reales de Spring; el transporte se sustituye para no tocar otros pendientes. */
@SpringBootTest(properties = {"mjrenew.stripe.transfers-enabled=false", "mjrenew.stripe.fee-reconciliation-enabled=false",
        "mjrenew.stripe.reconciliation-enabled=false", "mjrenew.notificaciones.correo-habilitado=true",
        "mjrenew.notificaciones.intervalo-ms=3600000"})
class DespachoNotificacionIntegrationTest {
    @Autowired ApplicationEventPublisher eventos;
    @Autowired TransactionTemplate tx;
    @Autowired UsuarioRepository usuarios;
    @Autowired AntiguedadRepository antiguedades;
    @Autowired NotificacionRepository notificaciones;
    @MockitoBean EntregaNotificacionService entregas;
    private UUID usuarioId, antiguedadId;

    @BeforeEach
    void preparar() {
        tx.executeWithoutResult(s -> {
            var u = new Usuario(); u.setNombreCompletoUsuario("Usuario Prueba");
            u.setCorreoElectronicoUsuario("despacho-" + UUID.randomUUID() + "@example.invalid");
            u.setContrasenaHashUsuario("hash-sin-login"); u.setActivoUsuario(true); u.setCorreoVerificadoUsuario(true);
            u.setTipoUsuario(TipoUsuario.PROPIETARIO); u.setRegistradoEnUsuario(OffsetDateTime.now());
            usuarioId = usuarios.save(u).getUsuariosId();
            var a = new Antiguedad(); a.setPropietario(u); a.setTipoMueble(TipoMueble.MESA);
            a.setEstadoActualAntiguedad(EstadoAntiguedad.EN_RESTAURACION); a.setRegistradaEnAntiguedad(OffsetDateTime.now());
            antiguedadId = antiguedades.saveAndFlush(a).getAntiguedadesId();
        });
    }

    @AfterEach
    void limpiar() {
        tx.executeWithoutResult(s -> { antiguedades.deleteById(antiguedadId); antiguedades.flush(); usuarios.deleteById(usuarioId); });
    }

    @Test
    void despachoHabilitadoEsperaElCommitYEncuentraElAvisoPersistido() {
        doAnswer(invocacion -> {
            assertEquals(1, notificaciones.countByDestinatarioIdAndLeidoEnIsNull(usuarioId));
            return null;
        }).when(entregas).procesarLote();
        tx.executeWithoutResult(s -> { eventos.publishEvent(evento()); verifyNoInteractions(entregas); });
        verify(entregas, timeout(3000)).procesarLote();
    }

    @Test
    void rollbackNoDespachaNiConservaElAviso() {
        tx.executeWithoutResult(s -> { eventos.publishEvent(evento()); s.setRollbackOnly(); });
        verify(entregas, after(200).never()).procesarLote();
        assertEquals(0, notificaciones.countByDestinatarioIdAndLeidoEnIsNull(usuarioId));
    }

    private NotificacionRestauracionEmitida evento() {
        return new NotificacionRestauracionEmitida(TipoNotificacion.AVANCE_PUBLICADO, antiguedadId, UUID.randomUUID(), usuarioId);
    }
}
