package com.mjrenew.mjrenew_backend.catalogo.service;

import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import com.mjrenew.mjrenew_backend.catalogo.repository.CatalogoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.enums.FlujoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.exception.TransicionEstadoInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.transaccion.dto.StripeCheckoutSession;
import com.mjrenew.mjrenew_backend.transaccion.entity.TransaccionBancaria;
import com.mjrenew.mjrenew_backend.transaccion.repository.TransaccionBancariaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/** Operaciones de base de datos cortas a ambos lados de la llamada a Stripe. */
@Service
public class CatalogoCompraReservaService {
    private final CatalogoAntiguedadRepository catalogos;
    private final TransaccionBancariaRepository transacciones;

    public CatalogoCompraReservaService(CatalogoAntiguedadRepository catalogos,
                                        TransaccionBancariaRepository transacciones) {
        this.catalogos = catalogos;
        this.transacciones = transacciones;
    }

    public record ReservaCompra(TransaccionBancaria pago, String referenciaStripe) {}

    @Transactional
    public ReservaCompra reservar(UUID catalogoId, Usuario comprador) {
        CatalogoAntiguedad catalogo = catalogos.buscarParaCompra(catalogoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una antigüedad en catálogo con id " + catalogoId));
        Antiguedad antiguedad = catalogo.getAntiguedad();

        if (!Boolean.TRUE.equals(catalogo.getActivaCatalogo())) {
            if (catalogo.getComprador() == null
                    || !Objects.equals(catalogo.getComprador().getUsuariosId(), comprador.getUsuariosId())
                    || antiguedad.getEstadoActualAntiguedad() != EstadoAntiguedad.LINK_PAGO_ENVIADO) {
                throw new SolicitudInvalidaException("Esta antigüedad ya no está disponible en el catálogo");
            }
            TransaccionBancaria pago = transacciones
                    .findFirstByCatalogoAntiguedad_CatalogoAntiguedadIdAndTipoTransaccionAndEstadoTransaccionOrderByCreadaEnTransaccionDesc(
                            catalogoId, TipoTransaccion.PAGO_VENTA, EstadoTransaccion.PENDIENTE)
                    .orElseThrow(() -> new SolicitudInvalidaException(
                            "La reserva no tiene un pago pendiente; requiere revisión"));
            if (!Objects.equals(pago.getOriginator().getUsuariosId(), comprador.getUsuariosId())) {
                throw new SolicitudInvalidaException("La reserva no corresponde al comprador actual");
            }
            return new ReservaCompra(pago, catalogo.getIdLinkStripeCatalogo());
        }

        if (antiguedad.getEstadoActualAntiguedad() != EstadoAntiguedad.EN_CATALOGO) {
            throw new TransicionEstadoInvalidaException(
                    "La antigüedad debe estar en EN_CATALOGO para iniciar una compra");
        }
        if (Objects.equals(antiguedad.getPropietario().getUsuariosId(), comprador.getUsuariosId())) {
            throw new SolicitudInvalidaException("No puedes comprar tu propia antigüedad");
        }

        TransaccionBancaria pago = new TransaccionBancaria();
        pago.setTipoTransaccion(TipoTransaccion.PAGO_VENTA);
        pago.setFlujoMjrenew(FlujoTransaccion.INGRESO);
        pago.setOriginator(comprador);
        pago.setBeneficiario(antiguedad.getPropietario());
        pago.setAntiguedad(antiguedad);
        pago.setCatalogoAntiguedad(catalogo);
        pago.setMontoBrutoMxnTransaccion(catalogo.getPrecioMxnCatalogo());
        pago.setEstadoTransaccion(EstadoTransaccion.PENDIENTE);
        pago.setCreadaEnTransaccion(OffsetDateTime.now());
        pago = transacciones.save(pago);

        catalogo.setComprador(comprador);
        catalogo.setActivaCatalogo(false);
        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.LINK_PAGO_ENVIADO);
        catalogos.save(catalogo);
        return new ReservaCompra(pago, null);
    }

    @Transactional
    public void vincularCheckout(UUID pagoId, UUID catalogoId, UUID compradorId,
                                 StripeCheckoutSession checkout) {
        TransaccionBancaria pago = transacciones.bloquearPorId(pagoId)
                .orElseThrow(() -> new SolicitudInvalidaException("El pago pendiente no existe"));
        CatalogoAntiguedad catalogo = catalogos.buscarParaCompra(catalogoId)
                .orElseThrow(() -> new SolicitudInvalidaException("La publicación reservada no existe"));

        if (!Objects.equals(pago.getCatalogoAntiguedad().getCatalogoAntiguedadId(), catalogoId)
                || !Objects.equals(pago.getOriginator().getUsuariosId(), compradorId)
                || catalogo.getComprador() == null
                || !Objects.equals(catalogo.getComprador().getUsuariosId(), compradorId)) {
            throw new SolicitudInvalidaException("El Checkout no corresponde a la reserva vigente");
        }
        if (Objects.equals(pago.getReferenciaStripeTransaccion(), checkout.referenciaStripe())
                && Objects.equals(catalogo.getIdLinkStripeCatalogo(), checkout.referenciaStripe())) {
            return; // Dos reintentos del mismo comprador recibieron la misma sesión de Stripe.
        }
        if (pago.getEstadoTransaccion() != EstadoTransaccion.PENDIENTE
                || Boolean.TRUE.equals(catalogo.getActivaCatalogo())
                || catalogo.getAntiguedad().getEstadoActualAntiguedad() != EstadoAntiguedad.LINK_PAGO_ENVIADO
                || pago.getReferenciaStripeTransaccion() != null
                || catalogo.getIdLinkStripeCatalogo() != null) {
            throw new SolicitudInvalidaException("La reserva cambió antes de vincular el Checkout");
        }

        pago.setReferenciaStripeTransaccion(checkout.referenciaStripe());
        catalogo.setIdLinkStripeCatalogo(checkout.referenciaStripe());
        catalogo.setUrlLinkPagoCatalogo(checkout.urlPagoStripe());
        transacciones.save(pago);
        catalogos.save(catalogo);
    }
}
