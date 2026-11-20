package com.mjrenew.mjrenew_backend.transaccion.repository;

import com.mjrenew.mjrenew_backend.transaccion.dto.DesgloseLiberacion;
import com.mjrenew.mjrenew_backend.transaccion.entity.TransaccionBancaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTransaccion;
import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.util.List;

public interface TransaccionBancariaRepository extends JpaRepository<TransaccionBancaria, UUID> {
    List<TransaccionBancaria> findByTipoTransaccionInAndEstadoTransaccion(List<TipoTransaccion> tipos, EstadoTransaccion estado);

    @Query("""
            select new com.mjrenew.mjrenew_backend.transaccion.dto.DesgloseLiberacion(
                t.antiguedad.antiguedadesId, t.estadoTransaccion, t.montoBrutoMxnTransaccion,
                t.comisionMjrenewMxn, t.cargoStripeMxn, coalesce(t.montoNetoRestauradorMxn, t.montoNetoPropietarioMxn))
            from TransaccionBancaria t where t.beneficiario.usuariosId = :beneficiario and t.tipoTransaccion = :tipo
            order by t.creadaEnTransaccion desc
            """)
    List<DesgloseLiberacion> obtenerCobros(
            @Param("beneficiario") UUID beneficiario, @Param("tipo") TipoTransaccion tipo);

    // MJRENEW-STRIPE: el bloqueo serializa reintentos simultáneos de webhook.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TransaccionBancaria>
    findByReferenciaStripeTransaccion(
            String referenciaStripeTransaccion
    );

    Optional<TransaccionBancaria>
    findFirstByAntiguedad_AntiguedadesIdAndTipoTransaccionAndEstadoTransaccionOrderByCreadaEnTransaccionDesc(
            UUID antiguedadId, TipoTransaccion tipoTransaccion, EstadoTransaccion estadoTransaccion);

    Optional<TransaccionBancaria>
    findFirstByCatalogoAntiguedad_CatalogoAntiguedadIdAndTipoTransaccionAndEstadoTransaccionOrderByCreadaEnTransaccionDesc(
            UUID catalogoId, TipoTransaccion tipoTransaccion, EstadoTransaccion estadoTransaccion);

    /** Busca una operación derivada de una transacción de pago, por tipo de liberación. */
    Optional<TransaccionBancaria>
    findByTransaccionPadre_TransaccionIdAndTipoTransaccion(
            UUID transaccionPadreId, TipoTransaccion tipoTransaccion);


    List<TransaccionBancaria>
    findTop50ByEstadoTransaccionAndReferenciaStripeTransaccionIsNotNullAndCreadaEnTransaccionBeforeOrderByCreadaEnTransaccionAsc(
            EstadoTransaccion estadoTransaccion, OffsetDateTime antesDe);

    List<TransaccionBancaria>
    findByTipoTransaccionAndEstadoTransaccionAndCargoStripeMxnIsNull(
            TipoTransaccion tipoTransaccion, EstadoTransaccion estadoTransaccion);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TransaccionBancaria t where t.transaccionId = :id")
    Optional<TransaccionBancaria> bloquearPorId(@Param("id") UUID id);
}
