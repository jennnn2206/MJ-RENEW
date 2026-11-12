package com.mjrenew.mjrenew_backend.catalogo;

import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import com.mjrenew.mjrenew_backend.catalogo.mapper.CatalogoAntiguedadMapper;
import com.mjrenew.mjrenew_backend.catalogo.repository.CatalogoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.catalogo.service.CatalogoAntiguedadService;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.propietario.repository.DimensionRepository;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.transaccion.dto.StripeCheckoutSession;
import com.mjrenew.mjrenew_backend.transaccion.entity.TransaccionBancaria;
import com.mjrenew.mjrenew_backend.transaccion.repository.TransaccionBancariaRepository;
import com.mjrenew.mjrenew_backend.transaccion.service.StripeCheckoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    @Mock TransaccionBancariaRepository transacciones;
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

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtenerDetalle(catalogoId));

        verifyNoInteractions(fotografias, dimensiones, mapper);
    }

    @Test
    void compraReservaLaAntiguedadYRegistraPagoPendiente() {
        when(catalogos.buscarParaCompra(catalogoId)).thenReturn(Optional.of(catalogo));
        when(stripe.crearSesionPago(any())).thenReturn(
                new StripeCheckoutSession("cs_test_123", "https://checkout.stripe.test/123"));

        var respuesta = servicio.iniciarCompra(catalogoId, comprador);

        assertEquals("https://checkout.stripe.test/123", respuesta.urlPagoStripe());
        assertFalse(catalogo.getActivaCatalogo());
        assertSame(comprador, catalogo.getComprador());
        assertEquals("cs_test_123", catalogo.getIdLinkStripeCatalogo());
        assertEquals(EstadoAntiguedad.LINK_PAGO_ENVIADO, antiguedad.getEstadoActualAntiguedad());
        ArgumentCaptor<TransaccionBancaria> pago = ArgumentCaptor.forClass(TransaccionBancaria.class);
        verify(stripe).crearSesionPago(pago.capture());
        assertEquals(TipoTransaccion.PAGO_VENTA, pago.getValue().getTipoTransaccion());
        assertEquals(EstadoTransaccion.PENDIENTE, pago.getValue().getEstadoTransaccion());
        assertEquals(new BigDecimal("1250.00"), pago.getValue().getMontoBrutoMxnTransaccion());
        assertEquals("cs_test_123", pago.getValue().getReferenciaStripeTransaccion());
        verify(transacciones, times(2)).save(pago.getValue());
    }

    @Test
    void reservaExistenteNoGeneraOtroCheckout() {
        catalogo.setActivaCatalogo(false);
        when(catalogos.buscarParaCompra(catalogoId)).thenReturn(Optional.of(catalogo));

        assertThrows(SolicitudInvalidaException.class, () -> servicio.iniciarCompra(catalogoId, comprador));

        verifyNoInteractions(stripe, transacciones);
    }
}
