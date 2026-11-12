package com.mjrenew.mjrenew_backend.catalogo;

import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import com.mjrenew.mjrenew_backend.catalogo.mapper.CatalogoAntiguedadMapper;
import com.mjrenew.mjrenew_backend.catalogo.repository.CatalogoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.catalogo.service.CatalogoAntiguedadService;
import com.mjrenew.mjrenew_backend.catalogo.service.CatalogoCompraReservaService;
import com.mjrenew.mjrenew_backend.catalogo.service.CheckoutPendienteException;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.propietario.repository.DimensionRepository;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.transaccion.dto.StripeCheckoutSession;
import com.mjrenew.mjrenew_backend.transaccion.entity.TransaccionBancaria;
import com.mjrenew.mjrenew_backend.transaccion.service.StripeCheckoutService;
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
class CatalogoAntiguedadServiceTest {
    @Mock CatalogoAntiguedadRepository catalogos;
    @Mock FotografiaAntiguedadRepository fotografias;
    @Mock DimensionRepository dimensiones;
    @Mock CatalogoCompraReservaService reservas;
    @Mock CatalogoAntiguedadMapper mapper;
    @Mock StripeCheckoutService stripe;
    @InjectMocks CatalogoAntiguedadService servicio;

    UUID catalogoId;
    CatalogoAntiguedad catalogo;
    Antiguedad antiguedad;
    Usuario comprador;

    @BeforeEach
    void preparar() {
        catalogoId = UUID.randomUUID();
        Usuario propietario = new Usuario();
        propietario.setUsuariosId(UUID.randomUUID());
        comprador = new Usuario();
        comprador.setUsuariosId(UUID.randomUUID());
        antiguedad = new Antiguedad();
        antiguedad.setAntiguedadesId(UUID.randomUUID());
        antiguedad.setPropietario(propietario);
        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.EN_CATALOGO);
        catalogo = new CatalogoAntiguedad();
        catalogo.setCatalogoAntiguedadId(catalogoId);
        catalogo.setAntiguedad(antiguedad);
        catalogo.setPrecioMxnCatalogo(new BigDecimal("1250.00"));
        catalogo.setActivaCatalogo(true);
    }

    @Test
    void fichaPublicaNoMuestraReservaOVentaInactiva() {
        catalogo.setActivaCatalogo(false);
        when(catalogos.findById(catalogoId)).thenReturn(Optional.of(catalogo));

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtenerDetalle(catalogoId, null));

        verifyNoInteractions(fotografias, dimensiones, mapper);
    }

    @Test
    void compradorPuedeRecuperarLaFichaDeSuReservaPendiente() {
        catalogo.setActivaCatalogo(false);
        catalogo.setComprador(comprador);
        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.LINK_PAGO_ENVIADO);
        when(catalogos.findById(catalogoId)).thenReturn(Optional.of(catalogo));

        assertDoesNotThrow(() -> servicio.obtenerDetalle(catalogoId, comprador.getUsuariosId()));

        verify(mapper).toDetalle(eq(catalogo), isNull(), isNull());
    }

    @Test
    void otroCompradorNoVeLaFichaReservada() {
        catalogo.setActivaCatalogo(false);
        catalogo.setComprador(comprador);
        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.LINK_PAGO_ENVIADO);
        when(catalogos.findById(catalogoId)).thenReturn(Optional.of(catalogo));

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.obtenerDetalle(catalogoId, UUID.randomUUID()));

        verifyNoInteractions(fotografias, dimensiones, mapper);
    }

    @Test
    void compraConcluidaNoMuestraFichaNiAlComprador() {
        catalogo.setActivaCatalogo(false);
        catalogo.setComprador(comprador);
        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.VENTA_COMPLETADA);
        when(catalogos.findById(catalogoId)).thenReturn(Optional.of(catalogo));

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.obtenerDetalle(catalogoId, comprador.getUsuariosId()));
    }

    @Test
    void compraVinculaLaSesionDespuesDeReservar() {
        TransaccionBancaria pago = new TransaccionBancaria();
        UUID pagoId = UUID.randomUUID();
        pago.setTransaccionId(pagoId);
        when(reservas.reservar(catalogoId, comprador))
                .thenReturn(new CatalogoCompraReservaService.ReservaCompra(pago, null));
        when(stripe.crearSesionPago(any())).thenReturn(
                new StripeCheckoutSession("cs_test_123", "https://checkout.stripe.test/123"));

        var respuesta = servicio.iniciarCompra(catalogoId, comprador);

        assertEquals("https://checkout.stripe.test/123", respuesta.urlPagoStripe());
        var orden = inOrder(reservas, stripe);
        orden.verify(reservas).reservar(catalogoId, comprador);
        orden.verify(stripe).crearSesionPago(pago);
        orden.verify(reservas).vincularCheckout(pagoId, catalogoId, comprador.getUsuariosId(),
                new StripeCheckoutSession("cs_test_123", "https://checkout.stripe.test/123"));
    }

    @Test
    void reintentoConReferenciaRecuperaCheckoutSinCrearOtro() {
        when(reservas.reservar(catalogoId, comprador))
                .thenReturn(new CatalogoCompraReservaService.ReservaCompra(new TransaccionBancaria(), "cs_test_123"));
        when(stripe.recuperarSesionAbierta("cs_test_123"))
                .thenReturn(new StripeCheckoutSession("cs_test_123", "https://checkout.stripe.test/123"));

        var respuesta = servicio.iniciarCompra(catalogoId, comprador);

        assertEquals("https://checkout.stripe.test/123", respuesta.urlPagoStripe());
        verify(stripe, never()).crearSesionPago(any());
        verify(reservas, never()).vincularCheckout(any(), any(), any(), any());
    }

    @Test
    void errorDeStripeMantieneReservaParaReintento() {
        TransaccionBancaria pago = new TransaccionBancaria();
        pago.setTransaccionId(UUID.randomUUID());
        when(reservas.reservar(catalogoId, comprador))
                .thenReturn(new CatalogoCompraReservaService.ReservaCompra(pago, null));
        when(stripe.crearSesionPago(pago)).thenThrow(new SolicitudInvalidaException("Error Stripe"));

        assertThrows(CheckoutPendienteException.class, () -> servicio.iniciarCompra(catalogoId, comprador));

        verify(reservas, never()).vincularCheckout(any(), any(), any(), any());
    }

    @Test
    void errorAlGuardarReferenciaNoDevuelveUrlDePago() {
        TransaccionBancaria pago = new TransaccionBancaria();
        UUID pagoId = UUID.randomUUID();
        pago.setTransaccionId(pagoId);
        StripeCheckoutSession checkout =
                new StripeCheckoutSession("cs_test_123", "https://checkout.stripe.test/123");
        when(reservas.reservar(catalogoId, comprador))
                .thenReturn(new CatalogoCompraReservaService.ReservaCompra(pago, null));
        when(stripe.crearSesionPago(pago)).thenReturn(checkout);
        doThrow(new SolicitudInvalidaException("Error BD"))
                .when(reservas).vincularCheckout(pagoId, catalogoId, comprador.getUsuariosId(), checkout);

        assertThrows(CheckoutPendienteException.class, () -> servicio.iniciarCompra(catalogoId, comprador));
    }
}
