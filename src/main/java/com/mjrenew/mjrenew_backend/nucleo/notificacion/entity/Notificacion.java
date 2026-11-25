package com.mjrenew.mjrenew_backend.nucleo.notificacion.entity;

import com.mjrenew.mjrenew_backend.nucleo.notificacion.*;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notificaciones")
public class Notificacion {
    @Id @Column(name = "notificaciones_id") private UUID id;
    @Column(name = "destinatario_id", nullable = false) private UUID destinatarioId;
    @Column(name = "antiguedad_id", nullable = false) private UUID antiguedadId;
    @Column(name = "referencia_id", nullable = false) private UUID referenciaId;
    @Enumerated(EnumType.STRING) @Column(name = "tipo", nullable = false, length = 40) private TipoNotificacion tipo;
    @Column(name = "titulo", nullable = false, length = 160) private String titulo;
    @Column(name = "mensaje", nullable = false, length = 500) private String mensaje;
    @Column(name = "creado_en", nullable = false) private OffsetDateTime creadoEn;
    @Column(name = "leido_en") private OffsetDateTime leidoEn;
    @Enumerated(EnumType.STRING) @Column(name = "estado_correo", nullable = false, length = 20)
    private EstadoCorreoNotificacion estadoCorreo;
    @Column(name = "intentos_correo", nullable = false) private int intentosCorreo;
    @Column(name = "proximo_intento_en", nullable = false) private OffsetDateTime proximoIntentoEn;
    @Column(name = "reserva_id") private UUID reservaId;
    @Column(name = "reservado_hasta") private OffsetDateTime reservadoHasta;
    @Column(name = "correo_enviado_en") private OffsetDateTime correoEnviadoEn;

    public UUID getId() { return id; }
    public UUID getDestinatarioId() { return destinatarioId; }
    public UUID getAntiguedadId() { return antiguedadId; }
    public UUID getReferenciaId() { return referenciaId; }
    public TipoNotificacion getTipo() { return tipo; }
    public String getTitulo() { return titulo; }
    public String getMensaje() { return mensaje; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public OffsetDateTime getLeidoEn() { return leidoEn; }
    public void setLeidoEn(OffsetDateTime leidoEn) { this.leidoEn = leidoEn; }
    public EstadoCorreoNotificacion getEstadoCorreo() { return estadoCorreo; }
    public void setEstadoCorreo(EstadoCorreoNotificacion estadoCorreo) { this.estadoCorreo = estadoCorreo; }
    public int getIntentosCorreo() { return intentosCorreo; }
    public void setIntentosCorreo(int intentosCorreo) { this.intentosCorreo = intentosCorreo; }
    public OffsetDateTime getProximoIntentoEn() { return proximoIntentoEn; }
    public void setProximoIntentoEn(OffsetDateTime proximoIntentoEn) { this.proximoIntentoEn = proximoIntentoEn; }
    public UUID getReservaId() { return reservaId; }
    public void setReservaId(UUID reservaId) { this.reservaId = reservaId; }
    public OffsetDateTime getReservadoHasta() { return reservadoHasta; }
    public void setReservadoHasta(OffsetDateTime reservadoHasta) { this.reservadoHasta = reservadoHasta; }
    public OffsetDateTime getCorreoEnviadoEn() { return correoEnviadoEn; }
    public void setCorreoEnviadoEn(OffsetDateTime correoEnviadoEn) { this.correoEnviadoEn = correoEnviadoEn; }
}
