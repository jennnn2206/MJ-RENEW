package com.mjrenew.mjrenew_backend.state;

/**
 * Estados intermedios del AFD: la pieza sigue avanzando por el proceso de
 * restauración/venta y admite transicionar a un siguiente estado.
 */
public class FlujoActivoEstadoBehavior implements EstadoAntiguedadBehavior {

    @Override
    public boolean esTerminal() {
        return false;
    }

    @Override
    public String descripcion() {
        return "En flujo activo";
    }
}
