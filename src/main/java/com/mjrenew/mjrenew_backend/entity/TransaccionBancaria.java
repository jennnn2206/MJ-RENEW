package com.mjrenew.mjrenew_backend.entity;

import com.mjrenew.mjrenew_backend.enums.EstadoTransaccion;
import com.mjrenew.mjrenew_backend.enums.FlujoTransaccion;
import com.mjrenew.mjrenew_backend.enums.TipoTransaccion;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transacciones_bancarias")
public class TransaccionBancaria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaccion_id")
    private UUID transaccionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_transaccion", nullable = false)
    private TipoTransaccion tipoTransaccion;

    @Enumerated(EnumType.STRING)
    @Column(name = "flujo_mjrenew", nullable = false)
    private FlujoTransaccion flujoMjrenew;

    @ManyToOne
    @JoinColumn(name = "originator_id", nullable = false)
    private Usuario originator;

    @ManyToOne
    @JoinColumn(name = "beneficiario_id", nullable = false)
    private Usuario beneficiario;

    @ManyToOne
    @JoinColumn(name = "antiguedad_id", nullable = false)
    private Antiguedad antiguedad;

    @ManyToOne
    @JoinColumn(name = "catalogo_antiguedad_id")
    private CatalogoAntiguedad catalogoAntiguedad;

    @ManyToOne
    @JoinColumn(name = "transaccion_padre_id")
    private TransaccionBancaria transaccionPadre;

    @Column(name = "monto_bruto_mxn_transaccion", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoBrutoMxnTransaccion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_transaccion", nullable = false)
    private EstadoTransaccion estadoTransaccion;

    @Column(name = "referencia_stripe_transaccion", length = 100)
    private String referenciaStripeTransaccion;

    @Column(name = "creada_en_transaccion", nullable = false)
    private OffsetDateTime creadaEnTransaccion;

    @Column(name = "procesada_en_transaccion")
    private OffsetDateTime procesadaEnTransaccion;

    public UUID getTransaccionId() {
        return transaccionId;
    }

    public void setTransaccionId(UUID transaccionId) {
        this.transaccionId = transaccionId;
    }

    public TipoTransaccion getTipoTransaccion() {
        return tipoTransaccion;
    }

    public void setTipoTransaccion(TipoTransaccion tipoTransaccion) {
        this.tipoTransaccion = tipoTransaccion;
    }

    public FlujoTransaccion getFlujoMjrenew() {
        return flujoMjrenew;
    }

    public void setFlujoMjrenew(FlujoTransaccion flujoMjrenew) {
        this.flujoMjrenew = flujoMjrenew;
    }

    public Usuario getOriginator() {
        return originator;
    }

    public void setOriginator(Usuario originator) {
        this.originator = originator;
    }

    public Usuario getBeneficiario() {
        return beneficiario;
    }

    public void setBeneficiario(Usuario beneficiario) {
        this.beneficiario = beneficiario;
    }

    public Antiguedad getAntiguedad() {
        return antiguedad;
    }

    public void setAntiguedad(Antiguedad antiguedad) {
        this.antiguedad = antiguedad;
    }

    public CatalogoAntiguedad getCatalogoAntiguedad() {
        return catalogoAntiguedad;
    }

    public void setCatalogoAntiguedad(CatalogoAntiguedad catalogoAntiguedad) {
        this.catalogoAntiguedad = catalogoAntiguedad;
    }

    public TransaccionBancaria getTransaccionPadre() {
        return transaccionPadre;
    }

    public void setTransaccionPadre(TransaccionBancaria transaccionPadre) {
        this.transaccionPadre = transaccionPadre;
    }

    public BigDecimal getMontoBrutoMxnTransaccion() {
        return montoBrutoMxnTransaccion;
    }

    public void setMontoBrutoMxnTransaccion(BigDecimal montoBrutoMxnTransaccion) {
        this.montoBrutoMxnTransaccion = montoBrutoMxnTransaccion;
    }

    public EstadoTransaccion getEstadoTransaccion() {
        return estadoTransaccion;
    }

    public void setEstadoTransaccion(EstadoTransaccion estadoTransaccion) {
        this.estadoTransaccion = estadoTransaccion;
    }

    public String getReferenciaStripeTransaccion() {
        return referenciaStripeTransaccion;
    }

    public void setReferenciaStripeTransaccion(String referenciaStripeTransaccion) {
        this.referenciaStripeTransaccion = referenciaStripeTransaccion;
    }

    public OffsetDateTime getCreadaEnTransaccion() {
        return creadaEnTransaccion;
    }

    public void setCreadaEnTransaccion(OffsetDateTime creadaEnTransaccion) {
        this.creadaEnTransaccion = creadaEnTransaccion;
    }

    public OffsetDateTime getProcesadaEnTransaccion() {
        return procesadaEnTransaccion;
    }

    public void setProcesadaEnTransaccion(OffsetDateTime procesadaEnTransaccion) {
        this.procesadaEnTransaccion = procesadaEnTransaccion;
    }
}