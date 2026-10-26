package com.mjrenew.mjrenew_backend.transaccion.repository;

import com.mjrenew.mjrenew_backend.transaccion.entity.TransaccionBancaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTransaccion;
import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.util.List;

public interface TransaccionBancariaRepository extends JpaRepository<TransaccionBancaria, UUID> {
    // MJRENEW-STRIPE: el bloqueo serializa reintentos simultáneos de webhook.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TransaccionBancaria>
    findByReferenciaStripeTransaccion(
            String referenciaStripeTransaccion
    );

    Optional<TransaccionBancaria>
    findFirstByAntiguedad_AntiguedadesIdAndTipoTransaccionAndEstadoTransaccionOrderByCreadaEnTransaccionDesc(
            UUID antiguedadId, TipoTransaccion tipoTransaccion, EstadoTransaccion estadoTransaccion);

    /** Busca una operación derivada de una transacción de pago, por tipo de liberación. */
    Optional<TransaccionBancaria>
    findByTransaccionPadre_TransaccionIdAndTipoTransaccion(
            UUID transaccionPadreId, TipoTransaccion tipoTransaccion);


    List<TransaccionBancaria>
    findTop50ByEstadoTransaccionAndReferenciaStripeTransaccionIsNotNullAndCreadaEnTransaccionBeforeOrderByCreadaEnTransaccionAsc(
            EstadoTransaccion estadoTransaccion, OffsetDateTime antesDe);
}
