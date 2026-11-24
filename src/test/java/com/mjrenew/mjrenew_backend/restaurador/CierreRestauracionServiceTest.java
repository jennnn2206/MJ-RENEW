package com.mjrenew.mjrenew_backend.restaurador;

import com.mjrenew.mjrenew_backend.nucleo.almacenamiento.AlmacenamientoService;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.propietario.dto.AntiguedadDetalleResponse;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.restaurador.service.CierreRestauracionService;
import com.mjrenew.mjrenew_backend.restaurador.service.RestauradorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CierreRestauracionServiceTest {
    @Mock RestauradorService restauraciones;
    @Mock AlmacenamientoService almacenamiento;
    @Mock FotografiaAntiguedadRepository fotografias;
    @InjectMocks CierreRestauracionService servicio;
    private final UUID id = UUID.randomUUID();
    private final UUID restaurador = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        when(restauraciones.obtenerDetalleAsignado(id, restaurador))
                .thenReturn(detalle(EstadoAntiguedad.AVANCE_PUBLICADO, List.of()));
    }

    @Test
    void rechazaTresFotosAntesDeSubir() {
        assertThrows(SolicitudInvalidaException.class, () -> servicio.cerrar(id, restaurador, fotos().subList(0, 3)));
        verifyNoInteractions(almacenamiento, fotografias);
    }

    @Test
    void validaTodoElLoteAntesDeEmpezar() {
        List<MultipartFile> fotos = new java.util.ArrayList<>(fotos());
        fotos.set(3, new MockMultipartFile("fotos", "texto.txt", "text/plain", new byte[]{1}));
        assertThrows(SolicitudInvalidaException.class, () -> servicio.cerrar(id, restaurador, fotos));
        verifyNoInteractions(almacenamiento);
    }

    @Test
    void rechazaArchivoVacioYTamanioExcesivo() {
        List<MultipartFile> fotos = new java.util.ArrayList<>(fotos());
        fotos.set(0, new MockMultipartFile("fotos", "foto.jpg", "image/jpeg", new byte[0]));
        assertThrows(SolicitudInvalidaException.class, () -> servicio.cerrar(id, restaurador, fotos));
        MultipartFile grande = mock(MultipartFile.class);
        when(grande.getContentType()).thenReturn("image/jpeg");
        when(grande.getSize()).thenReturn(10L * 1024 * 1024 + 1);
        fotos.set(0, grande);
        assertThrows(SolicitudInvalidaException.class, () -> servicio.cerrar(id, restaurador, fotos));
        verifyNoInteractions(almacenamiento);
    }

    @Test
    void antiguedadAjenaNuncaSubeArchivos() {
        when(restauraciones.obtenerDetalleAsignado(id, restaurador)).thenThrow(new RecursoNoEncontradoException("Ajena"));
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.cerrar(id, restaurador, fotos()));
        verifyNoInteractions(almacenamiento);
    }

    @Test
    void limpiaLasSubidasPreviasSiFallaUnArchivo() {
        when(almacenamiento.subirArchivo(any(), eq("fotos-finales")))
                .thenReturn("url-1").thenThrow(new IllegalStateException("Almacenamiento caído"));
        assertThrows(IllegalStateException.class, () -> servicio.cerrar(id, restaurador, fotos()));
        verify(almacenamiento).eliminarArchivo("url-1");
        verify(restauraciones, never()).guardarCierreRestauracion(any(), any(), anyList());
    }

    @Test
    void unFalloSQLNoBorraArchivosQueYaTienenRegistro() {
        when(almacenamiento.subirArchivo(any(), anyString())).thenReturn("url-1", "url-2", "url-3", "url-4");
        when(restauraciones.guardarCierreRestauracion(eq(id), eq(restaurador), anyList()))
                .thenThrow(new IllegalStateException("Confirmación SQL incierta"));
        when(fotografias.existsByUrlAlmacenFotografia("url-1")).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> servicio.cerrar(id, restaurador, fotos()));
        verify(almacenamiento, never()).eliminarArchivo("url-1");
        verify(almacenamiento).eliminarArchivo("url-2");
        verify(almacenamiento).eliminarArchivo("url-3");
        verify(almacenamiento).eliminarArchivo("url-4");
    }

    @Test
    void elReintentoDevuelveElCierreSinSubirOtroLote() {
        var existente = detalle(EstadoAntiguedad.RESTAURACION_LISTA, List.of("1", "2", "3", "4"));
        when(restauraciones.obtenerDetalleAsignado(id, restaurador)).thenReturn(existente);
        assertSame(existente, servicio.cerrar(id, restaurador, fotos()));
        verifyNoInteractions(almacenamiento, fotografias);
    }

    private AntiguedadDetalleResponse detalle(EstadoAntiguedad estado, List<String> finales) {
        return new AntiguedadDetalleResponse(id, null, null, null, null, null, estado,
                null, null, List.of(), finales, null);
    }

    static List<MultipartFile> fotos() {
        return IntStream.range(0, 4).mapToObj(i -> (MultipartFile)
                new MockMultipartFile("fotos", "final-" + i + ".jpg", "image/jpeg", new byte[]{1, 2, 3})).toList();
    }
}
