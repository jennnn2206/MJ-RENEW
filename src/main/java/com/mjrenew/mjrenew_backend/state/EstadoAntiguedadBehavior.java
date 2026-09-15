package com.mjrenew.mjrenew_backend.state;

/**
 * Comportamiento asociado a un grupo de estados del AFD de {@link com.mjrenew.mjrenew_backend.enums.EstadoAntiguedad}.
 * <p>
 * El AFD (el enum, con sus 25 valores) es la fuente de verdad del flujo de una pieza.
 * Este patrón State no lo reemplaza ni duplica: solo agrupa esos 25 valores en
 * categorías de comportamiento real del software (¿el estado admite transiciones o no?),
 * en vez de tener un bloque de if/else o switch disperso por el código.
 */
public interface EstadoAntiguedadBehavior {

    /**
     * Un estado terminal no admite transiciones a otro estado.
     */
    boolean esTerminal();

    /**
     * Etiqueta legible de la categoría de comportamiento, útil para respuestas de API y logs.
     */
    String descripcion();
}
