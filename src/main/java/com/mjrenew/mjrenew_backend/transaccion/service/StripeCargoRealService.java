package com.mjrenew.mjrenew_backend.transaccion.service;

import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.transaccion.dto.CargoRealStripe;
import com.mjrenew.mjrenew_backend.transaccion.dto.StripeCheckoutEstado;
import com.stripe.exception.StripeException;
import com.stripe.model.BalanceTransaction;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;

/** Consulta Stripe sin abrir una transacción de base de datos ni mover fondos. */
@Service
public class StripeCargoRealService {
    private final RequestOptions opciones;

    public StripeCargoRealService(@Value("${stripe.secret-key}") String stripeSecretKey) {
        this.opciones = RequestOptions.builder().setApiKey(stripeSecretKey).build();
    }

    /** Retorna null si Stripe todavía no publicó la transacción de saldo. */
    public CargoRealStripe consultar(String checkoutId) throws StripeException {
        Session sesion = Session.retrieve(checkoutId, opciones);
        StripeCheckoutEstado estado = StripeCheckoutEstado.desdeStripe(sesion);
        if (!"complete".equals(estado.estadoSesion()) || !"paid".equals(estado.estadoPago())
                || !"payment".equals(estado.modalidad()) || sesion.getPaymentIntent() == null) {
            throw new SolicitudInvalidaException("La sesión Stripe no corresponde a una venta pagada");
        }

        PaymentIntent intento = PaymentIntent.retrieve(sesion.getPaymentIntent(), opciones);
        if (!"succeeded".equals(intento.getStatus()) || intento.getLatestCharge() == null
                || !Objects.equals(intento.getId(), sesion.getPaymentIntent())
                || intento.getAmountReceived() == null || intento.getAmountReceived() <= 0
                || !Objects.equals(intento.getAmountReceived(), estado.montoCentavos())
                || !Objects.equals(intento.getCurrency(), estado.moneda())) {
            throw new SolicitudInvalidaException("El PaymentIntent de la venta no está confirmado");
        }
        Charge cargo = Charge.retrieve(intento.getLatestCharge(), opciones);
        if (!Boolean.TRUE.equals(cargo.getPaid()) || !Boolean.TRUE.equals(cargo.getCaptured())
                || !"succeeded".equals(cargo.getStatus())
                || !Boolean.FALSE.equals(cargo.getDisputed())
                || !Boolean.FALSE.equals(cargo.getRefunded())
                || cargo.getAmountRefunded() == null || cargo.getAmountRefunded() != 0L
                || !Objects.equals(cargo.getPaymentIntent(), intento.getId())
                || !Objects.equals(cargo.getAmount(), intento.getAmountReceived())
                || !Objects.equals(cargo.getAmountCaptured(), cargo.getAmount())
                || !Objects.equals(cargo.getCurrency(), intento.getCurrency())) {
            throw new SolicitudInvalidaException("El cargo Stripe no admite conciliación");
        }
        if (cargo.getBalanceTransaction() == null) {
            return null;
        }
        BalanceTransaction saldo = BalanceTransaction.retrieve(cargo.getBalanceTransaction(), opciones);
        if (!Objects.equals(saldo.getSource(), cargo.getId())
                || saldo.getAmount() == null || saldo.getAmount() <= 0
                || !Objects.equals(saldo.getAmount(), cargo.getAmount())
                || saldo.getFee() == null || saldo.getFee() < 0
                || !Objects.equals(saldo.getNet(), saldo.getAmount() - saldo.getFee())
                || !Objects.equals(saldo.getCurrency(), cargo.getCurrency())
                || !Objects.equals(estado.enProduccion(), intento.getLivemode())
                || !Objects.equals(estado.enProduccion(), cargo.getLivemode())) {
            throw new SolicitudInvalidaException("La transacción de saldo Stripe no coincide con el cargo");
        }
        return new CargoRealStripe(estado, intento.getId(), cargo.getId(), saldo.getId(),
                cargo.getAmount(), saldo.getFee(), saldo.getCurrency(), estado.enProduccion());
    }
}
