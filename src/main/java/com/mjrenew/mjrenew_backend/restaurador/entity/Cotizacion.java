package com.mjrenew.mjrenew_backend.restaurador.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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

    @Column(name = "descripcion_proceso_cotizacion", nullable = false, length = 2000)
    private String descripcionProcesoCotizacion;

    @Column(name = "costo_minimo_mxn_cotizacion", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoMinimoMxnCotizacion;

    @Column(name = "costo_maximo_mxn_cotizacion", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoMaximoMxnCotizacion;

    @Column(name = "tiempo_semanas_cotizacion", nullable = false)
    private Integer tiempoSemanasCotizacion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado_cotizacion", nullable = false, columnDefinition = "estado_cotizacion")
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

    public Integer getTiempoSemanasCotizacion() {
        return tiempoSemanasCotizacion;
    }

    public void setTiempoSemanasCotizacion(Integer tiempoSemanasCotizacion) {
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

    public String getDescripcionProcesoCotizacion() {
        return descripcionProcesoCotizacion;
    }

    public void setDescripcionProcesoCotizacion(
            String descripcionProcesoCotizacion
    ) {
        this.descripcionProcesoCotizacion = descripcionProcesoCotizacion;
    }

}