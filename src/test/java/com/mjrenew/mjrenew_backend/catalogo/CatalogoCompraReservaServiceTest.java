package com.mjrenew.mjrenew_backend.catalogo;

import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import com.mjrenew.mjrenew_backend.catalogo.repository.CatalogoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.catalogo.service.CatalogoCompraReservaService;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.transaccion.dto.StripeCheckoutSession;
import com.mjrenew.mjrenew_backend.transaccion.entity.TransaccionBancaria;
import com.mjrenew.mjrenew_backend.transaccion.repository.TransaccionBancariaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogoCompraReservaServiceTest {
    @Mock CatalogoAntiguedadRepository catalogos;
    @Mock TransaccionBancariaRepository transacciones;
    @InjectMocks CatalogoCompraReservaService servicio;

    UUID catalogoId;
    Usuario comprador;
    CatalogoAntiguedad catalogo;
    Antiguedad antiguedad;

    @BeforeEach
    void preparar() {
        catalogoId = UUID.randomUUID();
        comprador = new Usuario();
        comprador.setUsuariosId(UUID.randomUUID());
        Usuario propietario = new Usuario();
        propietario.setUsuariosId(UUID.randomUUID());
        antiguedad = new Antiguedad();
        antiguedad.setAntiguedadesId(UUID.randomUUID());
        antiguedad.setPropietario(propietario);
        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.EN_CATALOGO);
        catalogo = new CatalogoAntiguedad();
        catalogo.setCatalogoAntiguedadId(catalogoId);
        catalogo.setAntiguedad(antiguedad);
        catalogo.setActivaCatalogo(true);
        catalogo.setPrecioMxnCatalogo(new BigDecimal("1250.00"));
    }

    @Test
    void reservaSeGuardaAntesDeSolicitarCheckout() {
        when(catalogos.buscarParaCompra(catalogoId)).thenReturn(Optional.of(catalogo));
        when(transacciones.save(any())).thenAnswer(invocacion -> {
            TransaccionBancaria pago = invocacion.getArgument(0);
            pago.setTransaccionId(UUID.randomUUID());
            return pago;
        });

        var reserva = servicio.reservar(catalogoId, comprador);

        assertNotNull(reserva.pago().getTransaccionId());
        assertNull(reserva.referenciaStripe());
        assertEquals(TipoTransaccion.PAGO_VENTA, reserva.pago().getTipoTransaccion());
        assertEquals(EstadoTransaccion.PENDIENTE, reserva.pago().getEstadoTransaccion());
        assertEquals(new BigDecimal("1250.00"), reserva.pago().getMontoBrutoMxnTransaccion());
        assertFalse(catalogo.getActivaCatalogo());
        assertSame(comprador, catalogo.getComprador());
        assertEquals(EstadoAntiguedad.LINK_PAGO_ENVIADO, antiguedad.getEstadoActualAntiguedad());
        verify(catalogos).save(catalogo);
    }

    @Test
    void otroCompradorNoPuedeTomarUnaReservaMientrasSeCreaElCheckout() {
        when(catalogos.buscarParaCompra(catalogoId)).thenReturn(Optional.of(catalogo));
        when(transacciones.save(any())).thenAnswer(invocacion -> {
            TransaccionBancaria pago = invocacion.getArgument(0);
            pago.setTransaccionId(UUID.randomUUID());
            return pago;
        });
        Usuario segundoComprador = new Usuario();
        segundoComprador.setUsuariosId(UUID.randomUUID());

        servicio.reservar(catalogoId, comprador);

        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.reservar(catalogoId, segundoComprador));

        verify(transacciones, times(1)).save(any());
    }

    @Test
    void mismoCompradorReutilizaElPagoPendienteSinReferencia() {
        catalogo.setActivaCatalogo(false);
        catalogo.setComprador(comprador);
        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.LINK_PAGO_ENVIADO);
        TransaccionBancaria pago = new TransaccionBancaria();
        pago.setOriginator(comprador);
        when(catalogos.buscarParaCompra(catalogoId)).thenReturn(Optional.of(catalogo));
        when(transacciones.findFirstByCatalogoAntiguedad_CatalogoAntiguedadIdAndTipoTransaccionAndEstadoTransaccionOrderByCreadaEnTransaccionDesc(
                catalogoId, TipoTransaccion.PAGO_VENTA, EstadoTransaccion.PENDIENTE))
                .thenReturn(Optional.of(pago));

        var reserva = servicio.reservar(catalogoId, comprador);

        assertSame(pago, reserva.pago());
        assertNull(reserva.referenciaStripe());
        verify(transacciones, never()).save(any());
    }

    @Test
    void vinculoDeCheckoutEsIdempotente() {
        catalogo.setActivaCatalogo(false);
        catalogo.setComprador(comprador);
        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.LINK_PAGO_ENVIADO);
        TransaccionBancaria pago = new TransaccionBancaria();
        UUID pagoId = UUID.randomUUID();
        pago.setTransaccionId(pagoId);
        pago.setOriginator(comprador);
        pago.setCatalogoAntiguedad(catalogo);
        pago.setEstadoTransaccion(EstadoTransaccion.PENDIENTE);
        StripeCheckoutSession checkout = new StripeCheckoutSession("cs_test_123", "https://checkout.stripe.test/123");
        when(transacciones.bloquearPorId(pagoId)).thenReturn(Optional.of(pago));
        when(catalogos.buscarParaCompra(catalogoId)).thenReturn(Optional.of(catalogo));

        servicio.vincularCheckout(pagoId, catalogoId, comprador.getUsuariosId(), checkout);
        servicio.vincularCheckout(pagoId, catalogoId, comprador.getUsuariosId(), checkout);

        assertEquals("cs_test_123", pago.getReferenciaStripeTransaccion());
        assertEquals("cs_test_123", catalogo.getIdLinkStripeCatalogo());
        assertEquals(checkout.urlPagoStripe(), catalogo.getUrlLinkPagoCatalogo());
        verify(transacciones, times(1)).save(pago);
        verify(catalogos, times(1)).save(catalogo);
    }
}
