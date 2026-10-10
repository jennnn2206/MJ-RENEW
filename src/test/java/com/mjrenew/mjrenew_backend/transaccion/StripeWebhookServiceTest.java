package com.mjrenew.mjrenew_backend.transaccion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.transaccion.dto.StripeCheckoutEstado;
import com.mjrenew.mjrenew_backend.transaccion.service.StripePagoEstadoService;
import com.mjrenew.mjrenew_backend.transaccion.service.StripeWebhookService;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StripeWebhookServiceTest {
    private final StripePagoEstadoService pagos = mock(StripePagoEstadoService.class);
    private final StripeWebhookService webhook =
            new StripeWebhookService(pagos, new ObjectMapper(), "whsec_secreto_de_prueba");

    @Test
    void firmaValidaYEventoDeNuevaVersionSeProcesanSinDeserializadorDelSdk() throws Exception {
        String json = evento("checkout.session.completed");
        webhook.procesarEvento(json, firmar(json));
        verify(pagos).confirmar(any(StripeCheckoutEstado.class));
    }

    @Test
    void firmaIncorrectaRechazadaAntesDeConsultarLaBase() {
        String json = evento("checkout.session.completed");
        assertThrows(SolicitudInvalidaException.class,
                () -> webhook.procesarEvento(json, "t=1234,v1=falsificada"));
        verifyNoInteractions(pagos);
    }

    @Test
    void eventoAjenoNoAlteraLaTransaccion() throws Exception {
        String json = evento("payment_intent.created");
        webhook.procesarEvento(json, firmar(json));
        verifyNoInteractions(pagos);
    }

    private String evento(String tipo) {
        return """
                {
                  "id": "evt_test_123", "object": "event",
                  "api_version": "2026-08-26.dahlia",
                  "type": "%s",
                  "data": {"object": {
                    "object": "checkout.session", "id": "cs_test_123",
                    "status": "complete", "payment_status": "paid", "mode": "payment",
                    "currency": "mxn", "amount_total": 650000, "livemode": false,
                    "metadata": {"tipoTransaccion": "PAGO_RESTAURACION", "transaccionId": "%s"}
                  }}
                }
                """.formatted(tipo, UUID.randomUUID());
    }

    private String firmar(String payload) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec("whsec_secreto_de_prueba".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] bytes = hmac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
        return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(bytes);
    }
}
