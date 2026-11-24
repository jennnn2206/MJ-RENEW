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
import com.mjrenew.mjrenew_backend.transportista.mapper.TrasladoMapper;
import com.mjrenew.mjrenew_backend.transportista.repository.TrasladoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.transportista.service.TrasladoAntiguedadService;
import com.mjrenew.mjrenew_backend.transportista.service.ValidadorVentanaEntrega;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgramacionEntregaPropietarioTest {
    @Mock TrasladoAntiguedadRepository traslados;
    @Mock AntiguedadRepository antiguedades;
    @Mock CatalogoAntiguedadRepository catalogos;
    @Mock FotografiaAntiguedadRepository fotos;
    @Mock UsuarioRepository usuarios;
    @Mock TrasladoMapper mapper;
    @Mock AlmacenamientoService almacenamiento;
    @Spy ValidadorVentanaEntrega ventanas = new ValidadorVentanaEntrega();
    @InjectMocks TrasladoAntiguedadService servicio;
    private final UUID id = UUID.randomUUID();
    private final UUID propietarioId = UUID.randomUUID();
    private final LocalDate fecha = LocalDate.now().plusDays(1);
    private final LocalTime inicio = LocalTime.of(10, 0);
    private final LocalTime fin = LocalTime.of(12, 0);

    @Test
    void guardaHorarioYDireccionesDelServidorSinTransportista() {
        var pieza = pieza();
        solicitar();
        verify(traslados).save(argThat(t -> t.getTipoTraslado() == TipoTraslado.ENTREGA_PROPIETARIO
                && t.getTransportista() == null && t.getNumeroIntentoTraslado() == 1
                && t.getFechaAcordadaTraslado().equals(fecha) && t.getHoraInicioAcordadaTraslado().equals(inicio)
                && t.getHoraFinAcordadaTraslado().equals(fin) && t.getDireccionOrigenTraslado().equals("Taller")
                && t.getDireccionDestinoTraslado().equals("Domicilio")));
        assertEquals(EstadoAntiguedad.SOLICITUD_DEVOLUCION_TALLER, pieza.getEstadoActualAntiguedad());
    }

    @Test
    void repetirOAjustarHorarioMantieneElMismoIntento() {
        var pieza = pieza();
        var solicitud = solicitud(pieza);
        when(traslados.findFirstByAntiguedad_AntiguedadesIdAndTipoTrasladoOrderByAgendadoEnTrasladoDesc(
                id, TipoTraslado.ENTREGA_PROPIETARIO)).thenReturn(Optional.of(solicitud));
        solicitar();
        servicio.solicitarEntregaPropietario(id, propietarioId, fecha.plusDays(1), inicio, fin);
        verify(traslados, times(2)).save(solicitud);
        verify(traslados, never()).countByAntiguedad_AntiguedadesIdAndTipoTraslado(any(), any());
        assertEquals(fecha.plusDays(1), solicitud.getFechaAcordadaTraslado());
        assertEquals((short) 1, solicitud.getNumeroIntentoTraslado());
    }

    @Test
    void propietarioAjenoNoPuedeSolicitarEntrega() {
        assertThrows(RecursoNoEncontradoException.class, this::solicitar);
        verifyNoInteractions(traslados);
    }

    @Test
    void rechazaFechaPasadaYHorarioInvertido() {
        pieza();
        assertThrows(SolicitudInvalidaException.class, () -> servicio.solicitarEntregaPropietario(
                id, propietarioId, fecha.minusDays(2), inicio, fin));
        assertThrows(SolicitudInvalidaException.class, () -> servicio.solicitarEntregaPropietario(
                id, propietarioId, fecha, fin, inicio));
        verify(traslados, never()).save(any());
    }

    @Test
    void exigeDireccionesDelTallerYDelPropietario() {
        var pieza = pieza();
        pieza.getRestaurador().setDireccionTextoUsuario(" ");
        assertThrows(SolicitudInvalidaException.class, this::solicitar);
        pieza.getRestaurador().setDireccionTextoUsuario("Taller");
        pieza.getPropietario().setDireccionTextoUsuario(null);
        assertThrows(SolicitudInvalidaException.class, this::solicitar);
        verify(traslados, never()).save(any());
    }

    @Test
    void rechazaCuartoIntento() {
        pieza();
        when(traslados.countByAntiguedad_AntiguedadesIdAndTipoTraslado(id, TipoTraslado.ENTREGA_PROPIETARIO))
                .thenReturn(3L);
        assertThrows(TransicionEstadoInvalidaException.class, this::solicitar);
        verify(traslados, never()).save(any());
    }

    @Test
    void noModificaUnaEntregaYaAgendada() {
        pieza().setEstadoActualAntiguedad(EstadoAntiguedad.HORARIO_ENTREGA_AGENDADO);
        assertThrows(TransicionEstadoInvalidaException.class, this::solicitar);
        verifyNoInteractions(traslados);
    }

    @Test
    void asignaTransportistaRespetandoLaVentana() {
        var pieza = new Antiguedad();
        pieza.setEstadoActualAntiguedad(EstadoAntiguedad.SOLICITUD_DEVOLUCION_TALLER);
        var solicitud = paraAsignar(pieza);
        UUID transportistaId = UUID.randomUUID();
        Usuario transportista = new Usuario();
        transportista.setUsuariosId(transportistaId);
        transportista.setTipoUsuario(TipoUsuario.TRANSPORTISTA);
        transportista.setActivoUsuario(true);
        when(usuarios.findById(transportistaId)).thenReturn(Optional.of(transportista));

        servicio.asignarTransportistaEntregaPropietario(solicitud.getTrasladosAntiguedadId(), transportistaId);

        assertSame(transportista, solicitud.getTransportista());
        assertEquals(fecha, solicitud.getFechaAcordadaTraslado());
        assertEquals(inicio, solicitud.getHoraInicioAcordadaTraslado());
        assertEquals(fin, solicitud.getHoraFinAcordadaTraslado());
        assertEquals(EstadoAntiguedad.HORARIO_ENTREGA_AGENDADO, pieza.getEstadoActualAntiguedad());
    }

    @Test
    void noAsignaHorarioVencido() {
        var pieza = new Antiguedad();
        pieza.setEstadoActualAntiguedad(EstadoAntiguedad.SOLICITUD_DEVOLUCION_TALLER);
        var solicitud = paraAsignar(pieza);
        solicitud.setFechaAcordadaTraslado(fecha.minusDays(2));
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.asignarTransportistaEntregaPropietario(solicitud.getTrasladosAntiguedadId(), UUID.randomUUID()));
        verifyNoInteractions(usuarios);
    }

    @Test
    void rechazaTransportistaInactivo() {
        var pieza = new Antiguedad();
        pieza.setEstadoActualAntiguedad(EstadoAntiguedad.SOLICITUD_DEVOLUCION_TALLER);
        var solicitud = paraAsignar(pieza);
        UUID transportistaId = UUID.randomUUID();
        Usuario transportista = new Usuario();
        transportista.setTipoUsuario(TipoUsuario.TRANSPORTISTA);
        transportista.setActivoUsuario(false);
        when(usuarios.findById(transportistaId)).thenReturn(Optional.of(transportista));
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.asignarTransportistaEntregaPropietario(solicitud.getTrasladosAntiguedadId(), transportistaId));
        assertNull(solicitud.getTransportista());
    }

    @Test
    void reintentoDeAsignacionEsIdempotenteYNoPermiteOtroTransportista() {
        var solicitud = paraAsignar(new Antiguedad());
        Usuario asignado = new Usuario();
        asignado.setUsuariosId(UUID.randomUUID());
        solicitud.setTransportista(asignado);
        servicio.asignarTransportistaEntregaPropietario(solicitud.getTrasladosAntiguedadId(), asignado.getUsuariosId());
        assertThrows(TransicionEstadoInvalidaException.class, () -> servicio.asignarTransportistaEntregaPropietario(
                solicitud.getTrasladosAntiguedadId(), UUID.randomUUID()));
        assertSame(asignado, solicitud.getTransportista());
        verifyNoInteractions(usuarios);
    }

    private void solicitar() { servicio.solicitarEntregaPropietario(id, propietarioId, fecha, inicio, fin); }

    private Antiguedad pieza() {
        Antiguedad pieza = new Antiguedad();
        pieza.setAntiguedadesId(id);
        pieza.setEstadoActualAntiguedad(EstadoAntiguedad.DECISION_EN_TALLER);
        Usuario propietario = new Usuario();
        propietario.setUsuariosId(propietarioId);
        propietario.setDireccionTextoUsuario("Domicilio");
        Usuario restaurador = new Usuario();
        restaurador.setDireccionTextoUsuario("Taller");
        pieza.setPropietario(propietario);
        pieza.setRestaurador(restaurador);
        when(antiguedades.bloquearDelPropietario(id, propietarioId)).thenReturn(Optional.of(pieza));
        return pieza;
    }

    private TrasladoAntiguedad solicitud(Antiguedad pieza) {
        var solicitud = new TrasladoAntiguedad();
        solicitud.setAntiguedad(pieza);
        solicitud.setTrasladosAntiguedadId(UUID.randomUUID());
        solicitud.setTipoTraslado(TipoTraslado.ENTREGA_PROPIETARIO);
        solicitud.setResultadoTraslado(ResultadoTraslado.PENDIENTE);
        solicitud.setNumeroIntentoTraslado((short) 1);
        solicitud.setFechaAcordadaTraslado(fecha);
        solicitud.setHoraInicioAcordadaTraslado(inicio);
        solicitud.setHoraFinAcordadaTraslado(fin);
        return solicitud;
    }

    private TrasladoAntiguedad paraAsignar(Antiguedad pieza) {
        var solicitud = solicitud(pieza);
        when(traslados.buscarAntiguedadId(solicitud.getTrasladosAntiguedadId())).thenReturn(Optional.of(id));
        when(antiguedades.bloquearParaConfirmarPago(id)).thenReturn(Optional.of(pieza));
        when(traslados.buscarParaAsignacion(solicitud.getTrasladosAntiguedadId())).thenReturn(Optional.of(solicitud));
        return solicitud;
    }
}
