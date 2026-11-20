package com.mjrenew.mjrenew_backend.transaccion.service;

import com.mjrenew.mjrenew_backend.administrador.repository.DisputaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTransaccion;
import com.mjrenew.mjrenew_backend.propietario.repository.CuentaStripeConectadaRepository;
import com.mjrenew.mjrenew_backend.transaccion.entity.TransaccionBancaria;
import com.mjrenew.mjrenew_backend.transaccion.repository.TransaccionBancariaRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Transfer;
import com.stripe.net.RequestOptions;
import com.stripe.param.TransferCreateParams;
import com.stripe.param.TransferListParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "mjrenew.stripe.transfers-enabled", havingValue = "true")
public class TransferenciasConnectService {
    private static final Logger log = LoggerFactory.getLogger(TransferenciasConnectService.class);
    private final TransaccionBancariaRepository pagos;
    private final DisputaAntiguedadRepository disputas;
    private final CuentaStripeConectadaRepository cuentas;
    private final StripeConnectService connect;
    private final StripeCargoRealService cargos;
    private final StripeCargoRegistroService registro;
    private final TransactionTemplate tx;
    private final String clave;

    public TransferenciasConnectService(TransaccionBancariaRepository pagos, DisputaAntiguedadRepository disputas,
            CuentaStripeConectadaRepository cuentas, StripeConnectService connect, StripeCargoRealService cargos,
            StripeCargoRegistroService registro, TransactionTemplate tx, @Value("${stripe.secret-key}") String clave) {
        this.pagos = pagos;
        this.disputas = disputas;
        this.cuentas = cuentas;
        this.connect = connect;
        this.cargos = cargos;
        this.registro = registro;
        this.tx = tx;
        this.clave = clave;
    }

    @Async
    @TransactionalEventListener
    public void alAutorizar(TransaccionBancaria.PagoPorLiberar evento) {
        pagos.findByTransaccionPadre_TransaccionIdAndTipoTransaccion(evento.pagoId(), evento.tipo())
                .ifPresent(this::procesar);
    }

    @Scheduled(fixedDelayString = "${mjrenew.stripe.transfer-retry-ms:60000}")
    public void reintentarPendientes() {
        pagos.findByTipoTransaccionInAndEstadoTransaccion(List.of(TipoTransaccion.LIBERACION_PROPIETARIO,
                TipoTransaccion.LIBERACION_RESTAURADOR), EstadoTransaccion.PENDIENTE).forEach(this::procesar);
    }

    private void procesar(TransaccionBancaria liberacion) {
        UUID id = liberacion.getTransaccionId();
        try {
            if (liberacion.getEstadoTransaccion() != EstadoTransaccion.PENDIENTE) return;
            var pago = liberacion.getTransaccionPadre();
            if (pago == null || pago.getBeneficiario() == null
                    || !pago.getBeneficiario().getUsuariosId().equals(liberacion.getBeneficiario().getUsuariosId())
                    || !pago.getAntiguedad().getAntiguedadesId().equals(liberacion.getAntiguedad().getAntiguedadesId())) {
                throw new IllegalStateException("La pieza o el beneficiario difieren del pago original");
            }
            var cuenta = cuentas.findByUsuario_UsuariosId(liberacion.getBeneficiario().getUsuariosId());
            if (cuenta.isEmpty()) return;
            String destino = cuenta.get().getStripeAccountId();
            String grupo = "mjrenew-" + id;
            var opciones = RequestOptions.builder().setApiKey(clave).build();
            // Recupera una respuesta perdida incluso después de expirar la clave de idempotencia.
            if (liberacion.getPrimerIntentoTransferencia() != null) {
                var anteriores = Transfer.list(TransferListParams.builder().setTransferGroup(grupo).setLimit(2L).build(),
                        opciones).getData();
                if (!anteriores.isEmpty()) {
                    if (anteriores.size() != 1) throw new IllegalStateException("Hay varias transferencias para la liberación");
                    confirmar(id, anteriores.get(0));
                    return;
                }
            }
            if (!connect.consultarEstado(destino).transferenciasHabilitadas()) return;
            var cargo = cargos.consultar(liberacion.getTransaccionPadre().getReferenciaStripeTransaccion());
            if (cargo == null) return;
            var params = tx.execute(estado -> {
                var actual = pagos.bloquearPorId(id).orElseThrow();
                if (actual.getEstadoTransaccion() != EstadoTransaccion.PENDIENTE
                        || disputas.existsByAntiguedad_AntiguedadesIdAndResueltaEnDisputaIsNull(
                                actual.getAntiguedad().getAntiguedadesId())) return null;
                registro.registrar(id, cargo);
                if (actual.getPrimerIntentoTransferencia() == null) actual.iniciarTransferencia(destino);
                if (!destino.equals(actual.getDestinoTransferencia())
                        || actual.getPrimerIntentoTransferencia().isBefore(OffsetDateTime.now().minusHours(23))) {
                    throw new IllegalStateException("Revisar el intento anterior antes de reenviar");
                }
                return TransferCreateParams.builder().setAmount(centavos(actual)).setCurrency("mxn")
                        .setSourceTransaction(actual.getStripeChargeId()).setDestination(destino)
                        .setTransferGroup(grupo).build();
            });
            if (params != null) {
                var transferencia = Transfer.create(params, RequestOptions.builder().setApiKey(clave)
                        .setIdempotencyKey(grupo).build());
                confirmar(id, transferencia);
            }
        } catch (StripeException | RuntimeException ex) {
            log.warn("Liberación {} pendiente de conciliación: {}", id, ex.getMessage());
        }
    }

    private long centavos(TransaccionBancaria liberacion) {
        var neto = liberacion.getTipoTransaccion() == TipoTransaccion.LIBERACION_PROPIETARIO
                ? liberacion.getMontoNetoPropietarioMxn() : liberacion.getMontoNetoRestauradorMxn();
        return neto.movePointRight(2).longValueExact();
    }

    private void confirmar(UUID id, Transfer transferencia) {
        tx.executeWithoutResult(estado -> {
            var actual = pagos.bloquearPorId(id).orElseThrow();
            if (transferencia.getId() == null || !("mjrenew-" + id).equals(transferencia.getTransferGroup())
                    || !actual.getDestinoTransferencia().equals(transferencia.getDestination())
                    || !actual.getStripeChargeId().equals(transferencia.getSourceTransaction())
                    || !"mxn".equals(transferencia.getCurrency()) || transferencia.getAmount() != centavos(actual)
                    || !Long.valueOf(0).equals(transferencia.getAmountReversed())) {
                throw new IllegalStateException("La transferencia no coincide con la liberación");
            }
            if (actual.getEstadoTransaccion() == EstadoTransaccion.PENDIENTE) {
                actual.setReferenciaStripeTransaccion(transferencia.getId());
                actual.setEstadoTransaccion(EstadoTransaccion.EXITOSA);
                actual.setProcesadaEnTransaccion(OffsetDateTime.now());
            }
        });
    }
}
