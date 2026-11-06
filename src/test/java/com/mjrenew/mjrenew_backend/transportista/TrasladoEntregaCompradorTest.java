package com.mjrenew.mjrenew_backend.transportista;

import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import com.mjrenew.mjrenew_backend.catalogo.repository.CatalogoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.almacenamiento.AlmacenamientoService;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository.AntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.enums.*;
import com.mjrenew.mjrenew_backend.nucleo.exception.*;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.nucleo.usuario.repository.UsuarioRepository;
import com.mjrenew.mjrenew_backend.propietario.entity.FotografiaAntiguedad;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.transportista.dto.CrearTrasladoRequest;
import com.mjrenew.mjrenew_backend.transportista.entity.TrasladoAntiguedad;
import com.mjrenew.mjrenew_backend.transportista.mapper.TrasladoMapper;
import com.mjrenew.mjrenew_backend.transportista.repository.TrasladoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.transportista.service.TrasladoAntiguedadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrasladoEntregaCompradorTest {
    @Mock TrasladoAntiguedadRepository traslados;
    @Mock AntiguedadRepository antiguedades;
    @Mock CatalogoAntiguedadRepository catalogos;
    @Mock FotografiaAntiguedadRepository fotografias;
    @Mock UsuarioRepository usuarios;
    @Mock TrasladoMapper mapper;
    @Mock AlmacenamientoService almacenamiento;
    @InjectMocks TrasladoAntiguedadService servicio;

    private final UUID transportistaId = UUID.randomUUID();

    private Antiguedad vendida() {
        Antiguedad a = new Antiguedad();
        a.setAntiguedadesId(UUID.randomUUID());
        a.setEstadoActualAntiguedad(EstadoAntiguedad.VENTA_COMPLETADA);
        CatalogoAntiguedad c = new CatalogoAntiguedad();
        c.setComprador(new Usuario());
        c.setCompletadaEnCatalogo(OffsetDateTime.now());
        c.setActivaCatalogo(false);
        lenient().when(catalogos.findByAntiguedad_AntiguedadesId(a.getAntiguedadesId())).thenReturn(Optional.of(c));
        return a;
    }

    private TrasladoAntiguedad pendiente() {
        Antiguedad a = vendida();
        Usuario transportista = new Usuario();
        transportista.setUsuariosId(transportistaId);
        TrasladoAntiguedad t = new TrasladoAntiguedad();
        t.setTrasladosAntiguedadId(UUID.randomUUID());
        t.setAntiguedad(a);
        t.setTransportista(transportista);
        t.setTipoTraslado(TipoTraslado.ENTREGA_COMPRADOR);
        t.setResultadoTraslado(ResultadoTraslado.PENDIENTE);
        when(traslados.findById(t.getTrasladosAntiguedadId())).thenReturn(Optional.of(t));
        return t;
    }

    @Test
    void crearEntregaSoloDespuesDeVentaConfirmada() {
        Antiguedad a = vendida();
        Usuario transportista = new Usuario();
        transportista.setUsuariosId(transportistaId);
        transportista.setTipoUsuario(TipoUsuario.TRANSPORTISTA);
        transportista.setActivoUsuario(true);
        CrearTrasladoRequest r = new CrearTrasladoRequest(a.getAntiguedadesId(), transportistaId,
                TipoTraslado.ENTREGA_COMPRADOR, "Casa del propietario", "Casa del comprador",
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0), null);
        when(antiguedades.findById(a.getAntiguedadesId())).thenReturn(Optional.of(a));
        when(usuarios.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(traslados.save(any())).thenAnswer(i -> i.getArgument(0));

        servicio.crearTraslado(r);

        assertEquals(EstadoAntiguedad.VENTA_COMPLETADA, a.getEstadoActualAntiguedad());
        verify(traslados).save(argThat(t -> t.getTipoTraslado() == TipoTraslado.ENTREGA_COMPRADOR));
    }


    @Test
    void salidaValidaNoAlteraEstadoTerminalDeVenta() {
        TrasladoAntiguedad t = pendiente();
        when(traslados.save(t)).thenReturn(t);

        servicio.confirmarSalidaEntregaComprador(t.getTrasladosAntiguedadId(), transportistaId);

        assertNotNull(t.getSalidaConfirmadaEnTraslado());
        assertEquals(ResultadoTraslado.PENDIENTE, t.getResultadoTraslado());
        assertEquals(EstadoAntiguedad.VENTA_COMPLETADA, t.getAntiguedad().getEstadoActualAntiguedad());
    }

    @Test
    void noAgendaSinPagoConfirmado() {
        Antiguedad a = new Antiguedad();
        a.setAntiguedadesId(UUID.randomUUID());
        a.setEstadoActualAntiguedad(EstadoAntiguedad.LINK_PAGO_ENVIADO);
        Usuario transportista = new Usuario();
        transportista.setUsuariosId(transportistaId);
        transportista.setTipoUsuario(TipoUsuario.TRANSPORTISTA);
        transportista.setActivoUsuario(true);
        CrearTrasladoRequest request = new CrearTrasladoRequest(a.getAntiguedadesId(), transportistaId,
                TipoTraslado.ENTREGA_COMPRADOR, "Casa del propietario", "Casa del comprador",
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0), null);
        when(antiguedades.findById(a.getAntiguedadesId())).thenReturn(Optional.of(a));
        when(usuarios.findById(transportistaId)).thenReturn(Optional.of(transportista));

        assertThrows(TransicionEstadoInvalidaException.class, () -> servicio.crearTraslado(request));
        verify(traslados, never()).save(any());
    }

    @Test
    void noPermiteConfirmarEntregaSinSalidaPrevia() {
        TrasladoAntiguedad t = pendiente();
        assertThrows(TransicionEstadoInvalidaException.class,
                () -> servicio.confirmarEntregaComprador(t.getTrasladosAntiguedadId(), transportistaId, List.of()));
        verifyNoInteractions(almacenamiento);
    }

    @Test
    void entregaConEvidenciasNoConfirmaRecepcionDelComprador() {
        TrasladoAntiguedad t = pendiente();
        t.setSalidaConfirmadaEnTraslado(OffsetDateTime.now());
        when(traslados.save(t)).thenReturn(t);
        when(almacenamiento.subirArchivo(any(), eq("traslados"))).thenReturn("http://minio/test.jpg");
        var fotos = List.of(
                new MockMultipartFile("fotos", "a.jpg", "image/jpeg", new byte[]{1}),
                new MockMultipartFile("fotos", "b.jpg", "image/jpeg", new byte[]{1}),
                new MockMultipartFile("fotos", "c.jpg", "image/jpeg", new byte[]{1}),
                new MockMultipartFile("fotos", "d.jpg", "image/jpeg", new byte[]{1}));

        servicio.confirmarEntregaComprador(t.getTrasladosAntiguedadId(), transportistaId, fotos.stream().map(f -> (org.springframework.web.multipart.MultipartFile) f).toList());

        assertEquals(ResultadoTraslado.EXITOSO, t.getResultadoTraslado());
        assertEquals(EstadoAntiguedad.VENTA_COMPLETADA, t.getAntiguedad().getEstadoActualAntiguedad());
        verify(fotografias, times(4)).save(argThat(f -> f.getTraslado() == t
                && f.getTipoFotografia() == TipoFotografia.TRASLADO_LLEGADA));
    }

    @Test
    void noAceptaMenosDeCuatroEvidencias() {
        TrasladoAntiguedad t = pendiente();
        t.setSalidaConfirmadaEnTraslado(OffsetDateTime.now());
        assertThrows(SolicitudInvalidaException.class, () -> servicio.confirmarEntregaComprador(
                t.getTrasladosAntiguedadId(), transportistaId, List.of()));
        verify(traslados, never()).save(any());
    }

    @Test
    void otroTransportistaNoPuedeConfirmarEntrega() {
        TrasladoAntiguedad t = pendiente();
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.confirmarSalidaEntregaComprador(
                t.getTrasladosAntiguedadId(), UUID.randomUUID()));
    }

    @Test
    void salidaNoSePuedeRepetir() {
        TrasladoAntiguedad t = pendiente();
        t.setSalidaConfirmadaEnTraslado(OffsetDateTime.now());
        assertThrows(TransicionEstadoInvalidaException.class, () -> servicio.confirmarSalidaEntregaComprador(
                t.getTrasladosAntiguedadId(), transportistaId));
    }
}
