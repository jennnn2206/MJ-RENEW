package com.mjrenew.mjrenew_backend.restaurador.dto;

/**
 * Cuerpo de confirmarEvaluacionAntiguedad. Ya no lleva dimensiones: el
 * restaurador no tiene la pieza en su poder durante la evaluación (es
 * visual, a partir de las fotos del estado inicial que subió el
 * propietario), así que no puede medirla. Las dimensiones se capturan al
 * registrar la pieza (ver AntiguedadCreateRequest). Queda vacío a propósito
 * — confirmarEvaluacionAntiguedad solo transiciona el estado; para
 * rechazar existe RechazarEvaluacionRequest, que sí lleva motivo.
 */
public record EvaluarAntiguedadRequest() {
}