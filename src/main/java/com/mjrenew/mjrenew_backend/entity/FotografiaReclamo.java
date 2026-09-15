package com.mjrenew.mjrenew_backend.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fotografias_reclamo")
public class FotografiaReclamo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "fotografias_reclamo_id")
    private UUID fotografiasReclamoId;

    @ManyToOne
    @JoinColumn(name = "disputas_antiguedad_id", nullable = false)
    private DisputaAntiguedad disputaAntiguedad;

    @ManyToOne
    @JoinColumn(name = "antiguedad_id", nullable = false)
    private Antiguedad antiguedad;

    @Column(name = "descripcion_reclamo", nullable = false)
    private String descripcionReclamo;

    @Column(name = "url_almacen_reclamo", nullable = false, length = 300)
    private String urlAlmacenReclamo;

    @Column(name = "subida_en_reclamo", nullable = false)
    private OffsetDateTime subidaEnReclamo;


    public UUID getFotografiasReclamoId() {
        return fotografiasReclamoId;
    }

    public void setFotografiasReclamoId(UUID fotografiasReclamoId) {
        this.fotografiasReclamoId = fotografiasReclamoId;
    }

    public DisputaAntiguedad getDisputaAntiguedad() {
        return disputaAntiguedad;
    }

    public void setDisputaAntiguedad(DisputaAntiguedad disputaAntiguedad) {
        this.disputaAntiguedad = disputaAntiguedad;
    }

    public Antiguedad getAntiguedad() {
        return antiguedad;
    }

    public void setAntiguedad(Antiguedad antiguedad) {
        this.antiguedad = antiguedad;
    }

    public String getDescripcionReclamo() {
        return descripcionReclamo;
    }

    public void setDescripcionReclamo(String descripcionReclamo) {
        this.descripcionReclamo = descripcionReclamo;
    }

    public String getUrlAlmacenReclamo() {
        return urlAlmacenReclamo;
    }

    public void setUrlAlmacenReclamo(String urlAlmacenReclamo) {
        this.urlAlmacenReclamo = urlAlmacenReclamo;
    }

    public OffsetDateTime getSubidaEnReclamo() {
        return subidaEnReclamo;
    }

    public void setSubidaEnReclamo(OffsetDateTime subidaEnReclamo) {
        this.subidaEnReclamo = subidaEnReclamo;
    }
}