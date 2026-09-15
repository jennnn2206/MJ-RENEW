package com.mjrenew.mjrenew_backend.entity;

import com.mjrenew.mjrenew_backend.enums.TipoUsuario;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "usuarios_id")
    private UUID usuariosId;

    @Column(name = "nombre_completo_usuario", nullable = false, length = 100)
    private String nombreCompletoUsuario;

    @Column(name = "correo_electronico_usuario", nullable = false, unique = true, length = 150)
    private String correoElectronicoUsuario;

    @Column(name = "contrasena_hash_usuario", nullable = false, length = 72)
    private String contrasenaHashUsuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_usuario", nullable = false)
    private TipoUsuario tipoUsuario;

    @Column(name = "telefono_usuario", length = 15)
    private String telefonoUsuario;

    @Column(name = "direccion_texto_usuario", length = 300)
    private String direccionTextoUsuario;

    @Column(name = "latitud_usuario", precision = 9, scale = 6)
    private BigDecimal latitudUsuario;

    @Column(name = "longitud_usuario", precision = 9, scale = 6)
    private BigDecimal longitudUsuario;

    @Column(name = "correo_verificado_usuario", nullable = false)
    private Boolean correoVerificadoUsuario;

    @Column(name = "activo_usuario", nullable = false)
    private Boolean activoUsuario;

    @Column(name = "registrado_en_usuario", nullable = false)
    private OffsetDateTime registradoEnUsuario;

    public UUID getUsuariosId() {
        return usuariosId;
    }

    public void setUsuariosId(UUID usuariosId) {
        this.usuariosId = usuariosId;
    }

    public String getNombreCompletoUsuario() {
        return nombreCompletoUsuario;
    }

    public void setNombreCompletoUsuario(String nombreCompletoUsuario) {
        this.nombreCompletoUsuario = nombreCompletoUsuario;
    }

    public String getCorreoElectronicoUsuario() {
        return correoElectronicoUsuario;
    }

    public void setCorreoElectronicoUsuario(String correoElectronicoUsuario) {
        this.correoElectronicoUsuario = correoElectronicoUsuario;
    }

    public String getContrasenaHashUsuario() {
        return contrasenaHashUsuario;
    }

    public void setContrasenaHashUsuario(String contrasenaHashUsuario) {
        this.contrasenaHashUsuario = contrasenaHashUsuario;
    }

    public TipoUsuario getTipoUsuario() {
        return tipoUsuario;
    }

    public void setTipoUsuario(TipoUsuario tipoUsuario) {
        this.tipoUsuario = tipoUsuario;
    }

    public String getTelefonoUsuario() {
        return telefonoUsuario;
    }

    public void setTelefonoUsuario(String telefonoUsuario) {
        this.telefonoUsuario = telefonoUsuario;
    }

    public String getDireccionTextoUsuario() {
        return direccionTextoUsuario;
    }

    public void setDireccionTextoUsuario(String direccionTextoUsuario) {
        this.direccionTextoUsuario = direccionTextoUsuario;
    }

    public BigDecimal getLatitudUsuario() {
        return latitudUsuario;
    }

    public void setLatitudUsuario(BigDecimal latitudUsuario) {
        this.latitudUsuario = latitudUsuario;
    }

    public BigDecimal getLongitudUsuario() {
        return longitudUsuario;
    }

    public void setLongitudUsuario(BigDecimal longitudUsuario) {
        this.longitudUsuario = longitudUsuario;
    }

    public Boolean getCorreoVerificadoUsuario() {
        return correoVerificadoUsuario;
    }

    public void setCorreoVerificadoUsuario(Boolean correoVerificadoUsuario) {
        this.correoVerificadoUsuario = correoVerificadoUsuario;
    }

    public Boolean getActivoUsuario() {
        return activoUsuario;
    }

    public void setActivoUsuario(Boolean activoUsuario) {
        this.activoUsuario = activoUsuario;
    }

    public OffsetDateTime getRegistradoEnUsuario() {
        return registradoEnUsuario;
    }

    public void setRegistradoEnUsuario(OffsetDateTime registradoEnUsuario) {
        this.registradoEnUsuario = registradoEnUsuario;
    }
}