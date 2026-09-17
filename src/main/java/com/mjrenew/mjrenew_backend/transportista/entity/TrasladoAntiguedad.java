package com.mjrenew.mjrenew_backend.transportista.entity;

import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.ResultadoTraslado;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTraslado;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "traslados_antiguedad")
public class TrasladoAntiguedad {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "traslados_antiguedad_id")
    private UUID trasladosAntiguedadId;

    @ManyToOne
    @JoinColumn(name = "antiguedad_id", nullable = false)
    private Antiguedad antiguedad;

    @ManyToOne
    @JoinColumn(name = "transportista_id")
    private Usuario transportista;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_traslado", nullable = false)
    private TipoTraslado tipoTraslado;

    @Column(name = "direccion_origen_traslado", nullable = false, length = 300)
    private String direccionOrigenTraslado;

    @Column(name = "direccion_destino_traslado", nullable = false, length = 300)
    private String direccionDestinoTraslado;

    @Column(name = "fecha_acordada_traslado", nullable = false)
    private LocalDate fechaAcordadaTraslado;

    @Column(name = "hora_inicio_acordada_traslado", nullable = false)
    private LocalTime horaInicioAcordadaTraslado;

    @Column(name = "hora_fin_acordada_traslado", nullable = false)
    private LocalTime horaFinAcordadaTraslado;

    @Column(name = "numero_intento_traslado", nullable = false)
    private Short numeroIntentoTraslado;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado_traslado", nullable = false)
    private ResultadoTraslado resultadoTraslado;

    @Column(name = "costo_estimado_mxn_traslado", precision = 10, scale = 2)
    private BigDecimal costoEstimadoMxnTraslado;

    @Column(name = "motivo_fallo_traslado")
    private String motivoFalloTraslado;

    @Column(name = "agendado_en_traslado", nullable = false)
    private OffsetDateTime agendadoEnTraslado;

    @Column(name = "ejecutado_en_traslado")
    private OffsetDateTime ejecutadoEnTraslado;


    public UUID getTrasladosAntiguedadId() {
        return trasladosAntiguedadId;
    }

    public void setTrasladosAntiguedadId(UUID trasladosAntiguedadId) {
        this.trasladosAntiguedadId = trasladosAntiguedadId;
    }

    public Antiguedad getAntiguedad() {
        return antiguedad;
    }

    public void setAntiguedad(Antiguedad antiguedad) {
        this.antiguedad = antiguedad;
    }

    public Usuario getTransportista() {
        return transportista;
    }

    public void setTransportista(Usuario transportista) {
        this.transportista = transportista;
    }

    public TipoTraslado getTipoTraslado() {
        return tipoTraslado;
    }

    public void setTipoTraslado(TipoTraslado tipoTraslado) {
        this.tipoTraslado = tipoTraslado;
    }

    public String getDireccionOrigenTraslado() {
        return direccionOrigenTraslado;
    }

    public void setDireccionOrigenTraslado(String direccionOrigenTraslado) {
        this.direccionOrigenTraslado = direccionOrigenTraslado;
    }

    public String getDireccionDestinoTraslado() {
        return direccionDestinoTraslado;
    }

    public void setDireccionDestinoTraslado(String direccionDestinoTraslado) {
        this.direccionDestinoTraslado = direccionDestinoTraslado;
    }

    public LocalDate getFechaAcordadaTraslado() {
        return fechaAcordadaTraslado;
    }

    public void setFechaAcordadaTraslado(LocalDate fechaAcordadaTraslado) {
        this.fechaAcordadaTraslado = fechaAcordadaTraslado;
    }

    public LocalTime getHoraInicioAcordadaTraslado() {
        return horaInicioAcordadaTraslado;
    }

    public void setHoraInicioAcordadaTraslado(LocalTime horaInicioAcordadaTraslado) {
        this.horaInicioAcordadaTraslado = horaInicioAcordadaTraslado;
    }

    public LocalTime getHoraFinAcordadaTraslado() {
        return horaFinAcordadaTraslado;
    }

    public void setHoraFinAcordadaTraslado(LocalTime horaFinAcordadaTraslado) {
        this.horaFinAcordadaTraslado = horaFinAcordadaTraslado;
    }

    public Short getNumeroIntentoTraslado() {
        return numeroIntentoTraslado;
    }

    public void setNumeroIntentoTraslado(Short numeroIntentoTraslado) {
        this.numeroIntentoTraslado = numeroIntentoTraslado;
    }

    public ResultadoTraslado getResultadoTraslado() {
        return resultadoTraslado;
    }

    public void setResultadoTraslado(ResultadoTraslado resultadoTraslado) {
        this.resultadoTraslado = resultadoTraslado;
    }

    public BigDecimal getCostoEstimadoMxnTraslado() {
        return costoEstimadoMxnTraslado;
    }

    public void setCostoEstimadoMxnTraslado(BigDecimal costoEstimadoMxnTraslado) {
        this.costoEstimadoMxnTraslado = costoEstimadoMxnTraslado;
    }

    public String getMotivoFalloTraslado() {
        return motivoFalloTraslado;
    }

    public void setMotivoFalloTraslado(String motivoFalloTraslado) {
        this.motivoFalloTraslado = motivoFalloTraslado;
    }

    public OffsetDateTime getAgendadoEnTraslado() {
        return agendadoEnTraslado;
    }

    public void setAgendadoEnTraslado(OffsetDateTime agendadoEnTraslado) {
        this.agendadoEnTraslado = agendadoEnTraslado;
    }

    public OffsetDateTime getEjecutadoEnTraslado() {
        return ejecutadoEnTraslado;
    }

    public void setEjecutadoEnTraslado(OffsetDateTime ejecutadoEnTraslado) {
        this.ejecutadoEnTraslado = ejecutadoEnTraslado;
    }
}