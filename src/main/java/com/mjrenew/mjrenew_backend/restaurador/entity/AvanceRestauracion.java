package com.mjrenew.mjrenew_backend.restaurador.entity;

import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "avances_restauracion")
public class AvanceRestauracion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "avances_restauracion_id")
    private UUID avancesRestauracionId;

    @ManyToOne
    @JoinColumn(name = "antiguedad_id", nullable = false)
    private Antiguedad antiguedad;

    @ManyToOne
    @JoinColumn(name = "restaurador_id", nullable = false)
    private Usuario restaurador;

    @Column(name = "descripcion_avance", nullable = false, length = 300)
    private String descripcionAvance;

    @Column(name = "publicado_en_avance", nullable = false)
    private OffsetDateTime publicadoEnAvance;

    public UUID getAvancesRestauracionId() {
        return avancesRestauracionId;
    }

    public void setAvancesRestauracionId(UUID avancesRestauracionId) {
        this.avancesRestauracionId = avancesRestauracionId;
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

    public String getDescripcionAvance() {
        return descripcionAvance;
    }

    public void setDescripcionAvance(String descripcionAvance) {
        this.descripcionAvance = descripcionAvance;
    }

    public OffsetDateTime getPublicadoEnAvance() {
        return publicadoEnAvance;
    }

    public void setPublicadoEnAvance(OffsetDateTime publicadoEnAvance) {
        this.publicadoEnAvance = publicadoEnAvance;
    }
}