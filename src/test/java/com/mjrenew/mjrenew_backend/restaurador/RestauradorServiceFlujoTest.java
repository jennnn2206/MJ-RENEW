package com.mjrenew.mjrenew_backend.restaurador;

import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository.AntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoFotografia;
import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.propietario.entity.FotografiaAntiguedad;
import com.mjrenew.mjrenew_backend.propietario.mapper.AntiguedadMapper;
import com.mjrenew.mjrenew_backend.propietario.repository.DimensionRepository;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.restaurador.dto.EvaluarAntiguedadRequest;
import com.mjrenew.mjrenew_backend.restaurador.mapper.RestauradorMapper;
import com.mjrenew.mjrenew_backend.restaurador.repository.AvanceRestauracionRepository;
import com.mjrenew.mjrenew_backend.restaurador.repository.CotizacionRepository;
import com.mjrenew.mjrenew_backend.restaurador.repository.PerfilRestauradorRepository;
import com.mjrenew.mjrenew_backend.restaurador.service.RestauradorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Pruebas unitarias sin Stripe ni PostgreSQL: validar selección, permisos y AFD q1 -> q2. */
@ExtendWith(MockitoExtension.class)
class RestauradorServiceFlujoTest {
    @Mock AntiguedadRepository antiguedadRepository;
    @Mock FotografiaAntiguedadRepository fotoRepo;
    @Mock RestauradorMapper restauradorMapper;
    @Mock PerfilRestauradorRepository perfilRepo;
    @Mock DimensionRepository dimensionRepo;
    @Mock AntiguedadMapper antiguedadMapper;
    @Mock CotizacionRepository cotizacionRepo;
    @Mock AvanceRestauracionRepository avanceRepo;
    @InjectMocks RestauradorService servicio;

    @Test
    void solicitudesIncluyenEvaluacionYPresupuestoPendiente() {
        UUID restaurador = UUID.randomUUID();
        when(antiguedadRepository.findByRestaurador_UsuariosIdAndEstadoActualAntiguedadIn(
                eq(restaurador), anyCollection(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<?> resultado = servicio.obtenerSolicitudesAsignadas(restaurador, Pageable.unpaged());
        assertTrue(resultado.isEmpty());
        verify(antiguedadRepository).findByRestaurador_UsuariosIdAndEstadoActualAntiguedadIn(
                eq(restaurador), eq(List.of(EstadoAntiguedad.EN_EVALUACION,
                        EstadoAntiguedad.CALCULANDO_PRESUPUESTO)), any(Pageable.class));
    }

    @Test
    void rechazaLeerExpedienteNoAsignado() {
        UUID antiguedadId = UUID.randomUUID();
        UUID restauradorId = UUID.randomUUID();
        when(antiguedadRepository.findByAntiguedadesIdAndRestaurador_UsuariosId(antiguedadId, restauradorId))
                .thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.obtenerDetalleAsignado(antiguedadId, restauradorId));
    }

    @Test
    void noPermiteConfirmarEvaluacionSinTresFotos() {
        UUID antiguedadId = UUID.randomUUID();
        UUID restauradorId = UUID.randomUUID();
        Antiguedad antiguedad = new Antiguedad();
        antiguedad.setAntiguedadesId(antiguedadId);
        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.EN_EVALUACION);
        when(antiguedadRepository.findByAntiguedadesIdAndRestaurador_UsuariosId(antiguedadId, restauradorId))
                .thenReturn(Optional.of(antiguedad));
        when(fotoRepo.findByAntiguedad_AntiguedadesIdAndTipoFotografiaOrderBySubidaEnFotografiaAsc(
                antiguedadId, TipoFotografia.ESTADO_INICIAL)).thenReturn(List.of());

        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.confirmarEvaluacion(antiguedadId, restauradorId, new EvaluarAntiguedadRequest()));
        assertEquals(EstadoAntiguedad.EN_EVALUACION, antiguedad.getEstadoActualAntiguedad());
    }

    @Test
    void confirmaEvaluacionConTresFotosSinSaltarAPresupuestoPresentado() {
        UUID antiguedadId = UUID.randomUUID();
        UUID restauradorId = UUID.randomUUID();
        Antiguedad antiguedad = new Antiguedad();
        antiguedad.setAntiguedadesId(antiguedadId);
        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.EN_EVALUACION);
        when(antiguedadRepository.findByAntiguedadesIdAndRestaurador_UsuariosId(antiguedadId, restauradorId))
                .thenReturn(Optional.of(antiguedad));
        when(fotoRepo.findByAntiguedad_AntiguedadesIdAndTipoFotografiaOrderBySubidaEnFotografiaAsc(
                antiguedadId, TipoFotografia.ESTADO_INICIAL))
                .thenReturn(List.of(new FotografiaAntiguedad(), new FotografiaAntiguedad(), new FotografiaAntiguedad()));
        when(dimensionRepo.findByAntiguedad_AntiguedadesId(any())).thenReturn(Optional.empty());

        servicio.confirmarEvaluacion(antiguedadId, restauradorId, new EvaluarAntiguedadRequest());
        assertEquals(EstadoAntiguedad.CALCULANDO_PRESUPUESTO, antiguedad.getEstadoActualAntiguedad());
    }
}


