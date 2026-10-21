package com.mjrenew.mjrenew_backend.comprador;

import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import com.mjrenew.mjrenew_backend.catalogo.repository.CatalogoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.comprador.service.CompraService;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.*;
import com.mjrenew.mjrenew_backend.nucleo.exception.*;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.transportista.entity.TrasladoAntiguedad;
import com.mjrenew.mjrenew_backend.transportista.repository.TrasladoAntiguedadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompraServiceTest {
    @Mock CatalogoAntiguedadRepository catalogos;
    @Mock TrasladoAntiguedadRepository traslados;
    @Mock FotografiaAntiguedadRepository fotografias;
    @InjectMocks CompraService servicio;

    private final UUID compradorId = UUID.randomUUID();
    private final UUID catalogoId = UUID.randomUUID();

    private CatalogoAntiguedad venta() {
        Usuario comprador = new Usuario();
        comprador.setUsuariosId(compradorId);
        Antiguedad a = new Antiguedad();
        a.setAntiguedadesId(UUID.randomUUID());
        a.setEstadoActualAntiguedad(EstadoAntiguedad.VENTA_COMPLETADA);
        CatalogoAntiguedad c = new CatalogoAntiguedad();
        c.setCatalogoAntiguedadId(catalogoId);
        c.setAntiguedad(a);
        c.setComprador(comprador);
        c.setCompletadaEnCatalogo(OffsetDateTime.now());
        when(catalogos.buscarParaCompra(catalogoId)).thenReturn(Optional.of(c));
        return c;
    }

    private TrasladoAntiguedad entregaExitosa(CatalogoAntiguedad c) {
        TrasladoAntiguedad traslado = new TrasladoAntiguedad();
        traslado.setTrasladosAntiguedadId(UUID.randomUUID());
        traslado.setAntiguedad(c.getAntiguedad());
        traslado.setTipoTraslado(TipoTraslado.ENTREGA_COMPRADOR);
        traslado.setResultadoTraslado(ResultadoTraslado.EXITOSO);
        when(traslados.findFirstByAntiguedad_AntiguedadesIdAndTipoTrasladoAndResultadoTrasladoOrderByEjecutadoEnTrasladoDesc(
                c.getAntiguedad().getAntiguedadesId(), TipoTraslado.ENTREGA_COMPRADOR, ResultadoTraslado.EXITOSO))
                .thenReturn(Optional.of(traslado));
        return traslado;
    }

    @Test
    void soloCompradorAutorizadoPuedeConfirmar() {
        venta();
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.confirmarRecepcion(catalogoId, UUID.randomUUID()));
        verifyNoInteractions(traslados, fotografias);
    }

    @Test
    void necesitaTrasladoExitoso() {
        venta();
        assertThrows(TransicionEstadoInvalidaException.class, () -> servicio.confirmarRecepcion(catalogoId, compradorId));
        verifyNoInteractions(fotografias);
    }

    @Test
    void necesitaCuatroFotosDeEseTraslado() {
        CatalogoAntiguedad c = venta();
        entregaExitosa(c);
        assertThrows(TransicionEstadoInvalidaException.class, () -> servicio.confirmarRecepcion(catalogoId, compradorId));
    }

    @Test
    void acuseEsUnicoYAunqueSeReintenteNoCambiaTimestamp() {
        CatalogoAntiguedad c = venta();
        TrasladoAntiguedad traslado = entregaExitosa(c);
        when(fotografias.countByTraslado_TrasladosAntiguedadIdAndTipoFotografia(
                traslado.getTrasladosAntiguedadId(), TipoFotografia.TRASLADO_LLEGADA)).thenReturn(4L);

        var primero = servicio.confirmarRecepcion(catalogoId, compradorId);
        var segundo = servicio.confirmarRecepcion(catalogoId, compradorId);

        assertEquals(primero.recepcionConfirmadaEn(), segundo.recepcionConfirmadaEn());
        assertEquals(traslado.getTrasladosAntiguedadId(), segundo.trasladoId());
        assertEquals(EstadoAntiguedad.VENTA_COMPLETADA, c.getAntiguedad().getEstadoActualAntiguedad());
    }
}
