package com.mjrenew.mjrenew_backend.transaccion;

import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import com.mjrenew.mjrenew_backend.catalogo.repository.CatalogoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository.AntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.enums.*;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.transaccion.dto.StripeCheckoutEstado;
import com.mjrenew.mjrenew_backend.transaccion.entity.TransaccionBancaria;
import com.mjrenew.mjrenew_backend.transaccion.repository.TransaccionBancariaRepository;
import com.mjrenew.mjrenew_backend.transaccion.service.StripePagoEstadoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripePagoEstadoServiceTest {
    @Mock TransaccionBancariaRepository transacciones;
    @Mock AntiguedadRepository antiguedades;
    @Mock CatalogoAntiguedadRepository catalogos;
    StripePagoEstadoService servicio;
    TransaccionBancaria transaccion;
    Antiguedad antiguedad;
    String sesionId;

    @BeforeEach
    void preparar() {
        servicio = new StripePagoEstadoService(transacciones, antiguedades, catalogos, "sk_test_fake");
        sesionId = "cs_test_" + UUID.randomUUID();
        antiguedad = new Antiguedad();
        antiguedad.setAntiguedadesId(UUID.randomUUID());
        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.PRESUPUESTO_PRESENTADO);
        transaccion = new TransaccionBancaria();
        transaccion.setTransaccionId(UUID.randomUUID());
        transaccion.setTipoTransaccion(TipoTransaccion.PAGO_RESTAURACION);
        transaccion.setAntiguedad(antiguedad);
        transaccion.setReferenciaStripeTransaccion(sesionId);
        transaccion.setEstadoTransaccion(EstadoTransaccion.PENDIENTE);
        transaccion.setMontoBrutoMxnTransaccion(new BigDecimal("6500.00"));
    }

    StripeCheckoutEstado sesion(String tipo, String transaccionId, long centavos, String moneda, String status, String pago) {
        return new StripeCheckoutEstado(sesionId, tipo, transaccionId, status, pago, "payment", moneda, centavos, false);
    }

    StripeCheckoutEstado correcta() {
        return sesion("PAGO_RESTAURACION", transaccion.getTransaccionId().toString(), 650000L, "mxn", "complete", "paid");
    }

    void simularPersistencia() {
        when(transacciones.findByReferenciaStripeTransaccion(sesionId)).thenReturn(Optional.of(transaccion));
    }

    @Test
    void confirmaRestauracionSinSaltearAFD() {
        simularPersistencia();
        when(antiguedades.bloquearParaConfirmarPago(antiguedad.getAntiguedadesId()))
                .thenReturn(Optional.of(antiguedad));
        servicio.confirmar(correcta());
        assertEquals(EstadoTransaccion.EXITOSA, transaccion.getEstadoTransaccion());
        assertEquals(EstadoAntiguedad.PAGO_EN_ESCROW, antiguedad.getEstadoActualAntiguedad());
        assertNotNull(transaccion.getProcesadaEnTransaccion());
    }

    @Test
    void noConfirmaMontosDiferentes() {
        simularPersistencia();
        StripeCheckoutEstado falso = sesion("PAGO_RESTAURACION", transaccion.getTransaccionId().toString(), 100L, "mxn", "complete", "paid");
        assertThrows(SolicitudInvalidaException.class, () -> servicio.confirmar(falso));
        assertEquals(EstadoTransaccion.PENDIENTE, transaccion.getEstadoTransaccion());
        verifyNoInteractions(antiguedades);
    }

    @Test
    void noConfirmaUUIDDeOtraTransaccion() {
        simularPersistencia();
        StripeCheckoutEstado falso = sesion("PAGO_RESTAURACION", UUID.randomUUID().toString(), 650000L, "mxn", "complete", "paid");
        assertThrows(SolicitudInvalidaException.class, () -> servicio.confirmar(falso));
        verifyNoInteractions(antiguedades);
    }

    @Test
    void noConfirmaCheckoutTodaviaPendiente() {
        simularPersistencia();
        servicio.confirmar(sesion("PAGO_RESTAURACION", transaccion.getTransaccionId().toString(), 650000L, "mxn", "open", "unpaid"));
        assertEquals(EstadoTransaccion.PENDIENTE, transaccion.getEstadoTransaccion());
        verifyNoInteractions(antiguedades);
    }

    @Test
    void duplicadoNoRepiteTransicion() {
        simularPersistencia();
        when(antiguedades.bloquearParaConfirmarPago(antiguedad.getAntiguedadesId()))
                .thenReturn(Optional.of(antiguedad));
        transaccion.setEstadoTransaccion(EstadoTransaccion.EXITOSA);
        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.PAGO_EN_ESCROW);
        servicio.confirmar(correcta());
        assertEquals(EstadoAntiguedad.PAGO_EN_ESCROW, antiguedad.getEstadoActualAntiguedad());
    }

    @Test
    void pagoDeVentaExpiradoLiberaReserva() {
        transaccion.setTipoTransaccion(TipoTransaccion.PAGO_VENTA);
        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.LINK_PAGO_ENVIADO);
        CatalogoAntiguedad catalogo = new CatalogoAntiguedad();
        UUID catalogoId = UUID.randomUUID();
        catalogo.setCatalogoAntiguedadId(catalogoId);
        catalogo.setIdLinkStripeCatalogo(sesionId);
        catalogo.setActivaCatalogo(false);
        transaccion.setCatalogoAntiguedad(catalogo);
        simularPersistencia();
        when(antiguedades.bloquearParaConfirmarPago(antiguedad.getAntiguedadesId()))
                .thenReturn(Optional.of(antiguedad));
        when(catalogos.buscarParaCompra(catalogoId)).thenReturn(Optional.of(catalogo));
        servicio.marcarNoCompletado(sesion("PAGO_VENTA", transaccion.getTransaccionId().toString(),
                650000L, "mxn", "expired", "unpaid"));
        assertEquals(EstadoTransaccion.FALLIDA, transaccion.getEstadoTransaccion());
        assertEquals(EstadoAntiguedad.EN_CATALOGO, antiguedad.getEstadoActualAntiguedad());
        assertTrue(catalogo.getActivaCatalogo());
    }

    @Test
    void eventoVencidoNoCancelaPagoYaConfirmado() {
        simularPersistencia();
        transaccion.setEstadoTransaccion(EstadoTransaccion.EXITOSA);
        servicio.marcarNoCompletado(sesion("PAGO_RESTAURACION", transaccion.getTransaccionId().toString(),
                650000L, "mxn", "expired", "unpaid"));
        assertEquals(EstadoTransaccion.EXITOSA, transaccion.getEstadoTransaccion());
        verifyNoInteractions(antiguedades);
    }
}
