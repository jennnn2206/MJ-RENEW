package com.mjrenew.mjrenew_backend.transportista;

import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import com.mjrenew.mjrenew_backend.catalogo.repository.CatalogoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.comprador.dto.ProgramarEntregaCompraRequest;
import com.mjrenew.mjrenew_backend.nucleo.almacenamiento.AlmacenamientoService;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository.AntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.ResultadoTraslado;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTraslado;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoUsuario;
import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.nucleo.usuario.repository.UsuarioRepository;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.transportista.dto.TrasladoAntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.transportista.entity.TrasladoAntiguedad;
import com.mjrenew.mjrenew_backend.transportista.mapper.TrasladoMapper;
import com.mjrenew.mjrenew_backend.transportista.repository.TrasladoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.transportista.service.TrasladoAntiguedadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgramacionEntregaCompradorTest {
    @Mock TrasladoAntiguedadRepository traslados;
    @Mock AntiguedadRepository antiguedades;
    @Mock CatalogoAntiguedadRepository catalogos;
    @Mock FotografiaAntiguedadRepository fotografias;
    @Mock UsuarioRepository usuarios;
    @Mock TrasladoMapper mapper;
    @Mock AlmacenamientoService almacenamiento;
    @InjectMocks TrasladoAntiguedadService servicio;

    private final UUID catalogoId = UUID.randomUUID();
    private final UUID compradorId = UUID.randomUUID();
    private final UUID transportistaId = UUID.randomUUID();

    private CatalogoAntiguedad ventaPagada() {
        Usuario comprador = new Usuario();
        comprador.setUsuariosId(compradorId);
        comprador.setDireccionTextoUsuario("Calle del Comprador 123, Mérida");

        Usuario propietario = new Usuario();
        propietario.setUsuariosId(UUID.randomUUID());
        propietario.setDireccionTextoUsuario("Calle del Propietario 456, Mérida");

        Antiguedad antiguedad = new Antiguedad();
        antiguedad.setAntiguedadesId(UUID.randomUUID());
        antiguedad.setPropietario(propietario);
        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.VENTA_COMPLETADA);

        CatalogoAntiguedad catalogo = new CatalogoAntiguedad();
        catalogo.setCatalogoAntiguedadId(catalogoId);
        catalogo.setAntiguedad(antiguedad);
        catalogo.setComprador(comprador);
        catalogo.setCompletadaEnCatalogo(OffsetDateTime.now());
        catalogo.setActivaCatalogo(false);

        lenient().when(catalogos.buscarParaCompra(catalogoId)).thenReturn(Optional.of(catalogo));
        lenient().when(catalogos.findByAntiguedad_AntiguedadesId(antiguedad.getAntiguedadesId()))
                .thenReturn(Optional.of(catalogo));
        lenient().when(mapper.toDetalle(any(TrasladoAntiguedad.class)))
                .thenAnswer(inv -> detalle(inv.getArgument(0)));
        return catalogo;
    }

    private TrasladoAntiguedadDetalleResponse detalle(TrasladoAntiguedad t) {
        return new TrasladoAntiguedadDetalleResponse(
                t.getTrasladosAntiguedadId(), t.getAntiguedad().getAntiguedadesId(), t.getTipoTraslado(),
                t.getDireccionOrigenTraslado(), t.getDireccionDestinoTraslado(),
                t.getFechaAcordadaTraslado(), t.getHoraInicioAcordadaTraslado(), t.getHoraFinAcordadaTraslado(),
                t.getNumeroIntentoTraslado(), t.getResultadoTraslado(), t.getCostoEstimadoMxnTraslado(),
                t.getSalidaConfirmadaEnTraslado(), t.getEjecutadoEnTraslado(), t.getMotivoFalloTraslado());
    }

    private ProgramarEntregaCompraRequest ventanaFutura() {
        return new ProgramarEntregaCompraRequest(
                LocalDate.now(ZoneId.of("America/Merida")).plusDays(1),
                LocalTime.of(10, 0), LocalTime.of(12, 0));
    }

    @Test
    void registraVentanaSinAsignarTransportistaNiAlterarEstadoDeVenta() {
        CatalogoAntiguedad catalogo = ventaPagada();
        when(traslados.save(any(TrasladoAntiguedad.class))).thenAnswer(inv -> {
            TrasladoAntiguedad traslado = inv.getArgument(0);
            if (traslado.getTrasladosAntiguedadId() == null) {
                traslado.setTrasladosAntiguedadId(UUID.randomUUID());
            }
            return traslado;
        });

        ProgramarEntregaCompraRequest ventana = ventanaFutura();
        TrasladoAntiguedadDetalleResponse respuesta = servicio.solicitarEntregaComprador(
                catalogoId, compradorId, ventana.fechaAcordadaTraslado(),
                ventana.horaInicioAcordadaTraslado(), ventana.horaFinAcordadaTraslado());

        assertNotNull(respuesta.trasladoId());
        verify(traslados).save(argThat(t -> t.getTipoTraslado() == TipoTraslado.ENTREGA_COMPRADOR
                && t.getResultadoTraslado() == ResultadoTraslado.PENDIENTE
                && t.getTransportista() == null
                && t.getNumeroIntentoTraslado() == 1
                && t.getDireccionOrigenTraslado().contains("Propietario")
                && t.getDireccionDestinoTraslado().contains("Comprador")));
        assertEquals(EstadoAntiguedad.VENTA_COMPLETADA,
                catalogo.getAntiguedad().getEstadoActualAntiguedad());
    }

    @Test
    void noPermiteProgramarCompraDeOtroComprador() {
        ventaPagada();
        ProgramarEntregaCompraRequest ventana = ventanaFutura();
        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.solicitarEntregaComprador(catalogoId, UUID.randomUUID(), ventana.fechaAcordadaTraslado(),
                        ventana.horaInicioAcordadaTraslado(), ventana.horaFinAcordadaTraslado()));
        verify(traslados, never()).save(any());
    }

    @Test
    void noPermiteProgramarConFechaPasada() {
        ventaPagada();
        ProgramarEntregaCompraRequest ventanaPasada = new ProgramarEntregaCompraRequest(
                LocalDate.now(ZoneId.of("America/Merida")).minusDays(1),
                LocalTime.of(10, 0), LocalTime.of(12, 0));

        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.solicitarEntregaComprador(catalogoId, compradorId, ventanaPasada.fechaAcordadaTraslado(),
                        ventanaPasada.horaInicioAcordadaTraslado(), ventanaPasada.horaFinAcordadaTraslado()));
        verify(traslados, never()).save(any());
    }

    @Test
    void noPermiteDuplicarUnaSolicitudPendiente() {
        CatalogoAntiguedad catalogo = ventaPagada();
        ProgramarEntregaCompraRequest ventana = ventanaFutura();
        when(traslados.existsByAntiguedad_AntiguedadesIdAndTipoTrasladoAndResultadoTrasladoIn(
                catalogo.getAntiguedad().getAntiguedadesId(), TipoTraslado.ENTREGA_COMPRADOR,
                java.util.List.of(ResultadoTraslado.PENDIENTE, ResultadoTraslado.EXITOSO))).thenReturn(true);

        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.solicitarEntregaComprador(catalogoId, compradorId, ventana.fechaAcordadaTraslado(),
                        ventana.horaInicioAcordadaTraslado(), ventana.horaFinAcordadaTraslado()));
        verify(traslados, never()).save(any());
    }

    @Test
    void noPermiteProgramarSinDomicilioDeEntrega() {
        CatalogoAntiguedad catalogo = ventaPagada();
        catalogo.getComprador().setDireccionTextoUsuario(" ");
        ProgramarEntregaCompraRequest ventana = ventanaFutura();
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.solicitarEntregaComprador(catalogoId, compradorId, ventana.fechaAcordadaTraslado(),
                        ventana.horaInicioAcordadaTraslado(), ventana.horaFinAcordadaTraslado()));
        verify(traslados, never()).save(any());
    }

    @Test
    void asignaSoloTransportistaActivo() {
        CatalogoAntiguedad catalogo = ventaPagada();
        TrasladoAntiguedad traslado = new TrasladoAntiguedad();
        traslado.setTrasladosAntiguedadId(UUID.randomUUID());
        traslado.setAntiguedad(catalogo.getAntiguedad());
        traslado.setTipoTraslado(TipoTraslado.ENTREGA_COMPRADOR);
        traslado.setResultadoTraslado(ResultadoTraslado.PENDIENTE);
        traslado.setNumeroIntentoTraslado((short) 1);
        when(traslados.buscarParaAsignacion(traslado.getTrasladosAntiguedadId())).thenReturn(Optional.of(traslado));

        Usuario transportista = new Usuario();
        transportista.setUsuariosId(transportistaId);
        transportista.setTipoUsuario(TipoUsuario.TRANSPORTISTA);
        transportista.setActivoUsuario(true);
        when(usuarios.findById(transportistaId)).thenReturn(Optional.of(transportista));
        when(traslados.save(traslado)).thenReturn(traslado);

        servicio.asignarTransportistaEntregaComprador(traslado.getTrasladosAntiguedadId(), transportistaId);

        assertEquals(transportistaId, traslado.getTransportista().getUsuariosId());
        verify(traslados).save(traslado);
    }


    @Test
    void noPermiteProgramarSinDomicilioDelPropietario() {
        CatalogoAntiguedad catalogo = ventaPagada();
        catalogo.getAntiguedad().getPropietario().setDireccionTextoUsuario(" ");
        ProgramarEntregaCompraRequest ventana = ventanaFutura();

        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.solicitarEntregaComprador(catalogoId, compradorId,
                        ventana.fechaAcordadaTraslado(), ventana.horaInicioAcordadaTraslado(),
                        ventana.horaFinAcordadaTraslado()));
        verify(traslados, never()).save(any());
    }

    @Test
    void noPermiteProgramarSiLaRecepcionYaFueConfirmada() {
        CatalogoAntiguedad catalogo = ventaPagada();
        catalogo.setRecepcionConfirmadaEnCatalogo(OffsetDateTime.now());
        ProgramarEntregaCompraRequest ventana = ventanaFutura();

        assertThrows(com.mjrenew.mjrenew_backend.nucleo.exception.TransicionEstadoInvalidaException.class,
                () -> servicio.solicitarEntregaComprador(catalogoId, compradorId,
                        ventana.fechaAcordadaTraslado(), ventana.horaInicioAcordadaTraslado(),
                        ventana.horaFinAcordadaTraslado()));
        verify(traslados, never()).save(any());
    }

    @Test
    void noPermiteReasignarUnaSolicitudQueYaTieneTransportista() {
        CatalogoAntiguedad catalogo = ventaPagada();
        TrasladoAntiguedad traslado = new TrasladoAntiguedad();
        traslado.setTrasladosAntiguedadId(UUID.randomUUID());
        traslado.setAntiguedad(catalogo.getAntiguedad());
        traslado.setTipoTraslado(TipoTraslado.ENTREGA_COMPRADOR);
        traslado.setResultadoTraslado(ResultadoTraslado.PENDIENTE);
        Usuario asignado = new Usuario();
        asignado.setUsuariosId(UUID.randomUUID());
        traslado.setTransportista(asignado);
        when(traslados.buscarParaAsignacion(traslado.getTrasladosAntiguedadId())).thenReturn(Optional.of(traslado));

        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.asignarTransportistaEntregaComprador(
                        traslado.getTrasladosAntiguedadId(), transportistaId));
        verify(usuarios, never()).findById(any());
        verify(traslados, never()).save(any());
    }

    @Test
    void rechazaTransportistaInactivo() {
        CatalogoAntiguedad catalogo = ventaPagada();
        TrasladoAntiguedad traslado = new TrasladoAntiguedad();
        traslado.setTrasladosAntiguedadId(UUID.randomUUID());
        traslado.setAntiguedad(catalogo.getAntiguedad());
        traslado.setTipoTraslado(TipoTraslado.ENTREGA_COMPRADOR);
        traslado.setResultadoTraslado(ResultadoTraslado.PENDIENTE);
        when(traslados.buscarParaAsignacion(traslado.getTrasladosAntiguedadId())).thenReturn(Optional.of(traslado));

        Usuario transportista = new Usuario();
        transportista.setUsuariosId(transportistaId);
        transportista.setTipoUsuario(TipoUsuario.TRANSPORTISTA);
        transportista.setActivoUsuario(false);
        when(usuarios.findById(transportistaId)).thenReturn(Optional.of(transportista));

        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.asignarTransportistaEntregaComprador(traslado.getTrasladosAntiguedadId(), transportistaId));
        verify(traslados, never()).save(any());
    }
}
