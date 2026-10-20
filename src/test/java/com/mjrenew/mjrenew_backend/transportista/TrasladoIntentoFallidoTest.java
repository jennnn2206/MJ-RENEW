package com.mjrenew.mjrenew_backend.transportista;

import com.mjrenew.mjrenew_backend.nucleo.almacenamiento.AlmacenamientoService;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository.AntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.ResultadoTraslado;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoFotografia;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTraslado;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoUsuario;
import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.exception.TransicionEstadoInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.nucleo.usuario.repository.UsuarioRepository;
import com.mjrenew.mjrenew_backend.propietario.entity.FotografiaAntiguedad;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.transportista.dto.CrearTrasladoRequest;
import com.mjrenew.mjrenew_backend.transportista.dto.RegistrarIntentoFallidoRequest;
import com.mjrenew.mjrenew_backend.transportista.entity.TrasladoAntiguedad;
import com.mjrenew.mjrenew_backend.transportista.mapper.TrasladoMapper;
import com.mjrenew.mjrenew_backend.transportista.repository.TrasladoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.transportista.service.TrasladoAntiguedadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Pruebas unitarias sin PostgreSQL ni MinIO: intento fallido y reagendado de traslados. */
@ExtendWith(MockitoExtension.class)
class TrasladoIntentoFallidoTest {
    @Mock TrasladoAntiguedadRepository trasladoRepo;
    @Mock AntiguedadRepository antiguedadRepo;
    @Mock FotografiaAntiguedadRepository fotoRepo;
    @Mock UsuarioRepository usuarioRepo;
    @Mock TrasladoMapper mapper;
    @Mock AlmacenamientoService almacenamiento;
    @InjectMocks TrasladoAntiguedadService servicio;

    private final UUID transportistaId = UUID.randomUUID();
    private final RegistrarIntentoFallidoRequest nota = new RegistrarIntentoFallidoRequest("  No había nadie en casa  ");

    private TrasladoAntiguedad traslado(TipoTraslado tipo, EstadoAntiguedad estadoPieza) {
        Usuario transportista = new Usuario();
        transportista.setUsuariosId(transportistaId);
        Antiguedad antiguedad = new Antiguedad();
        antiguedad.setAntiguedadesId(UUID.randomUUID());
        antiguedad.setEstadoActualAntiguedad(estadoPieza);
        TrasladoAntiguedad t = new TrasladoAntiguedad();
        t.setTrasladosAntiguedadId(UUID.randomUUID());
        t.setTransportista(transportista);
        t.setAntiguedad(antiguedad);
        t.setTipoTraslado(tipo);
        t.setResultadoTraslado(ResultadoTraslado.PENDIENTE);
        when(trasladoRepo.findById(t.getTrasladosAntiguedadId())).thenReturn(Optional.of(t));
        return t;
    }

    @Test
    void entregaFallidaRegresaPiezaARestauracionListaYGuardaNota() {
        TrasladoAntiguedad t = traslado(TipoTraslado.ENTREGA_PROPIETARIO, EstadoAntiguedad.EN_DEVOLUCION_PROPIETARIO);
        when(trasladoRepo.save(t)).thenReturn(t);

        servicio.registrarIntentoFallido(t.getTrasladosAntiguedadId(), transportistaId, nota, null);

        assertEquals(ResultadoTraslado.NADIE_EN_CASA, t.getResultadoTraslado());
        assertEquals("No había nadie en casa", t.getMotivoFalloTraslado());
        assertNotNull(t.getEjecutadoEnTraslado());
        assertEquals(EstadoAntiguedad.RESTAURACION_LISTA, t.getAntiguedad().getEstadoActualAntiguedad());
        verifyNoInteractions(almacenamiento);
    }

    @Test
    void recoleccionFallidaRegresaPiezaAPagoEnEscrowYSubeFoto() {
        TrasladoAntiguedad t = traslado(TipoTraslado.RECOLECCION, EstadoAntiguedad.HORARIO_RECOLECCION_AGENDADO);
        when(trasladoRepo.save(t)).thenReturn(t);
        when(almacenamiento.subirArchivo(any(), eq("traslados"))).thenReturn("http://minio/traslados/x.jpg");
        MockMultipartFile foto = new MockMultipartFile("foto", "puerta.jpg", "image/jpeg", new byte[]{1, 2, 3});

        servicio.registrarIntentoFallido(t.getTrasladosAntiguedadId(), transportistaId, nota, foto);

        assertEquals(EstadoAntiguedad.PAGO_EN_ESCROW, t.getAntiguedad().getEstadoActualAntiguedad());
        ArgumentCaptor<FotografiaAntiguedad> captor = ArgumentCaptor.forClass(FotografiaAntiguedad.class);
        verify(fotoRepo).save(captor.capture());
        assertEquals(TipoFotografia.TRASLADO_INTENTO_FALLIDO, captor.getValue().getTipoFotografia());
        // Misma marca de tiempo: así el aviso al propietario encuentra la foto de este intento.
        assertEquals(t.getEjecutadoEnTraslado(), captor.getValue().getSubidaEnFotografia());
    }

    @Test
    void rechazaFotoQueNoEsImagen() {
        TrasladoAntiguedad t = traslado(TipoTraslado.ENTREGA_PROPIETARIO, EstadoAntiguedad.EN_DEVOLUCION_PROPIETARIO);
        MockMultipartFile pdf = new MockMultipartFile("foto", "x.pdf", "application/pdf", new byte[]{1});

        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.registrarIntentoFallido(t.getTrasladosAntiguedadId(), transportistaId, nota, pdf));
        assertEquals(ResultadoTraslado.PENDIENTE, t.getResultadoTraslado());
        verifyNoInteractions(almacenamiento);
    }

    @Test
    void noPermiteFallarEntregaAntesDeSalirDelTaller() {
        TrasladoAntiguedad t = traslado(TipoTraslado.ENTREGA_PROPIETARIO, EstadoAntiguedad.HORARIO_ENTREGA_AGENDADO);

        assertThrows(TransicionEstadoInvalidaException.class,
                () -> servicio.registrarIntentoFallido(t.getTrasladosAntiguedadId(), transportistaId, nota, null));
    }

    @Test
    void noPermiteFallarUnTrasladoYaCerrado() {
        TrasladoAntiguedad t = traslado(TipoTraslado.ENTREGA_PROPIETARIO, EstadoAntiguedad.EN_DEVOLUCION_PROPIETARIO);
        t.setResultadoTraslado(ResultadoTraslado.NADIE_EN_CASA);

        assertThrows(TransicionEstadoInvalidaException.class,
                () -> servicio.registrarIntentoFallido(t.getTrasladosAntiguedadId(), transportistaId, nota, null));
    }

    @Test
    void otroTransportistaNoPuedeRegistrarElFallo() {
        TrasladoAntiguedad t = traslado(TipoTraslado.ENTREGA_PROPIETARIO, EstadoAntiguedad.EN_DEVOLUCION_PROPIETARIO);

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.registrarIntentoFallido(t.getTrasladosAntiguedadId(), UUID.randomUUID(), nota, null));
    }

    @Test
    void reagendarTrasIntentoFallidoNumeraElSiguienteIntento() {
        Antiguedad antiguedad = new Antiguedad();
        antiguedad.setAntiguedadesId(UUID.randomUUID());
        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.RESTAURACION_LISTA);
        Usuario transportista = new Usuario();
        transportista.setUsuariosId(transportistaId);
        transportista.setTipoUsuario(TipoUsuario.TRANSPORTISTA);
        transportista.setActivoUsuario(true);
        CrearTrasladoRequest request = new CrearTrasladoRequest(antiguedad.getAntiguedadesId(), transportistaId,
                TipoTraslado.ENTREGA_PROPIETARIO, "Taller", "Casa", LocalDate.now(),
                LocalTime.of(10, 0), LocalTime.of(12, 0), null);

        when(antiguedadRepo.findById(antiguedad.getAntiguedadesId())).thenReturn(Optional.of(antiguedad));
        when(usuarioRepo.findById(transportistaId)).thenReturn(Optional.of(transportista));
        // El intento #1 terminó en NADIE_EN_CASA: no hay uno PENDIENTE ni EXITOSO.
        when(trasladoRepo.existsByAntiguedad_AntiguedadesIdAndTipoTrasladoAndResultadoTrasladoIn(
                eq(antiguedad.getAntiguedadesId()), eq(TipoTraslado.ENTREGA_PROPIETARIO), anyCollection())).thenReturn(false);
        when(trasladoRepo.countByAntiguedad_AntiguedadesIdAndTipoTraslado(
                antiguedad.getAntiguedadesId(), TipoTraslado.ENTREGA_PROPIETARIO)).thenReturn(1L);
        when(trasladoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        servicio.crearTraslado(request);

        ArgumentCaptor<TrasladoAntiguedad> captor = ArgumentCaptor.forClass(TrasladoAntiguedad.class);
        verify(trasladoRepo).save(captor.capture());
        assertEquals((short) 2, captor.getValue().getNumeroIntentoTraslado());
        assertEquals(EstadoAntiguedad.HORARIO_ENTREGA_AGENDADO, antiguedad.getEstadoActualAntiguedad());
    }
}
