package com.mjrenew.mjrenew_backend.propietario.dto;

import com.mjrenew.mjrenew_backend.nucleo.enums.EstiloMueble;
import com.mjrenew.mjrenew_backend.nucleo.enums.MaterialMueble;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoMueble;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Las fotos del estado inicial y las dimensiones las aporta el propietario
 * al registrar la pieza (no el restaurador durante la evaluación: para
 * entonces la pieza nunca estuvo en su poder, evalúa de forma visual con
 * estas mismas fotos). Todos estos campos son opcionales para no exigirle
 * al propietario datos que quizás no tenga a la mano todavía, y para no
 * romper los registros ya existentes que se crearon sin ellos.
 */
public record AntiguedadCreateRequest(

        @NotNull(message = "El tipo de mueble es obligatorio")
        TipoMueble tipoMueble,

        EstiloMueble estiloMueble,

        MaterialMueble materialMueble,

        @Size(max = 1000, message = "La descripción de daños no puede superar los 1000 caracteres")
        String descripcionDaniosAntiguedad,

        @Size(max = 300, message = "La procedencia no puede superar los 300 caracteres")
        String procedenciaMueble,

        List<@NotBlank(message = "Cada URL de fotografía debe tener contenido") String> urlsFotografiasIniciales,

        @DecimalMin(value = "0.01", message = "El alto debe ser mayor que cero")
        BigDecimal altoCmAntiguedad,

        @DecimalMin(value = "0.01", message = "El ancho debe ser mayor que cero")
        BigDecimal anchoCmAntiguedad,

        @DecimalMin(value = "0.01", message = "La profundidad debe ser mayor que cero")
        BigDecimal profundidadCmAntiguedad,

        @DecimalMin(value = "0.01", message = "El peso debe ser mayor que cero")
        BigDecimal pesoKgAntiguedad
) {
}
