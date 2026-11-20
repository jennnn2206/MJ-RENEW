package com.mjrenew.mjrenew_backend.transaccion;

import com.mjrenew.mjrenew_backend.administrador.repository.DisputaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.*;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.propietario.entity.CuentaStripeConectada;
import com.mjrenew.mjrenew_backend.propietario.repository.CuentaStripeConectadaRepository;
import com.mjrenew.mjrenew_backend.transaccion.dto.*;
import com.mjrenew.mjrenew_backend.transaccion.entity.TransaccionBancaria;
import com.mjrenew.mjrenew_backend.transaccion.repository.TransaccionBancariaRepository;
import com.mjrenew.mjrenew_backend.transaccion.service.*;
import com.stripe.model.Transfer;
import com.stripe.model.TransferCollection;
import com.stripe.param.TransferCreateParams;
import com.stripe.param.TransferListParams;
import com.stripe.net.RequestOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransferenciasConnectServiceTest {
    private final TransaccionBancariaRepository pagos = mock();
    private final DisputaAntiguedadRepository disputas = mock();
    private final CuentaStripeConectadaRepository cuentas = mock();
    private final StripeConnectService connect = mock();
    private final StripeCargoRealService cargos = mock();
    private final UUID id = UUID.randomUUID();
    private final TransaccionBancaria liberacion = new TransaccionBancaria();
    private final TransferenciasConnectService servicio = new TransferenciasConnectService(pagos, disputas,
            cuentas, connect, cargos, new StripeCargoRegistroService(pagos, "sk_test_demo"),
            new TransactionTemplate(mock(PlatformTransactionManager.class)), "sk_test_demo");

    @BeforeEach
    void preparar() throws Exception {
        var pago = new TransaccionBancaria();
        pago.setTransaccionId(UUID.randomUUID());
        pago.setEstadoTransaccion(EstadoTransaccion.EXITOSA);
        pago.setTipoTransaccion(TipoTransaccion.PAGO_RESTAURACION);
        pago.setMontoBrutoMxnTransaccion(new BigDecimal("100.00"));
        pago.setReferenciaStripeTransaccion("cs_origen");
        var pieza = new Antiguedad();
        pieza.setAntiguedadesId(UUID.randomUUID());
        var usuario = new Usuario();
        usuario.setUsuariosId(UUID.randomUUID());
        pago.setAntiguedad(pieza);
        pago.setBeneficiario(usuario);
        liberacion.setAntiguedad(pieza);
        liberacion.setBeneficiario(usuario);
        liberacion.setTransaccionId(id);
        liberacion.setTransaccionPadre(pago);
        liberacion.setTipoTransaccion(TipoTransaccion.LIBERACION_RESTAURADOR);
        liberacion.setMontoBrutoMxnTransaccion(pago.getMontoBrutoMxnTransaccion());
        liberacion.setComisionMjrenewMxn(new BigDecimal("7.00"));
        liberacion.setMontoAntesCargoStripeMxn(new BigDecimal("93.00"));
        liberacion.setEstadoTransaccion(EstadoTransaccion.PENDIENTE);
        when(pagos.findByTipoTransaccionInAndEstadoTransaccion(anyList(), any())).thenReturn(List.of(liberacion));
        when(pagos.bloquearPorId(id)).thenReturn(Optional.of(liberacion));
        var cuenta = new CuentaStripeConectada();
        cuenta.setStripeAccountId("acct_destino");
        when(cuentas.findByUsuario_UsuariosId(usuario.getUsuariosId())).thenReturn(Optional.of(cuenta));
        when(connect.consultarEstado("acct_destino")).thenReturn(new StripeCuentaConectadaEstado(true,true,false));
        var sesion = new StripeCheckoutEstado("cs_origen", "PAGO_RESTAURACION", pago.getTransaccionId().toString(),
                "complete", "paid", "payment", "mxn", 10000L, false);
        when(cargos.consultar("cs_origen")).thenReturn(new CargoRealStripe(sesion,"pi_pago","ch_pago","txn_saldo",
                10000L, 375L, "mxn", false));
    }

    @Test
    void disputaCongelaPagoAntesDelPrimerIntento() {
        when(disputas.existsByAntiguedad_AntiguedadesIdAndResueltaEnDisputaIsNull(any())).thenReturn(true);
        try (var stripe = mockStatic(Transfer.class)) {
            servicio.reintentarPendientes();
            stripe.verifyNoInteractions();
            assertNull(liberacion.getPrimerIntentoTransferencia());
        }
    }

    @Test
    void pagaLaVentaAlPropietarioSinEsperarEntrega() throws Exception {
        var pago = liberacion.getTransaccionPadre();
        pago.setTipoTransaccion(TipoTransaccion.PAGO_VENTA);
        liberacion.setTipoTransaccion(TipoTransaccion.LIBERACION_PROPIETARIO);
        var sesion = new StripeCheckoutEstado("cs_origen", "PAGO_VENTA", pago.getTransaccionId().toString(),
                "complete", "paid", "payment", "mxn", 10000L, false);
        when(cargos.consultar("cs_origen")).thenReturn(new CargoRealStripe(sesion, "pi_pago", "ch_pago",
                "txn_saldo", 10000L, 375L, "mxn", false));
        try (var stripe = mockStatic(Transfer.class)) {
            stripe.when(() -> Transfer.create(any(TransferCreateParams.class), any(RequestOptions.class)))
                    .thenAnswer(inv -> {
                        TransferCreateParams params = inv.getArgument(0);
                        var respuesta = new Transfer();
                        respuesta.setId("tr_venta");
                        respuesta.setCurrency(params.getCurrency());
                        respuesta.setDestination(params.getDestination());
                        respuesta.setTransferGroup(params.getTransferGroup());
                        respuesta.setAmount(params.getAmount());
                        respuesta.setAmountReversed(0L);
                        respuesta.setSourceTransaction(params.getSourceTransaction());
                        return respuesta;
                    });
            servicio.reintentarPendientes();
            assertEquals("tr_venta", liberacion.getReferenciaStripeTransaccion());
            assertEquals(new BigDecimal("89.25"), liberacion.getMontoNetoPropietarioMxn());
            assertNull(liberacion.getMontoNetoRestauradorMxn());
            assertEquals(EstadoTransaccion.EXITOSA, liberacion.getEstadoTransaccion());
        }
    }

    @Test
    void envioAmbiguoSeConciliaSinSegundaTransferencia() {
        var transferencia = new Transfer();
        transferencia.setId("tr_confirmada");
        transferencia.setTransferGroup("mjrenew-" + id);
        transferencia.setAmount(8925L);
        transferencia.setAmountReversed(0L);
        transferencia.setCurrency("mxn");
        transferencia.setSourceTransaction("ch_pago");
        transferencia.setDestination("acct_destino");
        var lista = new TransferCollection();
        lista.setData(List.of(transferencia));
        try (var stripe = mockStatic(Transfer.class)) {
            stripe.when(() -> Transfer.create(any(TransferCreateParams.class), any(RequestOptions.class)))
                    .thenAnswer(inv -> {
                        TransferCreateParams params = inv.getArgument(0);
                        RequestOptions opciones = inv.getArgument(1);
                        assertEquals(8925L, params.getAmount());
                        assertEquals("acct_destino", params.getDestination());
                        assertEquals("ch_pago", params.getSourceTransaction());
                        assertEquals("mjrenew-" + id, opciones.getIdempotencyKey());
                        throw new IllegalStateException("Respuesta perdida");
                    });
            stripe.when(() -> Transfer.list(any(TransferListParams.class), any(RequestOptions.class))).thenReturn(lista);
            servicio.reintentarPendientes();
            assertEquals(EstadoTransaccion.PENDIENTE, liberacion.getEstadoTransaccion());
            servicio.reintentarPendientes();
            assertEquals(EstadoTransaccion.EXITOSA, liberacion.getEstadoTransaccion());
            assertEquals("tr_confirmada", liberacion.getReferenciaStripeTransaccion());
            assertNull(liberacion.getMontoNetoPropietarioMxn());
            stripe.verify(() -> Transfer.create(any(TransferCreateParams.class), any(RequestOptions.class)), times(1));
        }
    }
}
