package com.mjrenew.mjrenew_backend.propietario.dto;

import com.mjrenew.mjrenew_backend.nucleo.enums.EstiloMueble;
import com.mjrenew.mjrenew_backend.nucleo.enums.MaterialMueble;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoMueble;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

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

        // MJRENEW-FLUJO: RF-008 requiere mínimo tres imágenes antes de solicitar evaluación.
        // Esta versión admite URLs; subir JPG/PNG desde el equipo está pendiente.
        @NotNull(message = "Agrega al menos 3 fotografías de la antigüedad")
        @Size(min = 3, message = "Debes registrar al menos 3 fotografías iniciales")
        List<@NotBlank(message = "La URL de una foto no puede estar vacía")
        @Size(max = 300, message = "La URL de una fotografía no puede superar 300 caracteres")
        @Pattern(regexp = "https?://.+", message = "Las fotos deben tener una URL http o https") String> urlsFotografiasIniciales,

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
