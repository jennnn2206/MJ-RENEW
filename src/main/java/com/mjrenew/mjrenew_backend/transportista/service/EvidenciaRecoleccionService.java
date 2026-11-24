package com.mjrenew.mjrenew_backend.transportista.service;

import com.mjrenew.mjrenew_backend.nucleo.almacenamiento.AlmacenamientoService;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.transportista.dto.TrasladoAntiguedadResumenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@Service
public class EvidenciaRecoleccionService {
    private static final Logger log = LoggerFactory.getLogger(EvidenciaRecoleccionService.class);
    private static final Set<String> TIPOS = Set.of("image/jpeg", "image/png", "image/webp");
    private final TrasladoAntiguedadService traslados;
    private final AlmacenamientoService almacenamiento;
    private final FotografiaAntiguedadRepository fotografias;

    public EvidenciaRecoleccionService(TrasladoAntiguedadService traslados, AlmacenamientoService almacenamiento,
                                      FotografiaAntiguedadRepository fotografias) {
        this.traslados = traslados;
        this.almacenamiento = almacenamiento;
        this.fotografias = fotografias;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public TrasladoAntiguedadResumenResponse confirmarSalida(UUID trasladoId, UUID transportistaId, List<MultipartFile> fotos) {
        if (!traslados.requiereEvidenciaRecoleccion(trasladoId, transportistaId)) {
            return traslados.registrarSalidaRecoleccion(trasladoId, transportistaId, List.of());
        }
        if (fotos == null || fotos.size() < 4) throw new SolicitudInvalidaException("Se requieren al menos cuatro fotografías");
        for (MultipartFile foto : fotos) {
            if (foto == null || foto.isEmpty() || foto.getContentType() == null || !TIPOS.contains(foto.getContentType())
                    || foto.getSize() > 10 * 1024 * 1024) {
                throw new SolicitudInvalidaException("Las fotografías deben ser JPG, PNG o WEBP de hasta 10 MB");
            }
        }
        List<String> subidas = new ArrayList<>();
        try {
            for (MultipartFile foto : fotos) subidas.add(almacenamiento.subirArchivo(foto, "traslados"));
            var respuesta = traslados.registrarSalidaRecoleccion(trasladoId, transportistaId, subidas);
            limpiarSinRegistro(subidas);
            return respuesta;
        } catch (RuntimeException ex) {
            limpiarSinRegistro(subidas);
            throw ex;
        }
    }

    private void limpiarSinRegistro(List<String> subidas) {
        for (String url : subidas) {
            try {
                if (!fotografias.existsByUrlAlmacenFotografia(url)) almacenamiento.eliminarArchivo(url);
            } catch (RuntimeException ex) {
                log.warn("No se pudo conciliar la evidencia de recolección {}", url, ex);
            }
        }
    }
}
