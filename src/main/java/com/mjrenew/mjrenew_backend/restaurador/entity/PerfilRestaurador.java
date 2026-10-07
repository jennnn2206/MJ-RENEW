package com.mjrenew.mjrenew_backend.restaurador.entity;

import com.mjrenew.mjrenew_backend.nucleo.enums.DisponibilidadRestaurador;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "perfiles_restaurador")
public class PerfilRestaurador {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "perfiles_restaurador_id")
    private UUID perfilesRestauradorId;

    @OneToOne
    @JoinColumn(name = "restaurador_id", nullable = false, unique = true)
    private Usuario restaurador;

    @Column(name = "especialidad_restaurador", length = 150, nullable = false)
    private String especialidadRestaurador;

    @Column(name = "anos_experiencia_restaurador", nullable = false)
    private Short anosExperienciaRestaurador;

    @Column(name = "descripcion_bio_restaurador")
    private String descripcionBioRestaurador;

    @Enumerated(EnumType.STRING)
    @Column(name = "disponibilidad_restaurador", nullable = false)
    private DisponibilidadRestaurador disponibilidadRestaurador;

    @Column(name = "aprobado_por_admin_restaurador", nullable = false)
    private Boolean aprobadoPorAdminRestaurador;

    @Column(name = "actualizado_en_restaurador", nullable = false)
    private OffsetDateTime actualizadoEnRestaurador;

    public UUID getPerfilesRestauradorId() {
        return perfilesRestauradorId;
    }

    public void setPerfilesRestauradorId(UUID perfilesRestauradorId) {
        this.perfilesRestauradorId = perfilesRestauradorId;
    }

    public Usuario getRestaurador() {
        return restaurador;
    }

    public void setRestaurador(Usuario restaurador) {
        this.restaurador = restaurador;
    }

    public String getEspecialidadRestaurador() {
        return especialidadRestaurador;
    }

    public void setEspecialidadRestaurador(String especialidadRestaurador) {
        this.especialidadRestaurador = especialidadRestaurador;
    }

    public Short getAnosExperienciaRestaurador() {
        return anosExperienciaRestaurador;
    }

    public void setAnosExperienciaRestaurador(Short anosExperienciaRestaurador) {
        this.anosExperienciaRestaurador = anosExperienciaRestaurador;
    }

    public String getDescripcionBioRestaurador() {
        return descripcionBioRestaurador;
    }

    public void setDescripcionBioRestaurador(String descripcionBioRestaurador) {
        this.descripcionBioRestaurador = descripcionBioRestaurador;
    }

    public DisponibilidadRestaurador getDisponibilidadRestaurador() {
        return disponibilidadRestaurador;
    }

    public void setDisponibilidadRestaurador(DisponibilidadRestaurador disponibilidadRestaurador) {
        this.disponibilidadRestaurador = disponibilidadRestaurador;
    }

    public Boolean getAprobadoPorAdminRestaurador() {
        return aprobadoPorAdminRestaurador;
    }

    public void setAprobadoPorAdminRestaurador(Boolean aprobadoPorAdminRestaurador) {
        this.aprobadoPorAdminRestaurador = aprobadoPorAdminRestaurador;
    }

    public OffsetDateTime getActualizadoEnRestaurador() {
        return actualizadoEnRestaurador;
    }

    public void setActualizadoEnRestaurador(OffsetDateTime actualizadoEnRestaurador) {
        this.actualizadoEnRestaurador = actualizadoEnRestaurador;
    }
}