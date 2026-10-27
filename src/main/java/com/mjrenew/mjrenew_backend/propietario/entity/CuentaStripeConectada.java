package com.mjrenew.mjrenew_backend.propietario.entity;

import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Relaciona un propietario de MJ Renew con su cuenta conectada de Stripe.
 * No almacena números de cuenta bancaria ni documentos de identidad: Stripe
 * gestiona esos datos dentro de su flujo de onboarding hospedado.
 */
@Entity
@Table(
        name = "cuentas_stripe_conectadas",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cuenta_stripe_usuario", columnNames = "usuario_id"),
                @UniqueConstraint(name = "uk_cuenta_stripe_account_id", columnNames = "stripe_account_id")
        }
)
public class CuentaStripeConectada {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "cuenta_stripe_conectada_id", nullable = false, updatable = false)
    private UUID cuentaStripeConectadaId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "usuario_id",
            referencedColumnName = "usuarios_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_cuenta_stripe_usuario")
    )
    private Usuario usuario;

    @Column(name = "stripe_account_id", nullable = false, unique = true, length = 255)
    private String stripeAccountId;

    @Column(name = "detalles_enviados", nullable = false)
    private boolean detallesEnviados;

    @Column(name = "transferencias_habilitadas", nullable = false)
    private boolean transferenciasHabilitadas;

    @Column(name = "payouts_habilitados", nullable = false)
    private boolean payoutsHabilitados;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private OffsetDateTime creadaEn;

    @Column(name = "actualizada_en", nullable = false)
    private OffsetDateTime actualizadaEn;

    @PrePersist
    protected void alCrear() {
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        if (creadaEn == null) {
            creadaEn = ahora;
        }
        actualizadaEn = ahora;
    }

    @PreUpdate
    protected void alActualizar() {
        actualizadaEn = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getCuentaStripeConectadaId() {
        return cuentaStripeConectadaId;
    }

    public void setCuentaStripeConectadaId(UUID cuentaStripeConectadaId) {
        this.cuentaStripeConectadaId = cuentaStripeConectadaId;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getStripeAccountId() {
        return stripeAccountId;
    }

    public void setStripeAccountId(String stripeAccountId) {
        this.stripeAccountId = stripeAccountId;
    }

    public boolean isDetallesEnviados() {
        return detallesEnviados;
    }

    public void setDetallesEnviados(boolean detallesEnviados) {
        this.detallesEnviados = detallesEnviados;
    }

    public boolean isTransferenciasHabilitadas() {
        return transferenciasHabilitadas;
    }

    public void setTransferenciasHabilitadas(boolean transferenciasHabilitadas) {
        this.transferenciasHabilitadas = transferenciasHabilitadas;
    }

    public boolean isPayoutsHabilitados() {
        return payoutsHabilitados;
    }

    public void setPayoutsHabilitados(boolean payoutsHabilitados) {
        this.payoutsHabilitados = payoutsHabilitados;
    }

    public OffsetDateTime getCreadaEn() {
        return creadaEn;
    }

    public void setCreadaEn(OffsetDateTime creadaEn) {
        this.creadaEn = creadaEn;
    }

    public OffsetDateTime getActualizadaEn() {
        return actualizadaEn;
    }

    public void setActualizadaEn(OffsetDateTime actualizadaEn) {
        this.actualizadaEn = actualizadaEn;
    }
}
