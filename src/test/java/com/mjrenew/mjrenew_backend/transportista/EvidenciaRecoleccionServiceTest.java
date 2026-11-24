package com.mjrenew.mjrenew_backend.transportista;

import com.mjrenew.mjrenew_backend.nucleo.almacenamiento.AlmacenamientoService;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.transportista.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvidenciaRecoleccionServiceTest {
    @Mock TrasladoAntiguedadService traslados;
    @Mock AlmacenamientoService almacenamiento;
    @Mock FotografiaAntiguedadRepository fotos;
    @InjectMocks EvidenciaRecoleccionService servicio;
    private final UUID id = UUID.randomUUID();
    private final UUID transportista = UUID.randomUUID();

    @Test
    void exigeCuatroFotosAntesDeSubir() {
        when(traslados.requiereEvidenciaRecoleccion(id, transportista)).thenReturn(true);
        assertThrows(SolicitudInvalidaException.class, () -> servicio.confirmarSalida(id, transportista, archivos().subList(0, 3)));
        verifyNoInteractions(almacenamiento);
    }

    @Test
    void unArchivoInvalidoAlFinalDelLoteImpideTodaLaSubida() {
        when(traslados.requiereEvidenciaRecoleccion(id, transportista)).thenReturn(true);
        var archivos = new ArrayList<>(archivos());
        archivos.set(3, new MockMultipartFile("fotos", "evidencia.txt", "text/plain", new byte[]{1}));
        assertThrows(SolicitudInvalidaException.class, () -> servicio.confirmarSalida(id, transportista, archivos));
        verifyNoInteractions(almacenamiento);
    }

    @Test
    void compensaLasSubidasSiFallaUnArchivo() {
        when(traslados.requiereEvidenciaRecoleccion(id, transportista)).thenReturn(true);
        when(almacenamiento.subirArchivo(any(), eq("traslados")))
                .thenReturn("http://almacenamiento/foto-1").thenThrow(new IllegalStateException("Error de subida"));
        assertThrows(IllegalStateException.class, () -> servicio.confirmarSalida(id, transportista, archivos()));
        verify(almacenamiento).eliminarArchivo("http://almacenamiento/foto-1");
        verify(traslados, never()).registrarSalidaRecoleccion(any(), any(), anyList());
    }

    @Test
    void unErrorSQLConservaLasEvidenciasQueTienenRegistro() {
        when(traslados.requiereEvidenciaRecoleccion(id, transportista)).thenReturn(true);
        when(almacenamiento.subirArchivo(any(), anyString())).thenReturn("u1", "u2", "u3", "u4");
        when(traslados.registrarSalidaRecoleccion(eq(id), eq(transportista), anyList()))
                .thenThrow(new IllegalStateException("Resultado incierto"));
        when(fotos.existsByUrlAlmacenFotografia("u1")).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> servicio.confirmarSalida(id, transportista, archivos()));
        verify(almacenamiento, never()).eliminarArchivo("u1");
        verify(almacenamiento).eliminarArchivo("u2");
        verify(almacenamiento).eliminarArchivo("u3");
        verify(almacenamiento).eliminarArchivo("u4");
    }

    @Test
    void reintentoDeSalidaYaRegistradaNoVuelveASubir() {
        servicio.confirmarSalida(id, transportista, archivos());
        verify(traslados).registrarSalidaRecoleccion(id, transportista, List.of());
        verifyNoInteractions(almacenamiento, fotos);
    }

    private List<MultipartFile> archivos() {
        MultipartFile archivo = new MockMultipartFile("fotos", "foto.jpg", "image/jpeg", new byte[]{1});
        return List.of(archivo, archivo, archivo, archivo);
    }
}
