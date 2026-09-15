package com.mjrenew.mjrenew_backend.mapper;

import com.mjrenew.mjrenew_backend.dto.antiguedad.AntiguedadCreateRequest;
import com.mjrenew.mjrenew_backend.dto.antiguedad.AntiguedadResponse;
import com.mjrenew.mjrenew_backend.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.entity.Usuario;

/**
 * Mapeo manual DTO <-> entidad. No asigna propietario, estado ni fechas:
 * esos valores dependen del usuario autenticado y del flujo del AFD,
 * y son responsabilidad del service, no del mapper.
 */
public final class AntiguedadMapper {

    private AntiguedadMapper() {
    }

    public static Antiguedad toEntity(AntiguedadCreateRequest request) {
        Antiguedad antiguedad = new Antiguedad();
        antiguedad.setTipoMueble(request.tipoMueble());
        antiguedad.setEstiloMueble(request.estiloMueble());
        antiguedad.setMaterialMueble(request.materialMueble());
        antiguedad.setDescripcionDaniosAntiguedad(request.descripcionDaniosAntiguedad());
        antiguedad.setProcedenciaMueble(request.procedenciaMueble());
        return antiguedad;
    }

    public static AntiguedadResponse toResponse(Antiguedad antiguedad) {
        Usuario propietario = antiguedad.getPropietario();
        Usuario restaurador = antiguedad.getRestaurador();
        return new AntiguedadResponse(
                antiguedad.getAntiguedadesId(),
                propietario != null ? propietario.getUsuariosId() : null,
                propietario != null ? propietario.getNombreCompletoUsuario() : null,
                restaurador != null ? restaurador.getUsuariosId() : null,
                restaurador != null ? restaurador.getNombreCompletoUsuario() : null,
                antiguedad.getTipoMueble(),
                antiguedad.getEstiloMueble(),
                antiguedad.getMaterialMueble(),
                antiguedad.getDescripcionDaniosAntiguedad(),
                antiguedad.getProcedenciaMueble(),
                antiguedad.getEstadoActualAntiguedad(),
                antiguedad.getRegistradaEnAntiguedad()
        );
    }
}
