package com.mjrenew.mjrenew_backend.catalogo.entity;

import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "catalogo_antiguedad")
public class CatalogoAntiguedad {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "catalogo_antiguedad_id")
    private UUID catalogoAntiguedadId;

    @OneToOne
    @JoinColumn(name = "antiguedad_id", nullable = false, unique = true)
    private Antiguedad antiguedad;

    @Column(name = "precio_mxn_catalogo", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioMxnCatalogo;

    @Column(name = "publicada_en_catalogo", nullable = false)
    private OffsetDateTime publicadaEnCatalogo;

    @Column(name = "expira_en_catalogo", nullable = false)
    private OffsetDateTime expiraEnCatalogo;

    @Column(name = "ultima_actualizacion_precio_catalogo")
    private OffsetDateTime ultimaActualizacionPrecioCatalogo;

    @Column(name = "activa_catalogo", nullable = false)
    private Boolean activaCatalogo;

    @ManyToOne
    @JoinColumn(name = "comprador_id")
    private Usuario comprador;

    @Column(name = "id_link_stripe_catalogo", length = 50)
    private String idLinkStripeCatalogo;

    @Column(name = "url_link_pago_catalogo", length = 300)
    private String urlLinkPagoCatalogo;

    @Column(name = "id_pago_stripe_catalogo", length = 50)
    private String idPagoStripeCatalogo;

    @Column(name = "completada_en_catalogo")
    private OffsetDateTime completadaEnCatalogo;

    public UUID getCatalogoAntiguedadId() {
        return catalogoAntiguedadId;
    }

    public void setCatalogoAntiguedadId(UUID catalogoAntiguedadId) {
        this.catalogoAntiguedadId = catalogoAntiguedadId;
    }

    public Antiguedad getAntiguedad() {
        return antiguedad;
    }

    public void setAntiguedad(Antiguedad antiguedad) {
        this.antiguedad = antiguedad;
    }

    public BigDecimal getPrecioMxnCatalogo() {
        return precioMxnCatalogo;
    }

    public void setPrecioMxnCatalogo(BigDecimal precioMxnCatalogo) {
        this.precioMxnCatalogo = precioMxnCatalogo;
    }

    public OffsetDateTime getPublicadaEnCatalogo() {
        return publicadaEnCatalogo;
    }

    public void setPublicadaEnCatalogo(OffsetDateTime publicadaEnCatalogo) {
        this.publicadaEnCatalogo = publicadaEnCatalogo;
    }

    public OffsetDateTime getExpiraEnCatalogo() {
        return expiraEnCatalogo;
    }

    public void setExpiraEnCatalogo(OffsetDateTime expiraEnCatalogo) {
        this.expiraEnCatalogo = expiraEnCatalogo;
    }

    public OffsetDateTime getUltimaActualizacionPrecioCatalogo() {
        return ultimaActualizacionPrecioCatalogo;
    }

    public void setUltimaActualizacionPrecioCatalogo(OffsetDateTime ultimaActualizacionPrecioCatalogo) {
        this.ultimaActualizacionPrecioCatalogo = ultimaActualizacionPrecioCatalogo;
    }

    public Boolean getActivaCatalogo() {
        return activaCatalogo;
    }

    public void setActivaCatalogo(Boolean activaCatalogo) {
        this.activaCatalogo = activaCatalogo;
    }

    public Usuario getComprador() {
        return comprador;
    }

    public void setComprador(Usuario comprador) {
        this.comprador = comprador;
    }

    public String getIdLinkStripeCatalogo() {
        return idLinkStripeCatalogo;
    }

    public void setIdLinkStripeCatalogo(String idLinkStripeCatalogo) {
        this.idLinkStripeCatalogo = idLinkStripeCatalogo;
    }

    public String getUrlLinkPagoCatalogo() {
        return urlLinkPagoCatalogo;
    }

    public void setUrlLinkPagoCatalogo(String urlLinkPagoCatalogo) {
        this.urlLinkPagoCatalogo = urlLinkPagoCatalogo;
    }

    public String getIdPagoStripeCatalogo() {
        return idPagoStripeCatalogo;
    }

    public void setIdPagoStripeCatalogo(String idPagoStripeCatalogo) {
        this.idPagoStripeCatalogo = idPagoStripeCatalogo;
    }

    public OffsetDateTime getCompletadaEnCatalogo() {
        return completadaEnCatalogo;
    }

    public void setCompletadaEnCatalogo(OffsetDateTime completadaEnCatalogo) {
        this.completadaEnCatalogo = completadaEnCatalogo;
    }
}