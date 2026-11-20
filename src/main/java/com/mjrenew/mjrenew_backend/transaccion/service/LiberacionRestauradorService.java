package com.mjrenew.mjrenew_backend.transaccion.service;

import java.math.RoundingMode;
import java.math.BigDecimal;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.*;
import com.mjrenew.mjrenew_backend.nucleo.exception.TransicionEstadoInvalidaException;
import com.mjrenew.mjrenew_backend.transaccion.entity.TransaccionBancaria;
import com.mjrenew.mjrenew_backend.transaccion.repository.TransaccionBancariaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class LiberacionRestauradorService {
    private final TransaccionBancariaRepository transacciones;

    public LiberacionRestauradorService(TransaccionBancariaRepository transacciones) {
        this.transacciones = transacciones;
    }

    // El invocador ya bloqueó la pieza del propietario que confirma la recepción.
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarRecepcion(Antiguedad pieza) {
        var pago = transacciones.findFirstByAntiguedad_AntiguedadesIdAndTipoTransaccionAndEstadoTransaccionOrderByCreadaEnTransaccionDesc(
                pieza.getAntiguedadesId(), TipoTransaccion.PAGO_RESTAURACION, EstadoTransaccion.EXITOSA)
                .orElseThrow(() -> new TransicionEstadoInvalidaException("Falta el pago confirmado de la restauración"));
        if (!pieza.getPropietario().getUsuariosId().equals(pago.getOriginator().getUsuariosId())
                || pieza.getRestaurador() == null
                || !pieza.getRestaurador().getUsuariosId().equals(pago.getBeneficiario().getUsuariosId())) {
            throw new TransicionEstadoInvalidaException("El pago no corresponde a los participantes de la restauración");
        }
        if (transacciones.findByTransaccionPadre_TransaccionIdAndTipoTransaccion(
                pago.getTransaccionId(), TipoTransaccion.LIBERACION_RESTAURADOR).isPresent()) return;

        var liberacion = new TransaccionBancaria();
        liberacion.setTipoTransaccion(TipoTransaccion.LIBERACION_RESTAURADOR);
        liberacion.setEstadoTransaccion(EstadoTransaccion.PENDIENTE);
        liberacion.setFlujoMjrenew(FlujoTransaccion.EGRESO);
        liberacion.setOriginator(pago.getOriginator());
        liberacion.setBeneficiario(pago.getBeneficiario());
        liberacion.setTransaccionPadre(pago);
        liberacion.setAntiguedad(pieza);
        liberacion.setMontoBrutoMxnTransaccion(pago.getMontoBrutoMxnTransaccion());
        var bruto = pago.getMontoBrutoMxnTransaccion();
        var comision = bruto.multiply(new BigDecimal("0.07")).setScale(2, RoundingMode.HALF_UP);
        liberacion.setComisionMjrenewMxn(comision);
        liberacion.setMontoAntesCargoStripeMxn(bruto.subtract(comision));
        liberacion.setCreadaEnTransaccion(OffsetDateTime.now());
        liberacion.avisarLiberacionPendiente();
        transacciones.save(liberacion);
    }
}
