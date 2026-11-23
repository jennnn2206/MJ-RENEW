package com.mjrenew.mjrenew_backend.nucleo.usuario.repository;

import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.usuariosId = :usuarioId")
    Optional<Usuario> bloquearPorId(@Param("usuarioId") UUID usuarioId);

    Optional<Usuario> findByCorreoElectronicoUsuario(String correoElectronicoUsuario);

    boolean existsByCorreoElectronicoUsuario(String correoElectronicoUsuario);
}
