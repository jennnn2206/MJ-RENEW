package com.mjrenew.mjrenew_backend.transportista;

import com.mjrenew.mjrenew_backend.catalogo.repository.CatalogoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.almacenamiento.AlmacenamientoService;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository.AntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.enums.*;
import com.mjrenew.mjrenew_backend.nucleo.exception.*;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.nucleo.usuario.repository.UsuarioRepository;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.transportista.entity.TrasladoAntiguedad;
import com.mjrenew.mjrenew_backend.transportista.repository.TrasladoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.transportista.mapper.TrasladoMapper;
import com.mjrenew.mjrenew_backend.transportista.service.*;
import com.mjrenew.mjrenew_backend.transportista.dto.ConfirmarLlegadaRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcuseRecoleccionServiceTest {
    @Mock TrasladoAntiguedadRepository traslados;
    @Mock AntiguedadRepository antiguedades;
    @Mock CatalogoAntiguedadRepository catalogos;
    @Mock FotografiaAntiguedadRepository fotos;
    @Mock UsuarioRepository usuarios;
    @Mock TrasladoMapper mapper;
    @Mock AlmacenamientoService almacenamiento;
    @Mock ValidadorVentanaEntrega ventanas;
    @InjectMocks TrasladoAntiguedadService servicio;
    private final UUID trasladoId = UUID.randomUUID();
    private final UUID propietarioId = UUID.randomUUID();
    private final UUID transportistaId = UUID.randomUUID();
    private Antiguedad antiguedad;
    private TrasladoAntiguedad traslado;

    @BeforeEach
    void preparar() {
        antiguedad = new Antiguedad();
        antiguedad.setAntiguedadesId(UUID.randomUUID());
        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.EN_RECOLECCION);
        Usuario propietario = new Usuario();
        propietario.setUsuariosId(propietarioId);
        antiguedad.setPropietario(propietario);
        Usuario transportista = new Usuario();
        transportista.setUsuariosId(transportistaId);
        traslado = new TrasladoAntiguedad();
        traslado.setAntiguedad(antiguedad);
        traslado.setTrasladosAntiguedadId(trasladoId);
        traslado.setTransportista(transportista);
        traslado.setTipoTraslado(TipoTraslado.RECOLECCION);
        traslado.setResultadoTraslado(ResultadoTraslado.PENDIENTE);
        traslado.setNumeroIntentoTraslado((short) 1);
        traslado.setSalidaConfirmadaEnTraslado(OffsetDateTime.now().minusMinutes(1));
        when(traslados.buscarAntiguedadId(trasladoId)).thenReturn(Optional.of(antiguedad.getAntiguedadesId()));
    }

    @Test
    void registraIdentidadYFechaUnaSolaVez() {
        paraPropietario();
        cuatroFotos();
        servicio.confirmarEntregaRecoleccion(trasladoId, propietarioId);
        var fecha = traslado.getRecoleccionConfirmadaEn();
        assertNotNull(fecha);
        assertEquals(propietarioId, traslado.getRecoleccionConfirmadaPor().getUsuariosId());
        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.RECIBIDO_EN_TALLER);
        traslado.setResultadoTraslado(ResultadoTraslado.EXITOSO);
        servicio.confirmarEntregaRecoleccion(trasladoId, propietarioId);
        assertEquals(fecha, traslado.getRecoleccionConfirmadaEn());
        verify(fotos, times(1)).countByTraslado_TrasladosAntiguedadIdAndTipoFotografia(trasladoId, TipoFotografia.TRASLADO_SALIDA);
    }

    @Test
    void noAceptaFotosDeOtroTraslado() {
        paraPropietario();
        assertThrows(SolicitudInvalidaException.class, () -> servicio.confirmarEntregaRecoleccion(trasladoId, propietarioId));
        verify(fotos).countByTraslado_TrasladosAntiguedadIdAndTipoFotografia(trasladoId, TipoFotografia.TRASLADO_SALIDA);
        assertNull(traslado.getRecoleccionConfirmadaEn());
    }

    @Test
    void propietarioAjenoNoPuedeConsultarNiConfirmarElTraslado() {
        UUID ajeno = UUID.randomUUID();
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.confirmarEntregaRecoleccion(trasladoId, ajeno));
        verify(traslados, never()).buscarParaAsignacion(any());
        verifyNoInteractions(fotos);
    }

    @Test
    void noConfirmaAntesDeLaRecoleccionNiUnIntentoFallido() {
        paraPropietario();
        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.HORARIO_RECOLECCION_AGENDADO);
        assertThrows(TransicionEstadoInvalidaException.class, () -> servicio.confirmarEntregaRecoleccion(trasladoId, propietarioId));
        traslado.setResultadoTraslado(ResultadoTraslado.NADIE_EN_CASA);
        assertThrows(TransicionEstadoInvalidaException.class, () -> servicio.confirmarEntregaRecoleccion(trasladoId, propietarioId));
        assertNull(traslado.getRecoleccionConfirmadaEn());
    }

    @Test
    void noAceptaUnaEntregaComoSiFueraRecoleccion() {
        paraPropietario();
        traslado.setTipoTraslado(TipoTraslado.ENTREGA_PROPIETARIO);
        assertThrows(SolicitudInvalidaException.class, () -> servicio.confirmarEntregaRecoleccion(trasladoId, propietarioId));
        assertNull(traslado.getRecoleccionConfirmadaEn());
    }

    @Test
    void llegadaExigeElAcuseDelPropietario() {
        paraTransportista();
        assertThrows(TransicionEstadoInvalidaException.class,
                () -> servicio.confirmarLlegadaTaller(trasladoId, transportistaId, new ConfirmarLlegadaRequest(List.of())));
        assertEquals(EstadoAntiguedad.EN_RECOLECCION, antiguedad.getEstadoActualAntiguedad());
        assertEquals(ResultadoTraslado.PENDIENTE, traslado.getResultadoTraslado());
    }

    @Test
    void llegadaVuelveAComprobarLasCuatroEvidencias() {
        paraTransportista();
        traslado.confirmarEntregaRecoleccion(antiguedad.getPropietario(), OffsetDateTime.now());
        when(fotos.countByTraslado_TrasladosAntiguedadIdAndTipoFotografia(trasladoId, TipoFotografia.TRASLADO_SALIDA))
                .thenReturn(3L);
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.confirmarLlegadaTaller(trasladoId, transportistaId, new ConfirmarLlegadaRequest(List.of())));
        assertEquals(ResultadoTraslado.PENDIENTE, traslado.getResultadoTraslado());
    }

    @Test
    void llegadaConAcuseYEvidenciaCompletaAvanzaAlTaller() {
        paraTransportista();
        cuatroFotos();
        traslado.confirmarEntregaRecoleccion(antiguedad.getPropietario(), OffsetDateTime.now());
        servicio.confirmarLlegadaTaller(trasladoId, transportistaId, new ConfirmarLlegadaRequest(List.of()));
        assertEquals(ResultadoTraslado.EXITOSO, traslado.getResultadoTraslado());
        assertEquals(EstadoAntiguedad.RECIBIDO_EN_TALLER, antiguedad.getEstadoActualAntiguedad());
        assertNotNull(traslado.getEjecutadoEnTraslado());
    }

    @Test
    void conservaUnaLlegadaHistoricaSinInventarUnAcuse() {
        paraTransportista();
        traslado.setResultadoTraslado(ResultadoTraslado.EXITOSO);
        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.EN_RESTAURACION);
        servicio.confirmarLlegadaTaller(trasladoId, transportistaId, new ConfirmarLlegadaRequest(List.of()));
        assertNull(traslado.getRecoleccionConfirmadaEn());
        assertEquals(EstadoAntiguedad.EN_RESTAURACION, antiguedad.getEstadoActualAntiguedad());
    }

    @Test
    void reintentoDeSalidaNoDuplicaFotosNiModificaLaFecha() {
        paraTransportista();
        cuatroFotos();
        var fecha = traslado.getSalidaConfirmadaEnTraslado();
        servicio.registrarSalidaRecoleccion(trasladoId, transportistaId, List.of());
        assertEquals(fecha, traslado.getSalidaConfirmadaEnTraslado());
        verifyNoInteractions(almacenamiento);
        verify(fotos, never()).save(any());
    }

    private void paraPropietario() {
        when(antiguedades.bloquearDelPropietario(antiguedad.getAntiguedadesId(), propietarioId)).thenReturn(Optional.of(antiguedad));
        when(traslados.buscarParaAsignacion(trasladoId)).thenReturn(Optional.of(traslado));
    }
    private void paraTransportista() {
        when(antiguedades.bloquearParaConfirmarPago(antiguedad.getAntiguedadesId())).thenReturn(Optional.of(antiguedad));
        when(traslados.buscarParaAsignacion(trasladoId)).thenReturn(Optional.of(traslado));
    }
    private void cuatroFotos() {
        when(fotos.countByTraslado_TrasladosAntiguedadIdAndTipoFotografia(trasladoId, TipoFotografia.TRASLADO_SALIDA))
                .thenReturn(4L);
    }
}
