package com.mjrenew.mjrenew_backend.propietario.estado;

/**
 * Estados finales positivos del AFD: el proceso concluyó como se esperaba
 * (restauración completada, venta completada, entrega realizada). No admite
 * transiciones posteriores.
 */
public class TerminalExitosoEstadoBehavior implements EstadoAntiguedadBehavior {

    @Override
    public boolean esTerminal() {
        return true;
    }

    @Override
    public String descripcion() {
        return "Finalizado exitosamente";
    }
}
