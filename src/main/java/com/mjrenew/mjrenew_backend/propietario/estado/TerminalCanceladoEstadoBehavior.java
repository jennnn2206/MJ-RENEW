package com.mjrenew.mjrenew_backend.propietario.estado;

/**
 * Estados finales negativos del AFD: el proceso se detuvo antes de completarse
 * (no cumple requisitos, el dueño declinó, falló la recolección o la entrega).
 * No admite transiciones posteriores.
 */
public class TerminalCanceladoEstadoBehavior implements EstadoAntiguedadBehavior {

    @Override
    public boolean esTerminal() {
        return true;
    }

    @Override
    public String descripcion() {
        return "Cancelado";
    }
}
