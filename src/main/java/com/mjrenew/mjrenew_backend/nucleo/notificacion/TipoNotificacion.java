package com.mjrenew.mjrenew_backend.nucleo.notificacion;

public enum TipoNotificacion {
    ANTIGUEDAD_ASIGNADA("Nueva antigüedad asignada", "Tienes una antigüedad asignada para evaluar."),
    PRESUPUESTO_PRESENTADO("Presupuesto disponible", "Tu restaurador presentó el presupuesto de tu antigüedad."),
    AVANCE_PUBLICADO("Nuevo avance de restauración", "Tu restaurador publicó un avance con evidencia fotográfica."),
    RESTAURACION_TERMINADA("Restauración terminada", "Tu antigüedad está restaurada. Revisa las fotografías finales y coordina su entrega.");

    private final String titulo;
    private final String mensaje;
    TipoNotificacion(String titulo, String mensaje) { this.titulo = titulo; this.mensaje = mensaje; }
    public String titulo() { return titulo; }
    public String mensaje() { return mensaje; }
    public String rutaPanel() { return this == ANTIGUEDAD_ASIGNADA ? "/restaurador" : "/propietario"; }
}
