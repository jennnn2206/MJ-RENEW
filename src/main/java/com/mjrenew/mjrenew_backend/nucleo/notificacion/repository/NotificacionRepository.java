package com.mjrenew.mjrenew_backend.nucleo.notificacion.repository;

import com.mjrenew.mjrenew_backend.nucleo.notificacion.entity.Notificacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface NotificacionRepository extends JpaRepository<Notificacion, UUID> {
    // PostgreSQL evita duplicados concurrentes sin invalidar la transacción del negocio.
    @Modifying
    @Query(value = """
            insert into notificaciones (notificaciones_id, destinatario_id, antiguedad_id, referencia_id,
              tipo, titulo, mensaje, creado_en, estado_correo, intentos_correo, proximo_intento_en)
            values (:id, :destinatario, :antiguedad, :referencia, :tipo, :titulo, :mensaje, :ahora, 'PENDIENTE', 0, :ahora)
            on conflict (tipo, referencia_id, destinatario_id) do nothing
            """, nativeQuery = true)
    int insertarSiFalta(@Param("id") UUID id, @Param("destinatario") UUID destinatario,
                       @Param("antiguedad") UUID antiguedad, @Param("referencia") UUID referencia,
                       @Param("tipo") String tipo, @Param("titulo") String titulo,
                       @Param("mensaje") String mensaje, @Param("ahora") OffsetDateTime ahora);

    Page<Notificacion> findByDestinatarioId(UUID destinatarioId, Pageable pageable);
    Page<Notificacion> findByDestinatarioIdAndLeidoEnIsNull(UUID destinatarioId, Pageable pageable);
    long countByDestinatarioIdAndLeidoEnIsNull(UUID destinatarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Notificacion n where n.id = :id and n.destinatarioId = :usuarioId")
    Optional<Notificacion> bloquearPropia(@Param("id") UUID id, @Param("usuarioId") UUID usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Notificacion n where n.id = :id")
    Optional<Notificacion> bloquearPorId(@Param("id") UUID id);

    @Query(value = """
            select n.* from notificaciones n join usuarios u on u.usuarios_id = n.destinatario_id
            where u.activo_usuario = true and u.correo_verificado_usuario = true
              and ((n.estado_correo = 'PENDIENTE' and n.proximo_intento_en <= :ahora)
                or (n.estado_correo = 'ENVIANDO' and n.reservado_hasta <= :ahora))
            order by n.proximo_intento_en, n.creado_en, n.notificaciones_id limit 1 for update of n skip locked
            """, nativeQuery = true)
    Optional<Notificacion> reservarSiguiente(@Param("ahora") OffsetDateTime ahora);
}
