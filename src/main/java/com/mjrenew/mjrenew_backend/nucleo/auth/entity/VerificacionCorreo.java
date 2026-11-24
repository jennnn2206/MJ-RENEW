package com.mjrenew.mjrenew_backend.nucleo.auth.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "verificaciones_correo")
public class VerificacionCorreo {
    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "emitido_en", nullable = false)
    private OffsetDateTime emitidoEn;
    @Column(name = "expira_en", nullable = false)
    private OffsetDateTime expiraEn;

    public UUID getUsuarioId() { return usuarioId; }
    public void setUsuarioId(UUID usuarioId) { this.usuarioId = usuarioId; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public OffsetDateTime getEmitidoEn() { return emitidoEn; }
    public void setEmitidoEn(OffsetDateTime emitidoEn) { this.emitidoEn = emitidoEn; }
    public OffsetDateTime getExpiraEn() { return expiraEn; }
    public void setExpiraEn(OffsetDateTime expiraEn) { this.expiraEn = expiraEn; }
}
