package com.mjrenew.mjrenew_backend.catalogo;

import com.mjrenew.mjrenew_backend.catalogo.service.CatalogoAntiguedadService;
import com.mjrenew.mjrenew_backend.catalogo.service.CatalogoCompraReservaService;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.transaccion.dto.StripeCheckoutSession;
import com.mjrenew.mjrenew_backend.transaccion.entity.TransaccionBancaria;
import com.mjrenew.mjrenew_backend.transaccion.service.StripeCheckoutService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class CheckoutSinTransaccionJdbcTest {
    @Autowired CatalogoAntiguedadService servicio;
    @MockitoBean CatalogoCompraReservaService reservas;
    @MockitoBean StripeCheckoutService stripe;

    @Test
    @Transactional
    void llamadaAStripeSuspendeInclusoUnaTransaccionDelInvocador() {
        UUID catalogoId = UUID.randomUUID();
        Usuario comprador = new Usuario();
        comprador.setUsuariosId(UUID.randomUUID());
        TransaccionBancaria pago = new TransaccionBancaria();
        pago.setTransaccionId(UUID.randomUUID());
        StripeCheckoutSession checkout =
                new StripeCheckoutSession("cs_test_123", "https://checkout.stripe.test/123");
        when(reservas.reservar(catalogoId, comprador))
                .thenReturn(new CatalogoCompraReservaService.ReservaCompra(pago, null));
        when(stripe.crearSesionPago(pago)).thenAnswer(invocacion -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            return checkout;
        });

        assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
        servicio.iniciarCompra(catalogoId, comprador);
        assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
        verify(reservas).vincularCheckout(pago.getTransaccionId(), catalogoId,
                comprador.getUsuariosId(), checkout);
    }
}
