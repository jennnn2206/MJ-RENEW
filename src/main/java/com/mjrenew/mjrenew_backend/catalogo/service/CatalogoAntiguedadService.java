package com.mjrenew.mjrenew_backend.catalogo.service;

import com.mjrenew.mjrenew_backend.catalogo.dto.CatalogoAntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.catalogo.dto.CatalogoAntiguedadResumenResponse;
import com.mjrenew.mjrenew_backend.catalogo.dto.UrlPagoStripeResponse;
import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import com.mjrenew.mjrenew_backend.catalogo.mapper.CatalogoAntiguedadMapper;
import com.mjrenew.mjrenew_backend.catalogo.repository.CatalogoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.*;
import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.exception.TransicionEstadoInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.propietario.dto.DimensionResponse;
import com.mjrenew.mjrenew_backend.propietario.repository.DimensionRepository;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.transaccion.entity.TransaccionBancaria;
import com.mjrenew.mjrenew_backend.transaccion.repository.TransaccionBancariaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class CatalogoAntiguedadService {

    private final CatalogoAntiguedadRepository catalogoRepository;
    private final FotografiaAntiguedadRepository fotografiaRepository;
    private final DimensionRepository dimensionRepository;
    private final TransaccionBancariaRepository transaccionRepository;
    private final CatalogoAntiguedadMapper catalogoMapper;

    public CatalogoAntiguedadService(CatalogoAntiguedadRepository catalogoRepository,
                                     FotografiaAntiguedadRepository fotografiaRepository,
                                     DimensionRepository dimensionRepository,
                                     TransaccionBancariaRepository transaccionRepository,
                                     CatalogoAntiguedadMapper catalogoMapper) {
        this.catalogoRepository = catalogoRepository;
        this.fotografiaRepository = fotografiaRepository;
        this.dimensionRepository = dimensionRepository;
        this.transaccionRepository = transaccionRepository;
        this.catalogoMapper = catalogoMapper;
    }

    public Page<CatalogoAntiguedadResumenResponse> explorarCatalogo(Pageable pageable) {
        return catalogoRepository.findByActivaCatalogoTrue(pageable)
                .map(catalogo -> catalogoMapper.toResumen(catalogo, obtenerUrlFotoPortada(catalogo.getAntiguedad())));
    }

    public CatalogoAntiguedadDetalleResponse obtenerDetalle(UUID catalogoId) {
        CatalogoAntiguedad catalogo = buscarOFallar(catalogoId);
        String urlFotoPortada = obtenerUrlFotoPortada(catalogo.getAntiguedad());
        DimensionResponse dimension = dimensionRepository
                .findByAntiguedad_AntiguedadesId(catalogo.getAntiguedad().getAntiguedadesId())
                .map(dim -> new DimensionResponse(
                        dim.getAltoCmAntiguedad(), dim.getAnchoCmAntiguedad(),
                        dim.getProfundidadCmAntiguedad(), dim.getPesoKgAntiguedad()))
                .orElse(null);
        return catalogoMapper.toDetalle(catalogo, urlFotoPortada, dimension);
    }

    @Transactional
    public UrlPagoStripeResponse iniciarCompra(UUID catalogoId, Usuario comprador) {
        CatalogoAntiguedad catalogo = buscarOFallar(catalogoId);
        Antiguedad antiguedad = catalogo.getAntiguedad();

        if (!Boolean.TRUE.equals(catalogo.getActivaCatalogo())) {
            throw new SolicitudInvalidaException("Esta pieza ya no está disponible en el catálogo");
        }
        if (antiguedad.getEstadoActualAntiguedad() != EstadoAntiguedad.EN_CATALOGO) {
            throw new TransicionEstadoInvalidaException(
                    "La antigüedad debe estar en EN_CATALOGO para iniciar una compra, y está en " + antiguedad.getEstadoActualAntiguedad()
            );
        }
        if (antiguedad.getPropietario().getUsuariosId().equals(comprador.getUsuariosId())) {
            throw new SolicitudInvalidaException("No puedes comprar tu propia pieza");
        }

        String idLinkStripe = "mock_" + UUID.randomUUID();
        String urlPago = "https://checkout.stripe.com/mock/" + idLinkStripe;

        catalogo.setComprador(comprador);
        catalogo.setIdLinkStripeCatalogo(idLinkStripe);
        catalogo.setUrlLinkPagoCatalogo(urlPago);
        catalogo.setActivaCatalogo(false);
        catalogoRepository.save(catalogo);

        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.LINK_PAGO_ENVIADO);

        TransaccionBancaria transaccion = new TransaccionBancaria();
        transaccion.setTipoTransaccion(TipoTransaccion.PAGO_VENTA);
        transaccion.setFlujoMjrenew(FlujoTransaccion.INGRESO);
        transaccion.setOriginator(comprador);
        transaccion.setBeneficiario(antiguedad.getPropietario());
        transaccion.setAntiguedad(antiguedad);
        transaccion.setCatalogoAntiguedad(catalogo);
        transaccion.setMontoBrutoMxnTransaccion(catalogo.getPrecioMxnCatalogo());
        transaccion.setEstadoTransaccion(EstadoTransaccion.PENDIENTE);
        transaccion.setReferenciaStripeTransaccion(idLinkStripe);
        transaccion.setCreadaEnTransaccion(OffsetDateTime.now());
        transaccionRepository.save(transaccion);

        return new UrlPagoStripeResponse(urlPago);
    }

    private String obtenerUrlFotoPortada(Antiguedad antiguedad) {
        return fotografiaRepository
                .findFirstByAntiguedad_AntiguedadesIdAndTipoFotografiaOrderBySubidaEnFotografiaAsc(
                        antiguedad.getAntiguedadesId(), TipoFotografia.ESTADO_INICIAL)
                .map(foto -> foto.getUrlAlmacenFotografia())
                .orElse(null);
    }

    private CatalogoAntiguedad buscarOFallar(UUID catalogoId) {
        return catalogoRepository.findById(catalogoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una pieza en catálogo con id " + catalogoId));
    }
}