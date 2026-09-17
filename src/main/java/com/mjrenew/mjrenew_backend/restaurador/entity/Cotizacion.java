package com.mjrenew.mjrenew_backend.restaurador.entity;

import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoCotizacion;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cotizaciones")
public class Cotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "cotizaciones_id")
    private UUID cotizacionesId;

    @ManyToOne
    @JoinColumn(name = "antiguedad_id", nullable = false)
    private Antiguedad antiguedad;

    @ManyToOne
    @JoinColumn(name = "restaurador_id", nullable = false)
    private Usuario restaurador;

    @Column(name = "costo_minimo_mxn_cotizacion", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoMinimoMxnCotizacion;

    @Column(name = "costo_maximo_mxn_cotizacion", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoMaximoMxnCotizacion;

    @Column(name = "tiempo_semanas_cotizacion", nullable = false)
    private Short tiempoSemanasCotizacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_cotizacion", nullable = false)
    private EstadoCotizacion estadoCotizacion;

    @Column(name = "motivo_rechazo_cotizacion")
    private String motivoRechazoCotizacion;

    @Column(name = "enviada_en_cotizacion", nullable = false)
    private OffsetDateTime enviadaEnCotizacion;

    @Column(name = "respondida_en_cotizacion")
    private OffsetDateTime respondidaEnCotizacion;

    // Getters y setters

    public UUID getCotizacionesId() {
        return cotizacionesId;
    }

    public void setCotizacionesId(UUID cotizacionesId) {
        this.cotizacionesId = cotizacionesId;
    }

    public Antiguedad getAntiguedad() {
        return antiguedad;
    }

    public void setAntiguedad(Antiguedad antiguedad) {
        this.antiguedad = antiguedad;
    }

    public Usuario getRestaurador() {
        return restaurador;
    }

    public void setRestaurador(Usuario restaurador) {
        this.restaurador = restaurador;
    }

    public BigDecimal getCostoMinimoMxnCotizacion() {
        return costoMinimoMxnCotizacion;
    }

    public void setCostoMinimoMxnCotizacion(BigDecimal costoMinimoMxnCotizacion) {
        this.costoMinimoMxnCotizacion = costoMinimoMxnCotizacion;
    }

    public BigDecimal getCostoMaximoMxnCotizacion() {
        return costoMaximoMxnCotizacion;
    }

    public void setCostoMaximoMxnCotizacion(BigDecimal costoMaximoMxnCotizacion) {
        this.costoMaximoMxnCotizacion = costoMaximoMxnCotizacion;
    }

    public Short getTiempoSemanasCotizacion() {
        return tiempoSemanasCotizacion;
    }

    public void setTiempoSemanasCotizacion(Short tiempoSemanasCotizacion) {
        this.tiempoSemanasCotizacion = tiempoSemanasCotizacion;
    }

    public EstadoCotizacion getEstadoCotizacion() {
        return estadoCotizacion;
    }

    public void setEstadoCotizacion(EstadoCotizacion estadoCotizacion) {
        this.estadoCotizacion = estadoCotizacion;
    }

    public String getMotivoRechazoCotizacion() {
        return motivoRechazoCotizacion;
    }

    public void setMotivoRechazoCotizacion(String motivoRechazoCotizacion) {
        this.motivoRechazoCotizacion = motivoRechazoCotizacion;
    }

    public OffsetDateTime getEnviadaEnCotizacion() {
        return enviadaEnCotizacion;
    }

    public void setEnviadaEnCotizacion(OffsetDateTime enviadaEnCotizacion) {
        this.enviadaEnCotizacion = enviadaEnCotizacion;
    }

    public OffsetDateTime getRespondidaEnCotizacion() {
        return respondidaEnCotizacion;
    }

    public void setRespondidaEnCotizacion(OffsetDateTime respondidaEnCotizacion) {
        this.respondidaEnCotizacion = respondidaEnCotizacion;
    }
}