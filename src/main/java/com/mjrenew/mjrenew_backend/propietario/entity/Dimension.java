package com.mjrenew.mjrenew_backend.propietario.entity;

import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dimensiones")
public class Dimension {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "dimensiones_id")
    private UUID dimensionesId;

    @OneToOne
    @JoinColumn(name = "antiguedad_id", nullable = false, unique = true)
    private Antiguedad antiguedad;

    @Column(name = "alto_cm_antiguedad", precision = 6, scale = 2)
    private BigDecimal altoCmAntiguedad;

    @Column(name = "ancho_cm_antiguedad", precision = 6, scale = 2)
    private BigDecimal anchoCmAntiguedad;

    @Column(name = "profundidad_cm_antiguedad", precision = 6, scale = 2)
    private BigDecimal profundidadCmAntiguedad;

    @Column(name = "peso_kg_antiguedad", precision = 6, scale = 2)
    private BigDecimal pesoKgAntiguedad;

    @Column(name = "registradas_en_dimensiones", nullable = false)
    private OffsetDateTime registradasEnDimensiones;

    public UUID getDimensionesId() {
        return dimensionesId;
    }

    public void setDimensionesId(UUID dimensionesId) {
        this.dimensionesId = dimensionesId;
    }

    public Antiguedad getAntiguedad() {
        return antiguedad;
    }

    public void setAntiguedad(Antiguedad antiguedad) {
        this.antiguedad = antiguedad;
    }

    public BigDecimal getAltoCmAntiguedad() {
        return altoCmAntiguedad;
    }

    public void setAltoCmAntiguedad(BigDecimal altoCmAntiguedad) {
        this.altoCmAntiguedad = altoCmAntiguedad;
    }

    public BigDecimal getAnchoCmAntiguedad() {
        return anchoCmAntiguedad;
    }

    public void setAnchoCmAntiguedad(BigDecimal anchoCmAntiguedad) {
        this.anchoCmAntiguedad = anchoCmAntiguedad;
    }

    public BigDecimal getProfundidadCmAntiguedad() {
        return profundidadCmAntiguedad;
    }

    public void setProfundidadCmAntiguedad(BigDecimal profundidadCmAntiguedad) {
        this.profundidadCmAntiguedad = profundidadCmAntiguedad;
    }

    public BigDecimal getPesoKgAntiguedad() {
        return pesoKgAntiguedad;
    }

    public void setPesoKgAntiguedad(BigDecimal pesoKgAntiguedad) {
        this.pesoKgAntiguedad = pesoKgAntiguedad;
    }

    public OffsetDateTime getRegistradasEnDimensiones() {
        return registradasEnDimensiones;
    }

    public void setRegistradasEnDimensiones(OffsetDateTime registradasEnDimensiones) {
        this.registradasEnDimensiones = registradasEnDimensiones;
    }
}