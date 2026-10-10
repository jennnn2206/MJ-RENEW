package com.mjrenew.mjrenew_backend.transaccion.service;

import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoTransaccion;
import com.mjrenew.mjrenew_backend.transaccion.dto.StripeCheckoutEstado;
import com.mjrenew.mjrenew_backend.transaccion.entity.TransaccionBancaria;
import com.mjrenew.mjrenew_backend.transaccion.repository.TransaccionBancariaRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Recuperación genérica de notificaciones perdidas. SIN IDs hardcodeados y SIN endpoint HTTP.
 * Solo habilitar cuando la regla de negocio permita confirmaciones por consulta autenticada a Stripe.
 */
@Service
@ConditionalOnProperty(name = "mjrenew.stripe.reconciliation-enabled", havingValue = "true")
public class StripeConciliacionService {
    private static final Logger log = LoggerFactory.getLogger(StripeConciliacionService.class);
    private final TransaccionBancariaRepository transacciones;
    private final StripeCheckoutService stripe;
    private final StripePagoEstadoService pagos;

    public StripeConciliacionService(TransaccionBancariaRepository transacciones,
                                     StripeCheckoutService stripe, StripePagoEstadoService pagos) {
        this.transacciones = transacciones;
        this.stripe = stripe;
        this.pagos = pagos;
    }

    @Scheduled(initialDelayString = "${mjrenew.stripe.reconciliation-initial-delay-ms:120000}",
            fixedDelayString = "${mjrenew.stripe.reconciliation-delay-ms:60000}")
    public void revisarPendientes() {
        // No se mantienen transacciones JDBC abiertas durante la llamada de red a Stripe.
        List<TransaccionBancaria> pendientes = transacciones
                .findTop50ByEstadoTransaccionAndReferenciaStripeTransaccionIsNotNullAndCreadaEnTransaccionBeforeOrderByCreadaEnTransaccionAsc(
                        EstadoTransaccion.PENDIENTE, OffsetDateTime.now().minusMinutes(1));
        for (TransaccionBancaria t : pendientes) {
            String referencia = t.getReferenciaStripeTransaccion();
            try {
                Session sesion = stripe.consultarSesionPorId(referencia);
                StripeCheckoutEstado datos = StripeCheckoutEstado.desdeStripe(sesion);
                if ("paid".equals(datos.estadoPago()) && "complete".equals(datos.estadoSesion())) {
                    pagos.confirmar(datos);
                } else if ("expired".equals(datos.estadoSesion()) && !"paid".equals(datos.estadoPago())) {
                    pagos.marcarNoCompletado(datos);
                }
                // Checkout completado pero pago asíncrono pendiente: esperar evento definitivo.
            } catch (StripeException ex) {
                log.warn("No se pudo consultar Checkout {}: {}", referencia, ex.getMessage());
            } catch (RuntimeException ex) {
                log.error("Fallo al conciliar sesión Stripe {}: revisar la transacción", referencia, ex);
            }
        }
    }
}
