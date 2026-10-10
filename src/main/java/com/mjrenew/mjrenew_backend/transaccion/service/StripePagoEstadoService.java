package com.mjrenew.mjrenew_backend.transaccion.service;

import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import com.mjrenew.mjrenew_backend.catalogo.repository.CatalogoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository.AntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.exception.TransicionEstadoInvalidaException;
import com.mjrenew.mjrenew_backend.transaccion.dto.StripeCheckoutEstado;
import com.mjrenew.mjrenew_backend.transaccion.entity.TransaccionBancaria;
import com.mjrenew.mjrenew_backend.transaccion.repository.TransaccionBancariaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Aplica estados de pago provenientes de fuentes confiables: webhook con firma validada
 * y, si se habilita explícitamente, consulta directa autenticada a Stripe.
 * Nunca recibe estados desde el navegador o parámetros públicos.
 */
@Service
public class StripePagoEstadoService {
    private static final Logger log = LoggerFactory.getLogger(StripePagoEstadoService.class);
    private final TransaccionBancariaRepository transacciones;
    private final AntiguedadRepository antiguedades;
    private final CatalogoAntiguedadRepository catalogos;
    private final boolean entornoProduccion;

    public StripePagoEstadoService(TransaccionBancariaRepository transacciones,
                                   AntiguedadRepository antiguedades,
                                   CatalogoAntiguedadRepository catalogos,
                                   @Value("${stripe.secret-key}") String stripeSecretKey) {
        this.transacciones = transacciones;
        this.antiguedades = antiguedades;
        this.catalogos = catalogos;
        this.entornoProduccion = stripeSecretKey.startsWith("sk_live_");
    }

    @Transactional
    public void confirmar(StripeCheckoutEstado sesion) {
        TransaccionBancaria transaccion = buscarYValidar(sesion);
        if (!"complete".equals(sesion.estadoSesion()) || !"paid".equals(sesion.estadoPago())) {
            return; // Checkout terminado no equivale siempre a pago completado.
        }
        validarMontoMoneda(sesion, transaccion);

        // El segundo bloqueo protege a la antigüedad ante distintas transacciones concurrentes.
        Antiguedad antiguedad = antiguedades.bloquearParaConfirmarPago(transaccion.getAntiguedad().getAntiguedadesId())
                .orElseThrow(() -> new SolicitudInvalidaException("La antigüedad pagada no existe"));

        if (transaccion.getEstadoTransaccion() == EstadoTransaccion.EXITOSA) {
            return; // Stripe reenvía eventos; una confirmación debe aplicarse una sola vez.
        }
        if (transaccion.getEstadoTransaccion() != EstadoTransaccion.PENDIENTE
                && transaccion.getEstadoTransaccion() != EstadoTransaccion.FALLIDA) {
            throw new SolicitudInvalidaException("La transacción no admite confirmación");
        }

        if (transaccion.getTipoTransaccion() == TipoTransaccion.PAGO_RESTAURACION) {
            exigirEstado(antiguedad, EstadoAntiguedad.PRESUPUESTO_PRESENTADO);
            antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.PAGO_EN_ESCROW);
        } else if (transaccion.getTipoTransaccion() == TipoTransaccion.PAGO_VENTA) {
            exigirEstado(antiguedad, EstadoAntiguedad.LINK_PAGO_ENVIADO);
            CatalogoAntiguedad catalogo = bloquearCatalogo(transaccion);
            if (!Objects.equals(sesion.sesionId(), catalogo.getIdLinkStripeCatalogo())) {
                throw new SolicitudInvalidaException("La sesión pagada no corresponde a la reserva vigente");
            }
            antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.VENTA_COMPLETADA);
            catalogo.setActivaCatalogo(false);
            catalogo.setCompletadaEnCatalogo(OffsetDateTime.now());
        } else {
            throw new SolicitudInvalidaException("Tipo de transacción Stripe no admitido");
        }
        transaccion.setEstadoTransaccion(EstadoTransaccion.EXITOSA);
        transaccion.setProcesadaEnTransaccion(OffsetDateTime.now());
        log.info("Pago Stripe confirmado para transaccionId={}", transaccion.getTransaccionId());
    }

    @Transactional
    public void marcarNoCompletado(StripeCheckoutEstado sesion) {
        TransaccionBancaria transaccion = buscarYValidar(sesion);
        if (transaccion.getEstadoTransaccion() != EstadoTransaccion.PENDIENTE) {
            return;
        }
        // Nunca degradar un pago confirmado ni confiar en un mensaje de fallo con payment_status=paid.
        if ("paid".equals(sesion.estadoPago())) {
            return;
        }
        Antiguedad antiguedad = antiguedades.bloquearParaConfirmarPago(transaccion.getAntiguedad().getAntiguedadesId())
                .orElseThrow(() -> new SolicitudInvalidaException("La antigüedad no existe"));
        transaccion.setEstadoTransaccion(EstadoTransaccion.FALLIDA);
        transaccion.setProcesadaEnTransaccion(OffsetDateTime.now());

        if (transaccion.getTipoTransaccion() == TipoTransaccion.PAGO_VENTA
                && antiguedad.getEstadoActualAntiguedad() == EstadoAntiguedad.LINK_PAGO_ENVIADO) {
            CatalogoAntiguedad catalogo = bloquearCatalogo(transaccion);
            // Un evento antiguo no puede liberar una reserva que ya pertenece a otra sesión.
            if (Objects.equals(sesion.sesionId(), catalogo.getIdLinkStripeCatalogo())) {
                antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.EN_CATALOGO);
                catalogo.setActivaCatalogo(true);
                catalogo.setComprador(null);
                catalogo.setIdLinkStripeCatalogo(null);
                catalogo.setUrlLinkPagoCatalogo(null);
            }
        }
        log.info("Pago Stripe no completado para transaccionId={}", transaccion.getTransaccionId());
    }

    private TransaccionBancaria buscarYValidar(StripeCheckoutEstado sesion) {
        if (sesion == null || sesion.sesionId() == null || sesion.sesionId().isBlank()) {
            throw new SolicitudInvalidaException("La sesión Stripe no contiene un identificador");
        }
        TransaccionBancaria transaccion = transacciones.findByReferenciaStripeTransaccion(sesion.sesionId())
                .orElseThrow(() -> new StripeTransaccionAunNoVisibleException(
                        "La sesión no está disponible todavía en PostgreSQL"));
        if (!Objects.equals(sesion.transaccionId(), transaccion.getTransaccionId().toString())
                || !Objects.equals(sesion.tipoOperacion(), transaccion.getTipoTransaccion().name())
                || !"payment".equals(sesion.modalidad())
                || sesion.enProduccion() == null || sesion.enProduccion() != entornoProduccion) {
            throw new SolicitudInvalidaException("El evento Stripe no coincide con la transacción registrada");
        }
        return transaccion;
    }

    private void validarMontoMoneda(StripeCheckoutEstado sesion, TransaccionBancaria transaccion) {
        long esperado = transaccion.getMontoBrutoMxnTransaccion().movePointRight(2).longValueExact();
        if (sesion.montoCentavos() == null || sesion.montoCentavos() != esperado
                || !"mxn".equalsIgnoreCase(sesion.moneda())) {
            throw new SolicitudInvalidaException("La moneda o el importe de Stripe no coincide");
        }
    }

    private CatalogoAntiguedad bloquearCatalogo(TransaccionBancaria transaccion) {
        if (transaccion.getCatalogoAntiguedad() == null) {
            throw new SolicitudInvalidaException("La transacción de venta no tiene publicación");
        }
        return catalogos.buscarParaCompra(transaccion.getCatalogoAntiguedad().getCatalogoAntiguedadId())
                .orElseThrow(() -> new SolicitudInvalidaException("La publicación no existe"));
    }

    private void exigirEstado(Antiguedad antiguedad, EstadoAntiguedad requerido) {
        if (antiguedad.getEstadoActualAntiguedad() != requerido) {
            throw new TransicionEstadoInvalidaException(
                    "La antigüedad debe estar en " + requerido + " para confirmar este pago");
        }
    }
}
