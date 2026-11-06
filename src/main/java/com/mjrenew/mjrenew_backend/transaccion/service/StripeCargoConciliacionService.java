package com.mjrenew.mjrenew_backend.transaccion.service;

import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTransaccion;
import com.mjrenew.mjrenew_backend.transaccion.dto.CargoRealStripe;
import com.mjrenew.mjrenew_backend.transaccion.entity.TransaccionBancaria;
import com.mjrenew.mjrenew_backend.transaccion.repository.TransaccionBancariaRepository;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Reintenta la lectura del fee sin mantener una transacción JDBC durante la llamada a Stripe. */
@Service
@ConditionalOnProperty(name = "mjrenew.stripe.fee-reconciliation-enabled", havingValue = "true")
public class StripeCargoConciliacionService {
    private static final Logger log = LoggerFactory.getLogger(StripeCargoConciliacionService.class);
    private final TransaccionBancariaRepository transacciones;
    private final StripeCargoRealService stripe;
    private final StripeCargoRegistroService registro;

    public StripeCargoConciliacionService(TransaccionBancariaRepository transacciones,
                                          StripeCargoRealService stripe,
                                          StripeCargoRegistroService registro) {
        this.transacciones = transacciones;
        this.stripe = stripe;
        this.registro = registro;
    }

    @Scheduled(initialDelayString = "${mjrenew.stripe.reconciliation-initial-delay-ms:120000}",
            fixedDelayString = "${mjrenew.stripe.fee-reconciliation-delay-ms:300000}")
    public void revisarCargosPendientes() {
        for (TransaccionBancaria liberacion : transacciones
                .findByTipoTransaccionAndEstadoTransaccionAndCargoStripeMxnIsNull(
                        TipoTransaccion.LIBERACION_PROPIETARIO, EstadoTransaccion.PENDIENTE)) {
            try {
                TransaccionBancaria pago = liberacion.getTransaccionPadre();
                if (pago == null || pago.getReferenciaStripeTransaccion() == null) {
                    log.warn("Liberación {} sin sesión Stripe de origen", liberacion.getTransaccionId());
                    continue;
                }
                CargoRealStripe cargo = stripe.consultar(pago.getReferenciaStripeTransaccion());
                if (cargo != null) {
                    registro.registrar(liberacion.getTransaccionId(), cargo);
                }
            } catch (StripeException ex) {
                log.warn("No se pudo consultar el cargo Stripe de liberación {}: {}",
                        liberacion.getTransaccionId(), ex.getMessage());
            } catch (RuntimeException ex) {
                log.error("No se pudo conciliar el cargo Stripe de liberación {}",
                        liberacion.getTransaccionId(), ex);
            }
        }
    }
}
