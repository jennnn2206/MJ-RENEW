package com.mjrenew.mjrenew_backend.administrador.entity;

import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "disputas_antiguedad")
public class DisputaAntiguedad {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "disputas_antiguedad_id")
    private UUID disputasAntiguedadId;

    @ManyToOne
    @JoinColumn(name = "antiguedad_id", nullable = false)
    private Antiguedad antiguedad;

    @ManyToOne
    @JoinColumn(name = "abierta_por", nullable = false)
    private Usuario abiertaPor;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_ciclo_al_abrir_disputa", nullable = false)
    private EstadoAntiguedad estadoCicloAlAbrirDisputa;

    @Column(name = "descripcion_disputa", nullable = false)
    private String descripcionDisputa;

    @ManyToOne
    @JoinColumn(name = "admin_asignado")
    private Usuario adminAsignado;

    @Column(name = "resolucion_disputa")
    private String resolucionDisputa;

    @Column(name = "abierta_en_disputa", nullable = false)
    private OffsetDateTime abiertaEnDisputa;

    @Column(name = "resuelta_en_disputa")
    private OffsetDateTime resueltaEnDisputa;


    public UUID getDisputasAntiguedadId() {
        return disputasAntiguedadId;
    }

    public void setDisputasAntiguedadId(UUID disputasAntiguedadId) {
        this.disputasAntiguedadId = disputasAntiguedadId;
    }

    public Antiguedad getAntiguedad() {
        return antiguedad;
    }

    public void setAntiguedad(Antiguedad antiguedad) {
        this.antiguedad = antiguedad;
    }

    public Usuario getAbiertaPor() {
        return abiertaPor;
    }

    public void setAbiertaPor(Usuario abiertaPor) {
        this.abiertaPor = abiertaPor;
    }

    public EstadoAntiguedad getEstadoCicloAlAbrirDisputa() {
        return estadoCicloAlAbrirDisputa;
    }

    public void setEstadoCicloAlAbrirDisputa(EstadoAntiguedad estadoCicloAlAbrirDisputa) {
        this.estadoCicloAlAbrirDisputa = estadoCicloAlAbrirDisputa;
    }

    public String getDescripcionDisputa() {
        return descripcionDisputa;
    }

    public void setDescripcionDisputa(String descripcionDisputa) {
        this.descripcionDisputa = descripcionDisputa;
    }

    public Usuario getAdminAsignado() {
        return adminAsignado;
    }

    public void setAdminAsignado(Usuario adminAsignado) {
        this.adminAsignado = adminAsignado;
    }

    public String getResolucionDisputa() {
        return resolucionDisputa;
    }

    public void setResolucionDisputa(String resolucionDisputa) {
        this.resolucionDisputa = resolucionDisputa;
    }

    public OffsetDateTime getAbiertaEnDisputa() {
        return abiertaEnDisputa;
    }

    public void setAbiertaEnDisputa(OffsetDateTime abiertaEnDisputa) {
        this.abiertaEnDisputa = abiertaEnDisputa;
    }

    public OffsetDateTime getResueltaEnDisputa() {
        return resueltaEnDisputa;
    }

    public void setResueltaEnDisputa(OffsetDateTime resueltaEnDisputa) {
        this.resueltaEnDisputa = resueltaEnDisputa;
    }
}