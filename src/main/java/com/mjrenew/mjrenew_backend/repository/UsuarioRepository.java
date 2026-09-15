package com.mjrenew.mjrenew_backend.repository;

import com.mjrenew.mjrenew_backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByCorreoElectronicoUsuario(String correoElectronicoUsuario);

    boolean existsByCorreoElectronicoUsuario(String correoElectronicoUsuario);
}
