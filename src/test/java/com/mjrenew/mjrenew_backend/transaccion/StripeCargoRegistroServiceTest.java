package com.mjrenew.mjrenew_backend.transaccion;

import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.transaccion.dto.CargoRealStripe;
import com.mjrenew.mjrenew_backend.transaccion.dto.StripeCheckoutEstado;
import com.mjrenew.mjrenew_backend.transaccion.entity.TransaccionBancaria;
import com.mjrenew.mjrenew_backend.transaccion.repository.TransaccionBancariaRepository;
import com.mjrenew.mjrenew_backend.transaccion.service.StripeCargoRegistroService;
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
class StripeCargoRegistroServiceTest {
    @Mock TransaccionBancariaRepository transacciones;
    StripeCargoRegistroService servicio;
    TransaccionBancaria pago;
    TransaccionBancaria liberacion;
    UUID liberacionId;

    @BeforeEach
    void preparar() {
        servicio = new StripeCargoRegistroService(transacciones, "sk_test_fake");
        pago = new TransaccionBancaria();
        pago.setTransaccionId(UUID.randomUUID());
        pago.setTipoTransaccion(TipoTransaccion.PAGO_VENTA);
        pago.setEstadoTransaccion(EstadoTransaccion.EXITOSA);
        pago.setReferenciaStripeTransaccion("cs_test_venta");
        pago.setMontoBrutoMxnTransaccion(new BigDecimal("6500.00"));
        liberacion = new TransaccionBancaria();
        liberacionId = UUID.randomUUID();
        liberacion.setTransaccionId(liberacionId);
        liberacion.setTipoTransaccion(TipoTransaccion.LIBERACION_PROPIETARIO);
        liberacion.setEstadoTransaccion(EstadoTransaccion.PENDIENTE);
        liberacion.setTransaccionPadre(pago);
        liberacion.setMontoBrutoMxnTransaccion(new BigDecimal("6500.00"));
        liberacion.setComisionMjrenewMxn(new BigDecimal("455.00"));
        liberacion.setMontoAntesCargoStripeMxn(new BigDecimal("6045.00"));
        when(transacciones.bloquearPorId(liberacionId)).thenReturn(Optional.of(liberacion));
    }

    private CargoRealStripe cargo(long centavos, long fee, String sesionId) {
        StripeCheckoutEstado sesion = new StripeCheckoutEstado(sesionId, "PAGO_VENTA",
                pago.getTransaccionId().toString(), "complete", "paid", "payment", "mxn", 650000L, false);
        return new CargoRealStripe(sesion, "pi_venta", "ch_venta", "txn_venta",
                centavos, fee, "mxn", false);
    }

    @Test
    void guardaFeeRealYNetoSinMarcarTransferenciaExitosa() {
        servicio.registrar(liberacionId, cargo(650000L, 23750L, "cs_test_venta"));

        assertEquals(new BigDecimal("237.50"), liberacion.getCargoStripeMxn());
        assertEquals(new BigDecimal("5807.50"), liberacion.getMontoNetoPropietarioMxn());
        assertEquals("pi_venta", liberacion.getStripePaymentIntentId());
        assertEquals("ch_venta", liberacion.getStripeChargeId());
        assertEquals("txn_venta", liberacion.getStripeBalanceTransactionId());
        assertEquals(EstadoTransaccion.PENDIENTE, liberacion.getEstadoTransaccion());
        assertNull(liberacion.getProcesadaEnTransaccion());
    }

    @Test
    void rechazaCargoDeOtraSesionOMonto() {
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.registrar(liberacionId, cargo(650000L, 23750L, "cs_test_otra")));
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.registrar(liberacionId, cargo(100L, 1L, "cs_test_venta")));
        assertNull(liberacion.getCargoStripeMxn());
    }

    @Test
    void rechazaNetoNoPositivo() {
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.registrar(liberacionId, cargo(650000L, 604500L, "cs_test_venta")));
        assertNull(liberacion.getMontoNetoPropietarioMxn());
    }

    @Test
    void reintentoIdenticoEsIdempotentePeroCambioDeFeeSeRechaza() {
        CargoRealStripe original = cargo(650000L, 23750L, "cs_test_venta");
        servicio.registrar(liberacionId, original);
        servicio.registrar(liberacionId, original);
        assertEquals(new BigDecimal("5807.50"), liberacion.getMontoNetoPropietarioMxn());
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.registrar(liberacionId, cargo(650000L, 23800L, "cs_test_venta")));
        assertEquals(new BigDecimal("237.50"), liberacion.getCargoStripeMxn());
    }

    @Test
    void reintentoRechazaNetoGuardadoInconsistente() {
        CargoRealStripe original = cargo(650000L, 23750L, "cs_test_venta");
        servicio.registrar(liberacionId, original);
        liberacion.setMontoNetoPropietarioMxn(new BigDecimal("6000.00"));
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.registrar(liberacionId, original));
    }
}
