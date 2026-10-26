package com.mjrenew.mjrenew_backend.transaccion.service;

import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.enums.FlujoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.transaccion.entity.TransaccionBancaria;
import com.mjrenew.mjrenew_backend.transaccion.repository.TransaccionBancariaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Prepara el registro de liberación al propietario para una venta del catálogo.
 * No ejecuta una transferencia real: esta última requiere la cuenta conectada del
 * propietario y la integración de Stripe Connect, todavía no configuradas.
 */
@Service
public class LiberacionPropietarioService {
    private static final Logger log = LoggerFactory.getLogger(LiberacionPropietarioService.class);

    private final TransaccionBancariaRepository transacciones;

    public LiberacionPropietarioService(TransaccionBancariaRepository transacciones) {
        this.transacciones = transacciones;
    }

    /**
     * Registra una sola operación pendiente por cada PAGO_VENTA confirmado.
     * El monto guardado es el bruto de la venta como referencia de trazabilidad;
     * no representa todavía el neto transferible al propietario.
     */
    @Transactional
    public TransaccionBancaria registrarPendiente(TransaccionBancaria pagoVenta) {
        validarPagoVenta(pagoVenta);

        return transacciones.findByTransaccionPadre_TransaccionIdAndTipoTransaccion(
                        pagoVenta.getTransaccionId(), TipoTransaccion.LIBERACION_PROPIETARIO)
                .orElseGet(() -> crearLiberacionPendiente(pagoVenta));
    }

    private TransaccionBancaria crearLiberacionPendiente(TransaccionBancaria pagoVenta) {
        CatalogoAntiguedad catalogo = pagoVenta.getCatalogoAntiguedad();
        Antiguedad antiguedad = catalogo.getAntiguedad();
        Usuario propietario = antiguedad.getPropietario();

        TransaccionBancaria liberacion = new TransaccionBancaria();
        liberacion.setTipoTransaccion(TipoTransaccion.LIBERACION_PROPIETARIO);
        liberacion.setFlujoMjrenew(FlujoTransaccion.EGRESO);
        // Se conserva el pagador original para rastrear el origen de los fondos.
        liberacion.setOriginator(pagoVenta.getOriginator());
        liberacion.setBeneficiario(propietario);
        liberacion.setAntiguedad(antiguedad);
        liberacion.setCatalogoAntiguedad(catalogo);
        liberacion.setTransaccionPadre(pagoVenta);
        liberacion.setMontoBrutoMxnTransaccion(pagoVenta.getMontoBrutoMxnTransaccion());
        liberacion.setEstadoTransaccion(EstadoTransaccion.PENDIENTE);
        liberacion.setCreadaEnTransaccion(OffsetDateTime.now());

        TransaccionBancaria guardada = transacciones.save(liberacion);
        log.info("Liberación al propietario registrada como pendiente para pagoVentaId={}",
                pagoVenta.getTransaccionId());
        return guardada;
    }

    private void validarPagoVenta(TransaccionBancaria pagoVenta) {
        if (pagoVenta == null
                || pagoVenta.getTipoTransaccion() != TipoTransaccion.PAGO_VENTA
                || pagoVenta.getEstadoTransaccion() != EstadoTransaccion.EXITOSA) {
            throw new SolicitudInvalidaException(
                    "Solo se puede preparar la liberación al propietario después de confirmar un PAGO_VENTA");
        }

        CatalogoAntiguedad catalogo = pagoVenta.getCatalogoAntiguedad();
        if (catalogo == null || catalogo.getAntiguedad() == null
                || catalogo.getComprador() == null || pagoVenta.getOriginator() == null
                || pagoVenta.getBeneficiario() == null || pagoVenta.getAntiguedad() == null) {
            throw new SolicitudInvalidaException(
                    "El pago de catálogo no contiene comprador, propietario, antigüedad y publicación completos");
        }

        Antiguedad antiguedad = catalogo.getAntiguedad();
        Usuario comprador = catalogo.getComprador();
        Usuario propietario = antiguedad.getPropietario();
        if (propietario == null
                || !Objects.equals(comprador.getUsuariosId(), pagoVenta.getOriginator().getUsuariosId())
                || !Objects.equals(propietario.getUsuariosId(), pagoVenta.getBeneficiario().getUsuariosId())
                || !Objects.equals(antiguedad.getAntiguedadesId(), pagoVenta.getAntiguedad().getAntiguedadesId())
                || pagoVenta.getMontoBrutoMxnTransaccion() == null
                || pagoVenta.getMontoBrutoMxnTransaccion().signum() <= 0) {
            throw new SolicitudInvalidaException(
                    "Los participantes o el monto del pago no coinciden con la venta del catálogo");
        }
    }
}
