package com.mjrenew.mjrenew_backend.state;

import com.mjrenew.mjrenew_backend.enums.EstadoAntiguedad;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Resuelve el {@link EstadoAntiguedadBehavior} de cada uno de los 25 valores de
 * {@link EstadoAntiguedad}, agrupándolos en 3 categorías de comportamiento real
 * (no una clase de estado por cada valor del AFD):
 * <ul>
 *   <li>Flujo activo: la pieza sigue avanzando por el proceso.</li>
 *   <li>Terminal exitoso: el proceso concluyó como se esperaba.</li>
 *   <li>Terminal cancelado: el proceso se detuvo antes de completarse.</li>
 * </ul>
 * El mapa se construye una sola vez, asignando explícitamente cada valor del enum
 * a su categoría, para que quede claro y auditable a qué grupo pertenece cada estado.
 */
@Component
public class EstadoAntiguedadBehaviorResolver {

    private final Map<EstadoAntiguedad, EstadoAntiguedadBehavior> comportamientos = new EnumMap<>(EstadoAntiguedad.class);

    public EstadoAntiguedadBehaviorResolver() {
        EstadoAntiguedadBehavior flujoActivo = new FlujoActivoEstadoBehavior();
        EstadoAntiguedadBehavior terminalExitoso = new TerminalExitosoEstadoBehavior();
        EstadoAntiguedadBehavior terminalCancelado = new TerminalCanceladoEstadoBehavior();

        // Flujo activo (17): la pieza todavía puede transicionar a un siguiente estado.
        comportamientos.put(EstadoAntiguedad.PIEZA_CAPTURADA, flujoActivo);
        comportamientos.put(EstadoAntiguedad.EN_EVALUACION, flujoActivo);
        comportamientos.put(EstadoAntiguedad.CALCULANDO_PRESUPUESTO, flujoActivo);
        comportamientos.put(EstadoAntiguedad.PRESUPUESTO_PRESENTADO, flujoActivo);
        comportamientos.put(EstadoAntiguedad.PAGO_EN_ESCROW, flujoActivo);
        comportamientos.put(EstadoAntiguedad.HORARIO_RECOLECCION_AGENDADO, flujoActivo);
        comportamientos.put(EstadoAntiguedad.EN_RECOLECCION, flujoActivo);
        comportamientos.put(EstadoAntiguedad.RECIBIDO_EN_TALLER, flujoActivo);
        comportamientos.put(EstadoAntiguedad.EN_RESTAURACION, flujoActivo);
        comportamientos.put(EstadoAntiguedad.AVANCE_PUBLICADO, flujoActivo);
        comportamientos.put(EstadoAntiguedad.RESTAURACION_LISTA, flujoActivo);
        comportamientos.put(EstadoAntiguedad.DECISION_EN_TALLER, flujoActivo);
        comportamientos.put(EstadoAntiguedad.HORARIO_ENTREGA_AGENDADO, flujoActivo);
        comportamientos.put(EstadoAntiguedad.EN_DEVOLUCION_PROPIETARIO, flujoActivo);
        comportamientos.put(EstadoAntiguedad.EN_CATALOGO, flujoActivo);
        comportamientos.put(EstadoAntiguedad.LINK_PAGO_ENVIADO, flujoActivo);
        comportamientos.put(EstadoAntiguedad.SOLICITUD_DEVOLUCION_TALLER, flujoActivo);

        // Terminal exitoso (4): el proceso concluyó como se esperaba.
        comportamientos.put(EstadoAntiguedad.ENTREGADO_AL_PROPIETARIO, terminalExitoso);
        comportamientos.put(EstadoAntiguedad.RESTAURACION_COMPLETADA, terminalExitoso);
        comportamientos.put(EstadoAntiguedad.VENTA_COMPLETADA, terminalExitoso);
        comportamientos.put(EstadoAntiguedad.DEVUELTA_DESDE_TALLER, terminalExitoso);

        // Terminal cancelado (4): el proceso se detuvo antes de completarse.
        comportamientos.put(EstadoAntiguedad.CANCELADA_NO_CUMPLE, terminalCancelado);
        comportamientos.put(EstadoAntiguedad.CANCELADA_DUENO_DECLINO, terminalCancelado);
        comportamientos.put(EstadoAntiguedad.CANCELADA_RECOLECCION, terminalCancelado);
        comportamientos.put(EstadoAntiguedad.CANCELADA_ENTREGA, terminalCancelado);
    }

    public EstadoAntiguedadBehavior resolver(EstadoAntiguedad estado) {
        EstadoAntiguedadBehavior comportamiento = comportamientos.get(estado);
        if (comportamiento == null) {
            throw new IllegalStateException("No hay comportamiento definido para el estado: " + estado);
        }
        return comportamiento;
    }
}
