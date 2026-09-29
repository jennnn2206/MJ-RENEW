package com.mjrenew.mjrenew_backend.restaurador.repository;

import com.mjrenew.mjrenew_backend.nucleo.enums.DisponibilidadRestaurador;
import com.mjrenew.mjrenew_backend.restaurador.entity.PerfilRestaurador;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PerfilRestauradorRepository
        extends JpaRepository<PerfilRestaurador, UUID> {

    @EntityGraph(attributePaths = "restaurador")
    Page<PerfilRestaurador>
    findByDisponibilidadRestauradorAndAprobadoPorAdminRestauradorTrueAndRestaurador_ActivoUsuarioTrue(
            DisponibilidadRestaurador disponibilidad,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "restaurador")
    Optional<PerfilRestaurador>
    findByRestaurador_UsuariosIdAndDisponibilidadRestauradorAndAprobadoPorAdminRestauradorTrueAndRestaurador_ActivoUsuarioTrue(
            UUID restauradorId,
            DisponibilidadRestaurador disponibilidad
    );

    /*
     * Sin filtro de aprobado/disponible/activo: se usa para que el propio
     * restaurador consulte o cree su perfil (obtenerPerfilRestaurador,
     * actualizarPerfilRestaurador, actualizarDisponibilidadRestaurador) y
     * para que el administrador lo ubique por el id del usuario
     * (aprobarRestaurador), sin importar el estado de aprobación.
     */
    @EntityGraph(attributePaths = "restaurador")
    Optional<PerfilRestaurador> findByRestaurador_UsuariosId(
            UUID restauradorId
    );

    @EntityGraph(attributePaths = "restaurador")
    Page<PerfilRestaurador> findByAprobadoPorAdminRestauradorFalse(
            Pageable pageable
    );
}