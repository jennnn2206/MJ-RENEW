package com.mjrenew.mjrenew_backend.restaurador;

import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository.AntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoFotografia;
import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.exception.TransicionEstadoInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.nucleo.usuario.repository.UsuarioRepository;
import com.mjrenew.mjrenew_backend.restaurador.dto.PublicarAvanceRequest;
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
    @Mock UsuarioRepository usuarioRepository;
    @Mock org.springframework.context.ApplicationEventPublisher eventos;
    @InjectMocks RestauradorService servicio;

    @Test
    void iniciaConUnLugarDisponibleYBloqueaAntesDeContar() {
        UUID restaurador = UUID.randomUUID();
        Antiguedad antiguedad = recibidaEnTaller(restaurador);
        when(antiguedadRepository.countByRestaurador_UsuariosIdAndEstadoActualAntiguedadIn(
                eq(restaurador), anyCollection())).thenReturn(1L);

        servicio.iniciarRestauracion(antiguedad.getAntiguedadesId(), restaurador);

        assertEquals(EstadoAntiguedad.EN_RESTAURACION, antiguedad.getEstadoActualAntiguedad());
        assertNotNull(antiguedad.getRestauracionInicioAntiguedad());
        var orden = inOrder(usuarioRepository, antiguedadRepository);
        orden.verify(usuarioRepository).bloquearPorId(restaurador);
        orden.verify(antiguedadRepository).bloquearDelRestaurador(antiguedad.getAntiguedadesId(), restaurador);
        orden.verify(antiguedadRepository).countByRestaurador_UsuariosIdAndEstadoActualAntiguedadIn(
                restaurador, List.of(EstadoAntiguedad.EN_RESTAURACION, EstadoAntiguedad.AVANCE_PUBLICADO));
    }

    @Test
    void rechazaLaTerceraSinModificarEstadoNiFecha() {
        UUID restaurador = UUID.randomUUID();
        Antiguedad antiguedad = recibidaEnTaller(restaurador);
        when(antiguedadRepository.countByRestaurador_UsuariosIdAndEstadoActualAntiguedadIn(
                eq(restaurador), anyCollection())).thenReturn(2L);

        assertThrows(TransicionEstadoInvalidaException.class,
                () -> servicio.iniciarRestauracion(antiguedad.getAntiguedadesId(), restaurador));

        assertEquals(EstadoAntiguedad.RECIBIDO_EN_TALLER, antiguedad.getEstadoActualAntiguedad());
        assertNull(antiguedad.getRestauracionInicioAntiguedad());
        verifyNoInteractions(dimensionRepo, fotoRepo);
    }

    @Test
    void rechazaDatosAnterioresQueYaSuperanElLimite() {
        UUID restaurador = UUID.randomUUID();
        Antiguedad antiguedad = recibidaEnTaller(restaurador);
        when(antiguedadRepository.countByRestaurador_UsuariosIdAndEstadoActualAntiguedadIn(
                eq(restaurador), anyCollection())).thenReturn(3L);

        assertThrows(TransicionEstadoInvalidaException.class,
                () -> servicio.iniciarRestauracion(antiguedad.getAntiguedadesId(), restaurador));
        assertNull(antiguedad.getRestauracionInicioAntiguedad());
    }

    @Test
    void noReiniciaUnaRestauracionEnCurso() {
        UUID restaurador = UUID.randomUUID();
        Antiguedad antiguedad = recibidaEnTaller(restaurador);
        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.EN_RESTAURACION);

        assertThrows(TransicionEstadoInvalidaException.class,
                () -> servicio.iniciarRestauracion(antiguedad.getAntiguedadesId(), restaurador));
        verify(antiguedadRepository, never())
                .countByRestaurador_UsuariosIdAndEstadoActualAntiguedadIn(any(), anyCollection());
    }

    @Test
    void noIniciaUnaAntiguedadAjena() {
        UUID restaurador = UUID.randomUUID();
        when(usuarioRepository.bloquearPorId(restaurador)).thenReturn(Optional.of(new Usuario()));
        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.iniciarRestauracion(UUID.randomUUID(), restaurador));
        verify(antiguedadRepository, never())
                .countByRestaurador_UsuariosIdAndEstadoActualAntiguedadIn(any(), anyCollection());
    }

    @Test
    void unAvanceTardioNoReabreUnaRestauracionTerminada() {
        UUID restaurador = UUID.randomUUID();
        UUID antiguedadId = UUID.randomUUID();
        Antiguedad antiguedad = new Antiguedad();
        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.RESTAURACION_LISTA);
        when(antiguedadRepository.bloquearDelRestaurador(antiguedadId, restaurador))
                .thenReturn(Optional.of(antiguedad));

        assertThrows(TransicionEstadoInvalidaException.class,
                () -> servicio.publicarAvanceRestauracion(antiguedadId, restaurador,
                        new PublicarAvanceRequest("Avance tardío"), null));
        assertEquals(EstadoAntiguedad.RESTAURACION_LISTA, antiguedad.getEstadoActualAntiguedad());
        verifyNoInteractions(avanceRepo, fotoRepo);
    }

    private Antiguedad recibidaEnTaller(UUID restaurador) {
        Antiguedad antiguedad = new Antiguedad();
        antiguedad.setAntiguedadesId(UUID.randomUUID());
        antiguedad.setEstadoActualAntiguedad(EstadoAntiguedad.RECIBIDO_EN_TALLER);
        when(usuarioRepository.bloquearPorId(restaurador)).thenReturn(Optional.of(new Usuario()));
        when(antiguedadRepository.bloquearDelRestaurador(antiguedad.getAntiguedadesId(), restaurador))
                .thenReturn(Optional.of(antiguedad));
        return antiguedad;
    }

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


