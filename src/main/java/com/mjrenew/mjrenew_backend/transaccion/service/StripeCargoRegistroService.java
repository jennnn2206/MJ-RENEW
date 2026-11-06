package com.mjrenew.mjrenew_backend.transaccion.service;

import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.transaccion.dto.CargoRealStripe;
import com.mjrenew.mjrenew_backend.transaccion.dto.StripeCheckoutEstado;
import com.mjrenew.mjrenew_backend.transaccion.entity.TransaccionBancaria;
import com.mjrenew.mjrenew_backend.transaccion.repository.TransaccionBancariaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** Guarda el cargo real una sola vez; la liberación permanece pendiente. */
@Service
public class StripeCargoRegistroService {
    private final TransaccionBancariaRepository transacciones;
    private final boolean entornoProduccion;

    public StripeCargoRegistroService(TransaccionBancariaRepository transacciones,
                                      @Value("${stripe.secret-key}") String stripeSecretKey) {
        this.transacciones = transacciones;
        this.entornoProduccion = stripeSecretKey.startsWith("sk_live_");
    }

    @Transactional
    public void registrar(UUID liberacionId, CargoRealStripe cargo) {
        TransaccionBancaria liberacion = transacciones.bloquearPorId(liberacionId)
                .orElseThrow(() -> new SolicitudInvalidaException("La liberación no existe"));
        TransaccionBancaria pago = liberacion.getTransaccionPadre();
        StripeCheckoutEstado sesion = cargo == null ? null : cargo.sesion();
        if (liberacion.getTipoTransaccion() != TipoTransaccion.LIBERACION_PROPIETARIO
                || liberacion.getEstadoTransaccion() != EstadoTransaccion.PENDIENTE
                || pago == null || pago.getTipoTransaccion() != TipoTransaccion.PAGO_VENTA
                || pago.getEstadoTransaccion() != EstadoTransaccion.EXITOSA
                || sesion == null || !Objects.equals(sesion.sesionId(), pago.getReferenciaStripeTransaccion())
                || !Objects.equals(sesion.transaccionId(), pago.getTransaccionId().toString())
                || !TipoTransaccion.PAGO_VENTA.name().equals(sesion.tipoOperacion())
                || !"complete".equals(sesion.estadoSesion()) || !"paid".equals(sesion.estadoPago())
                || !"payment".equals(sesion.modalidad())
                || !"mxn".equalsIgnoreCase(sesion.moneda())
                || !"mxn".equalsIgnoreCase(cargo.moneda())
                || !Objects.equals(sesion.enProduccion(), entornoProduccion)
                || !Objects.equals(cargo.enProduccion(), entornoProduccion)
                || cargo.montoCentavos() == null || cargo.cargoCentavos() == null
                || cargo.cargoCentavos() < 0
                || !Objects.equals(cargo.montoCentavos(), sesion.montoCentavos())
                || pago.getMontoBrutoMxnTransaccion() == null
                || pago.getMontoBrutoMxnTransaccion().movePointRight(2).longValueExact() != cargo.montoCentavos()
                || liberacion.getComisionMjrenewMxn() == null
                || liberacion.getMontoAntesCargoStripeMxn() == null
                || liberacion.getMontoBrutoMxnTransaccion() == null
                || !Objects.equals(liberacion.getMontoBrutoMxnTransaccion(), pago.getMontoBrutoMxnTransaccion())
                || !Objects.equals(liberacion.getMontoBrutoMxnTransaccion()
                        .subtract(liberacion.getComisionMjrenewMxn()),
                        liberacion.getMontoAntesCargoStripeMxn())
                || vacio(cargo.paymentIntentId()) || vacio(cargo.chargeId())
                || vacio(cargo.balanceTransactionId())) {
            throw new SolicitudInvalidaException("El cargo Stripe no coincide con la venta confirmada");
        }
        BigDecimal cargoMxn = BigDecimal.valueOf(cargo.cargoCentavos(), 2);
        BigDecimal neto = liberacion.getMontoAntesCargoStripeMxn().subtract(cargoMxn);
        if (neto.signum() <= 0) {
            throw new SolicitudInvalidaException("El cargo Stripe deja un importe no válido para el propietario");
        }
        if (liberacion.getCargoStripeMxn() != null) {
            if (liberacion.getCargoStripeMxn().compareTo(cargoMxn) != 0
                    || !Objects.equals(liberacion.getMontoNetoPropietarioMxn(), neto)
                    || !Objects.equals(liberacion.getStripePaymentIntentId(), cargo.paymentIntentId())
                    || !Objects.equals(liberacion.getStripeChargeId(), cargo.chargeId())
                    || !Objects.equals(liberacion.getStripeBalanceTransactionId(), cargo.balanceTransactionId())) {
                throw new SolicitudInvalidaException("El cargo Stripe ya conciliado tiene datos distintos");
            }
            return;
        }
        liberacion.setCargoStripeMxn(cargoMxn);
        liberacion.setMontoNetoPropietarioMxn(neto);
        liberacion.setStripePaymentIntentId(cargo.paymentIntentId());
        liberacion.setStripeChargeId(cargo.chargeId());
        liberacion.setStripeBalanceTransactionId(cargo.balanceTransactionId());
    }

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
