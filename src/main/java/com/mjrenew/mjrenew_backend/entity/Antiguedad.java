package com.mjrenew.mjrenew_backend.entity;

import com.mjrenew.mjrenew_backend.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.enums.EstiloMueble;
import com.mjrenew.mjrenew_backend.enums.MaterialMueble;
import com.mjrenew.mjrenew_backend.enums.TipoMueble;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "antiguedades")
public class Antiguedad {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "antiguedades_id")
    private UUID antiguedadesId;

    @ManyToOne
    @JoinColumn(name = "propietario_id", nullable = false)
    private Usuario propietario;

    @ManyToOne
    @JoinColumn(name = "restaurador_id")
    private Usuario restaurador;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_mueble", nullable = false)
    private TipoMueble tipoMueble;

    @Enumerated(EnumType.STRING)
    @Column(name = "estilo_mueble", nullable = false)
    private EstiloMueble estiloMueble;

    @Enumerated(EnumType.STRING)
    @Column(name = "material_mueble", nullable = false)
    private MaterialMueble materialMueble;

    @Column(name = "descripcion_danios_antiguedad", nullable = false)
    private String descripcionDaniosAntiguedad;

    @Column(name = "procedencia_mueble", length = 300)
    private String procedenciaMueble;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_actual_antiguedad", nullable = false)
    private EstadoAntiguedad estadoActualAntiguedad;

    @Column(name = "registrada_en_antiguedad", nullable = false)
    private OffsetDateTime registradaEnAntiguedad;

    @Column(name = "restauracion_inicio_antiguedad")
    private OffsetDateTime restauracionInicioAntiguedad;

    @Column(name = "restauracion_fin_antiguedad")
    private OffsetDateTime restauracionFinAntiguedad;

    public UUID getAntiguedadesId() {
        return antiguedadesId;
    }

    public void setAntiguedadesId(UUID antiguedadesId) {
        this.antiguedadesId = antiguedadesId;
    }

    public Usuario getPropietario() {
        return propietario;
    }

    public void setPropietario(Usuario propietario) {
        this.propietario = propietario;
    }

    public Usuario getRestaurador() {
        return restaurador;
    }

    public void setRestaurador(Usuario restaurador) {
        this.restaurador = restaurador;
    }

    public TipoMueble getTipoMueble() {
        return tipoMueble;
    }

    public void setTipoMueble(TipoMueble tipoMueble) {
        this.tipoMueble = tipoMueble;
    }

    public EstiloMueble getEstiloMueble() {
        return estiloMueble;
    }

    public void setEstiloMueble(EstiloMueble estiloMueble) {
        this.estiloMueble = estiloMueble;
    }

    public MaterialMueble getMaterialMueble() {
        return materialMueble;
    }

    public void setMaterialMueble(MaterialMueble materialMueble) {
        this.materialMueble = materialMueble;
    }

    public String getDescripcionDaniosAntiguedad() {
        return descripcionDaniosAntiguedad;
    }

    public void setDescripcionDaniosAntiguedad(String descripcionDaniosAntiguedad) {
        this.descripcionDaniosAntiguedad = descripcionDaniosAntiguedad;
    }

    public String getProcedenciaMueble() {
        return procedenciaMueble;
    }

    public void setProcedenciaMueble(String procedenciaMueble) {
        this.procedenciaMueble = procedenciaMueble;
    }

    public EstadoAntiguedad getEstadoActualAntiguedad() {
        return estadoActualAntiguedad;
    }

    public void setEstadoActualAntiguedad(EstadoAntiguedad estadoActualAntiguedad) {
        this.estadoActualAntiguedad = estadoActualAntiguedad;
    }

    public OffsetDateTime getRegistradaEnAntiguedad() {
        return registradaEnAntiguedad;
    }

    public void setRegistradaEnAntiguedad(OffsetDateTime registradaEnAntiguedad) {
        this.registradaEnAntiguedad = registradaEnAntiguedad;
    }

    public OffsetDateTime getRestauracionInicioAntiguedad() {
        return restauracionInicioAntiguedad;
    }

    public void setRestauracionInicioAntiguedad(OffsetDateTime restauracionInicioAntiguedad) {
        this.restauracionInicioAntiguedad = restauracionInicioAntiguedad;
    }

    public OffsetDateTime getRestauracionFinAntiguedad() {
        return restauracionFinAntiguedad;
    }

    public void setRestauracionFinAntiguedad(OffsetDateTime restauracionFinAntiguedad) {
        this.restauracionFinAntiguedad = restauracionFinAntiguedad;
    }
}