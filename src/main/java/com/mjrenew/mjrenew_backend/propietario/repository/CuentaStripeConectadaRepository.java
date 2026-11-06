package com.mjrenew.mjrenew_backend.propietario.repository;

import com.mjrenew.mjrenew_backend.propietario.entity.CuentaStripeConectada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CuentaStripeConectadaRepository extends JpaRepository<CuentaStripeConectada, UUID> {

    Optional<CuentaStripeConectada> findByUsuario_UsuariosId(UUID usuarioId);

    Optional<CuentaStripeConectada> findByStripeAccountId(String stripeAccountId);
}
