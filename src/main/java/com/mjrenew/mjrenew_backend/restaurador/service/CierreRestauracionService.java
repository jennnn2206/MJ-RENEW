package com.mjrenew.mjrenew_backend.restaurador.service;

import com.mjrenew.mjrenew_backend.nucleo.almacenamiento.AlmacenamientoService;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.exception.TransicionEstadoInvalidaException;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CierreRestauracionService {
    private static final Logger log = LoggerFactory.getLogger(CierreRestauracionService.class);
    private static final Set<String> TIPOS_FOTO = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_BYTES_FOTO = 10 * 1024 * 1024;
    private final RestauradorService restauraciones;
    private final AlmacenamientoService almacenamiento;
    private final FotografiaAntiguedadRepository fotografias;

    public CierreRestauracionService(RestauradorService restauraciones, AlmacenamientoService almacenamiento,
                                    FotografiaAntiguedadRepository fotografias) {
        this.restauraciones = restauraciones;
        this.almacenamiento = almacenamiento;
        this.fotografias = fotografias;
    }

    // Suspende incluso una transacción del invocador durante las llamadas al almacenamiento.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AntiguedadDetalleResponse cerrar(UUID antiguedadId, UUID restauradorId, List<MultipartFile> fotos) {
        AntiguedadDetalleResponse actual = restauraciones.obtenerDetalleAsignado(antiguedadId, restauradorId);
        if (actual.estadoActualAntiguedad() == EstadoAntiguedad.RESTAURACION_LISTA
                && actual.fotosEstadoFinal().size() >= 4) return actual;
        if (actual.estadoActualAntiguedad() != EstadoAntiguedad.EN_RESTAURACION
                && actual.estadoActualAntiguedad() != EstadoAntiguedad.AVANCE_PUBLICADO
                && actual.estadoActualAntiguedad() != EstadoAntiguedad.RESTAURACION_LISTA) {
            throw new TransicionEstadoInvalidaException("La antigüedad debe estar en restauración para terminarla");
        }
        validarFotos(fotos);
        List<String> subidas = new ArrayList<>();
        try {
            for (MultipartFile foto : fotos) {
                subidas.add(almacenamiento.subirArchivo(foto, "fotos-finales"));
            }
            var detalle = restauraciones.guardarCierreRestauracion(antiguedadId, restauradorId, subidas);
            limpiarNoGuardadas(subidas);
            return detalle;
        } catch (RuntimeException ex) {
            limpiarNoGuardadas(subidas);
            throw ex;
        }
    }

    private void validarFotos(List<MultipartFile> fotos) {
        if (fotos == null || fotos.size() < 4) {
            throw new SolicitudInvalidaException("Se requieren al menos 4 fotografías del estado final");
        }
        for (MultipartFile foto : fotos) {
            if (foto == null || foto.isEmpty() || foto.getContentType() == null
                    || !TIPOS_FOTO.contains(foto.getContentType())) {
                throw new SolicitudInvalidaException("Las fotografías finales deben ser imágenes JPG, PNG o WEBP");
            }
            if (foto.getSize() > MAX_BYTES_FOTO) {
                throw new SolicitudInvalidaException("Cada fotografía final puede pesar como máximo 10 MB");
            }
        }
    }

    private void limpiarNoGuardadas(List<String> subidas) {
        for (String url : subidas) {
            try {
                // Ante una confirmación SQL ambigua, conserva cualquier archivo que ya tenga evidencia registrada.
                if (!fotografias.existsByUrlAlmacenFotografia(url)) almacenamiento.eliminarArchivo(url);
            } catch (RuntimeException ex) {
                log.warn("No se pudo conciliar el archivo de cierre {}", url, ex);
            }
        }
    }
}
