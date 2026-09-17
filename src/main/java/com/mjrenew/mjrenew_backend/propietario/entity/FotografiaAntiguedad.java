package com.mjrenew.mjrenew_backend.propietario.entity;

import com.mjrenew.mjrenew_backend.restaurador.entity.AvanceRestauracion;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoFotografia;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fotografias_antiguedad")
public class FotografiaAntiguedad {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "fotografias_antiguedad_id")
    private UUID fotografiasAntiguedadId;

    @ManyToOne
    @JoinColumn(name = "antiguedad_id", nullable = false)
    private Antiguedad antiguedad;

    @ManyToOne
    @JoinColumn(name = "avance_id")
    private AvanceRestauracion avance;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_fotografia", nullable = false)
    private TipoFotografia tipoFotografia;

    @Column(name = "url_almacen_fotografia", nullable = false, length = 300)
    private String urlAlmacenFotografia;

    @Column(name = "subida_en_fotografia", nullable = false)
    private OffsetDateTime subidaEnFotografia;

    public UUID getFotografiasAntiguedadId() {
        return fotografiasAntiguedadId;
    }

    public void setFotografiasAntiguedadId(UUID fotografiasAntiguedadId) {
        this.fotografiasAntiguedadId = fotografiasAntiguedadId;
    }

    public Antiguedad getAntiguedad() {
        return antiguedad;
    }

    public void setAntiguedad(Antiguedad antiguedad) {
        this.antiguedad = antiguedad;
    }

    public AvanceRestauracion getAvance() {
        return avance;
    }

    public void setAvance(AvanceRestauracion avance) {
        this.avance = avance;
    }

    public TipoFotografia getTipoFotografia() {
        return tipoFotografia;
    }

    public void setTipoFotografia(TipoFotografia tipoFotografia) {
        this.tipoFotografia = tipoFotografia;
    }

    public String getUrlAlmacenFotografia() {
        return urlAlmacenFotografia;
    }

    public void setUrlAlmacenFotografia(String urlAlmacenFotografia) {
        this.urlAlmacenFotografia = urlAlmacenFotografia;
    }

    public OffsetDateTime getSubidaEnFotografia() {
        return subidaEnFotografia;
    }

    public void setSubidaEnFotografia(OffsetDateTime subidaEnFotografia) {
        this.subidaEnFotografia = subidaEnFotografia;
    }
}