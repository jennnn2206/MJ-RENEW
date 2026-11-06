package com.mjrenew.mjrenew_backend.transaccion;

import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.transaccion.dto.CargoRealStripe;
import com.mjrenew.mjrenew_backend.transaccion.service.StripeCargoRealService;
import com.stripe.model.BalanceTransaction;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

class StripeCargoRealServiceTest {
    private final StripeCargoRealService servicio = new StripeCargoRealService("sk_test_fake");

    @Test
    void obtieneFeeDeBalanceTransactionDelCargoConfirmado() throws Exception {
        Session sesion = sesion();
        PaymentIntent intento = intento();
        Charge cargo = cargo();
        BalanceTransaction saldo = saldo();
        try (MockedStatic<Session> sesiones = mockStatic(Session.class);
             MockedStatic<PaymentIntent> intentos = mockStatic(PaymentIntent.class);
             MockedStatic<Charge> cargos = mockStatic(Charge.class);
             MockedStatic<BalanceTransaction> saldos = mockStatic(BalanceTransaction.class)) {
            sesiones.when(() -> Session.retrieve(eq("cs_test_venta"), any(RequestOptions.class))).thenReturn(sesion);
            intentos.when(() -> PaymentIntent.retrieve(eq("pi_venta"), any(RequestOptions.class))).thenReturn(intento);
            cargos.when(() -> Charge.retrieve(eq("ch_venta"), any(RequestOptions.class))).thenReturn(cargo);
            saldos.when(() -> BalanceTransaction.retrieve(eq("txn_venta"), any(RequestOptions.class))).thenReturn(saldo);

            CargoRealStripe resultado = servicio.consultar("cs_test_venta");
            assertEquals(23750L, resultado.cargoCentavos());
            assertEquals(650000L, resultado.montoCentavos());
            assertEquals("txn_venta", resultado.balanceTransactionId());
            assertEquals("pi_venta", resultado.paymentIntentId());
        }
    }

    @Test
    void esperaSiStripeAunNoPublicaLaTransaccionDeSaldo() throws Exception {
        Session sesion = sesion();
        PaymentIntent intento = intento();
        Charge cargo = cargo();
        cargo.setBalanceTransaction(null);
        try (MockedStatic<Session> sesiones = mockStatic(Session.class);
             MockedStatic<PaymentIntent> intentos = mockStatic(PaymentIntent.class);
             MockedStatic<Charge> cargos = mockStatic(Charge.class)) {
            sesiones.when(() -> Session.retrieve(eq("cs_test_venta"), any(RequestOptions.class))).thenReturn(sesion);
            intentos.when(() -> PaymentIntent.retrieve(eq("pi_venta"), any(RequestOptions.class))).thenReturn(intento);
            cargos.when(() -> Charge.retrieve(eq("ch_venta"), any(RequestOptions.class))).thenReturn(cargo);

            assertNull(servicio.consultar("cs_test_venta"));
        }
    }

    @Test
    void rechazaTransaccionDeSaldoDeOtroCargo() throws Exception {
        Session sesion = sesion();
        PaymentIntent intento = intento();
        Charge cargo = cargo();
        BalanceTransaction saldo = saldo();
        saldo.setSource("ch_otra");
        try (MockedStatic<Session> sesiones = mockStatic(Session.class);
             MockedStatic<PaymentIntent> intentos = mockStatic(PaymentIntent.class);
             MockedStatic<Charge> cargos = mockStatic(Charge.class);
             MockedStatic<BalanceTransaction> saldos = mockStatic(BalanceTransaction.class)) {
            sesiones.when(() -> Session.retrieve(eq("cs_test_venta"), any(RequestOptions.class))).thenReturn(sesion);
            intentos.when(() -> PaymentIntent.retrieve(eq("pi_venta"), any(RequestOptions.class))).thenReturn(intento);
            cargos.when(() -> Charge.retrieve(eq("ch_venta"), any(RequestOptions.class))).thenReturn(cargo);
            saldos.when(() -> BalanceTransaction.retrieve(eq("txn_venta"), any(RequestOptions.class))).thenReturn(saldo);

            assertThrows(SolicitudInvalidaException.class, () -> servicio.consultar("cs_test_venta"));
        }
    }

    @Test
    void rechazaEstadoDeReembolsoIndeterminado() throws Exception {
        Session sesion = sesion();
        PaymentIntent intento = intento();
        Charge cargo = cargo();
        cargo.setRefunded(null);
        try (MockedStatic<Session> sesiones = mockStatic(Session.class);
             MockedStatic<PaymentIntent> intentos = mockStatic(PaymentIntent.class);
             MockedStatic<Charge> cargos = mockStatic(Charge.class)) {
            sesiones.when(() -> Session.retrieve(eq("cs_test_venta"), any(RequestOptions.class))).thenReturn(sesion);
            intentos.when(() -> PaymentIntent.retrieve(eq("pi_venta"), any(RequestOptions.class))).thenReturn(intento);
            cargos.when(() -> Charge.retrieve(eq("ch_venta"), any(RequestOptions.class))).thenReturn(cargo);

            assertThrows(SolicitudInvalidaException.class, () -> servicio.consultar("cs_test_venta"));
        }
    }

    private Session sesion() {
        Session sesion = new Session();
        sesion.setId("cs_test_venta");
        sesion.setStatus("complete");
        sesion.setPaymentStatus("paid");
        sesion.setMode("payment");
        sesion.setCurrency("mxn");
        sesion.setAmountTotal(650000L);
        sesion.setLivemode(false);
        sesion.setPaymentIntent("pi_venta");
        sesion.setMetadata(Map.of("tipoTransaccion", "PAGO_VENTA", "transaccionId", "venta-1"));
        return sesion;
    }

    private PaymentIntent intento() {
        PaymentIntent intento = new PaymentIntent();
        intento.setId("pi_venta");
        intento.setStatus("succeeded");
        intento.setLatestCharge("ch_venta");
        intento.setAmountReceived(650000L);
        intento.setCurrency("mxn");
        intento.setLivemode(false);
        return intento;
    }

    private Charge cargo() {
        Charge cargo = new Charge();
        cargo.setId("ch_venta");
        cargo.setPaymentIntent("pi_venta");
        cargo.setPaid(true);
        cargo.setCaptured(true);
        cargo.setStatus("succeeded");
        cargo.setDisputed(false);
        cargo.setRefunded(false);
        cargo.setAmountRefunded(0L);
        cargo.setAmount(650000L);
        cargo.setAmountCaptured(650000L);
        cargo.setCurrency("mxn");
        cargo.setLivemode(false);
        cargo.setBalanceTransaction("txn_venta");
        return cargo;
    }

    private BalanceTransaction saldo() {
        BalanceTransaction saldo = new BalanceTransaction();
        saldo.setId("txn_venta");
        saldo.setSource("ch_venta");
        saldo.setAmount(650000L);
        saldo.setFee(23750L);
        saldo.setNet(626250L);
        saldo.setCurrency("mxn");
        return saldo;
    }
}
