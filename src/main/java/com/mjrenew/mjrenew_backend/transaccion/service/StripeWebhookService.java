package com.mjrenew.mjrenew_backend.transaccion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.exception.TransicionEstadoInvalidaException;
import com.mjrenew.mjrenew_backend.transaccion.entity.TransaccionBancaria;
import com.mjrenew.mjrenew_backend.transaccion.repository.TransaccionBancariaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class StripeWebhookService {

    private final TransaccionBancariaRepository transaccionRepository;
    private final ObjectMapper objectMapper;

    public StripeWebhookService(
            TransaccionBancariaRepository transaccionRepository,
            ObjectMapper objectMapper
    ) {
        this.transaccionRepository = transaccionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void procesarEvento(String payload) {

        JsonNode evento;

        try {
            evento = objectMapper.readTree(payload);
        } catch (JsonProcessingException ex) {
            throw new SolicitudInvalidaException(
                    "El evento recibido de Stripe no tiene un formato válido"
            );
        }

        String tipoEvento = evento
                .path("type")
                .asText();

        /*
         * Por ahora solo procesamos el evento de pago completado.
         * Los demás eventos se ignoran hasta implementar Stripe real.
         */
        if (!"checkout.session.completed".equals(tipoEvento)) {
            return;
        }

        String referenciaStripe = evento
                .path("data")
                .path("object")
                .path("id")
                .asText();

        if (referenciaStripe == null || referenciaStripe.isBlank()) {
            throw new SolicitudInvalidaException(
                    "El evento de Stripe no contiene una referencia de pago"
            );
        }

        TransaccionBancaria transaccion =
                transaccionRepository
                        .findByReferenciaStripeTransaccion(
                                referenciaStripe
                        )
                        .orElseThrow(() ->
                                new RecursoNoEncontradoException(
                                        "No existe una transacción asociada a la referencia Stripe recibida"
                                )
                        );

        /*
         * Stripe puede reenviar el mismo evento.
         * Si ya fue procesado, no repetimos las transiciones.
         */
        if (transaccion.getEstadoTransaccion()
                == EstadoTransaccion.EXITOSA) {
            return;
        }

        if (transaccion.getEstadoTransaccion()
                != EstadoTransaccion.PENDIENTE) {

            throw new SolicitudInvalidaException(
                    "La transacción ya no se encuentra pendiente"
            );
        }

        if (transaccion.getTipoTransaccion()
                == TipoTransaccion.PAGO_VENTA) {

            procesarPagoVenta(transaccion);

        } else if (transaccion.getTipoTransaccion()
                == TipoTransaccion.PAGO_RESTAURACION) {

            procesarPagoRestauracion(transaccion);
        }
    }

    private void procesarPagoVenta(
            TransaccionBancaria transaccion
    ) {

        Antiguedad antiguedad =
                transaccion.getAntiguedad();

        if (antiguedad.getEstadoActualAntiguedad()
                != EstadoAntiguedad.LINK_PAGO_ENVIADO) {

            throw new TransicionEstadoInvalidaException(
                    "La antigüedad debe estar en LINK_PAGO_ENVIADO para confirmar la venta"
            );
        }

        CatalogoAntiguedad catalogo =
                transaccion.getCatalogoAntiguedad();

        if (catalogo == null) {
            throw new SolicitudInvalidaException(
                    "La transacción de venta no tiene una publicación de catálogo asociada"
            );
        }

        antiguedad.setEstadoActualAntiguedad(
                EstadoAntiguedad.VENTA_COMPLETADA
        );

        catalogo.setActivaCatalogo(false);
        catalogo.setCompletadaEnCatalogo(
                OffsetDateTime.now()
        );

        marcarTransaccionExitosa(transaccion);
    }

    private void procesarPagoRestauracion(
            TransaccionBancaria transaccion
    ) {

        Antiguedad antiguedad =
                transaccion.getAntiguedad();

        if (antiguedad.getEstadoActualAntiguedad()
                != EstadoAntiguedad.PRESUPUESTO_PRESENTADO) {

            throw new TransicionEstadoInvalidaException(
                    "La antigüedad debe estar en PRESUPUESTO_PRESENTADO para confirmar el pago de restauración"
            );
        }

        antiguedad.setEstadoActualAntiguedad(
                EstadoAntiguedad.PAGO_EN_ESCROW
        );

        marcarTransaccionExitosa(transaccion);
    }

    private void marcarTransaccionExitosa(
            TransaccionBancaria transaccion
    ) {

        transaccion.setEstadoTransaccion(
                EstadoTransaccion.EXITOSA
        );

        transaccion.setProcesadaEnTransaccion(
                OffsetDateTime.now()
        );
    }
}