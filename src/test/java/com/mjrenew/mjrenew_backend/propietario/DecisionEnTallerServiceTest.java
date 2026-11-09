package com.mjrenew.mjrenew_backend.propietario;

import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import com.mjrenew.mjrenew_backend.catalogo.mapper.CatalogoAntiguedadMapper;
import com.mjrenew.mjrenew_backend.catalogo.repository.CatalogoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.repository.AntiguedadRepository;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.exception.TransicionEstadoInvalidaException;
import com.mjrenew.mjrenew_backend.propietario.dto.PublicarEnCatalogoRequest;
import com.mjrenew.mjrenew_backend.propietario.mapper.AntiguedadMapper;
import com.mjrenew.mjrenew_backend.propietario.repository.DimensionRepository;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.propietario.service.AntiguedadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DecisionEnTallerServiceTest {
    @Mock AntiguedadRepository antiguedades;
    @Mock DimensionRepository dimensiones;
    @Mock FotografiaAntiguedadRepository fotografias;
    @Mock AntiguedadMapper antiguedadMapper;
    @Mock CatalogoAntiguedadRepository catalogos;
    @Mock CatalogoAntiguedadMapper catalogoMapper;
    @InjectMocks AntiguedadService service;

    private final UUID piezaId = UUID.randomUUID();
    private final UUID propietarioId = UUID.randomUUID();

    private Antiguedad piezaEn(EstadoAntiguedad estado) {
        Antiguedad pieza = new Antiguedad();
        pieza.setAntiguedadesId(piezaId);
        pieza.setEstadoActualAntiguedad(estado);
        when(antiguedades.bloquearDelPropietario(piezaId, propietarioId))
                .thenReturn(Optional.of(pieza));
        return pieza;
    }

    @Test
    void abreDecisionSoloCuandoRestauracionEstaLista() {
        Antiguedad pieza = piezaEn(EstadoAntiguedad.RESTAURACION_LISTA);

        service.abrirDecisionEnTaller(piezaId, propietarioId);

        assertEquals(EstadoAntiguedad.DECISION_EN_TALLER, pieza.getEstadoActualAntiguedad());
    }

    @Test
    void solicitarDevolucionImpideElegirOtraRuta() {
        Antiguedad pieza = piezaEn(EstadoAntiguedad.DECISION_EN_TALLER);

        service.solicitarDevolucionTaller(piezaId, propietarioId);

        assertEquals(EstadoAntiguedad.SOLICITUD_DEVOLUCION_TALLER, pieza.getEstadoActualAntiguedad());
        assertThrows(TransicionEstadoInvalidaException.class,
                () -> service.confirmarRecogidaPersonal(piezaId, propietarioId));
    }

    @Test
    void recogidaPersonalCierraElFlujoSinTraslado() {
        Antiguedad pieza = piezaEn(EstadoAntiguedad.DECISION_EN_TALLER);

        service.confirmarRecogidaPersonal(piezaId, propietarioId);

        assertEquals(EstadoAntiguedad.DEVUELTA_DESDE_TALLER, pieza.getEstadoActualAntiguedad());
        verifyNoInteractions(catalogos);
    }

    @Test
    void entregaPermiteConservarLaPieza() {
        Antiguedad pieza = piezaEn(EstadoAntiguedad.ENTREGADO_AL_PROPIETARIO);

        service.conservarRestauracion(piezaId, propietarioId);

        assertEquals(EstadoAntiguedad.RESTAURACION_COMPLETADA, pieza.getEstadoActualAntiguedad());
    }

    @Test
    void publicaDirectamenteDesdeElTaller() {
        Antiguedad pieza = piezaEn(EstadoAntiguedad.DECISION_EN_TALLER);
        when(catalogos.save(any(CatalogoAntiguedad.class))).thenAnswer(inv -> inv.getArgument(0));

        service.publicarEnCatalogo(piezaId, propietarioId,
                new PublicarEnCatalogoRequest(BigDecimal.valueOf(38000), "  Taller Centro 123  "));

        assertEquals(EstadoAntiguedad.EN_CATALOGO, pieza.getEstadoActualAntiguedad());
        verify(catalogos).save(argThat(c -> c.getAntiguedad() == pieza
                && Boolean.TRUE.equals(c.getActivaCatalogo())
                && c.getPrecioMxnCatalogo().compareTo(BigDecimal.valueOf(38000)) == 0
                && "Taller Centro 123".equals(c.getDireccionRecogidaTallerCatalogo())));
    }

    @Test
    void conservaPublicacionTrasEntregaEnDomicilio() {
        Antiguedad pieza = piezaEn(EstadoAntiguedad.ENTREGADO_AL_PROPIETARIO);
        when(catalogos.save(any(CatalogoAntiguedad.class))).thenAnswer(inv -> inv.getArgument(0));

        service.publicarEnCatalogo(piezaId, propietarioId,
                new PublicarEnCatalogoRequest(BigDecimal.valueOf(12000), null));

        assertEquals(EstadoAntiguedad.EN_CATALOGO, pieza.getEstadoActualAntiguedad());
    }

    @Test
    void noPublicaDesdeTallerSinDireccionDeRecogida() {
        Antiguedad pieza = piezaEn(EstadoAntiguedad.DECISION_EN_TALLER);

        assertThrows(SolicitudInvalidaException.class,
                () -> service.publicarEnCatalogo(piezaId, propietarioId,
                        new PublicarEnCatalogoRequest(BigDecimal.TEN, " ")));
        assertEquals(EstadoAntiguedad.DECISION_EN_TALLER, pieza.getEstadoActualAntiguedad());
        verify(catalogos, never()).save(any());
    }

    @Test
    void noPublicaSinDecisionNiEntrega() {
        Antiguedad pieza = piezaEn(EstadoAntiguedad.RESTAURACION_LISTA);

        assertThrows(TransicionEstadoInvalidaException.class,
                () -> service.publicarEnCatalogo(piezaId, propietarioId,
                        new PublicarEnCatalogoRequest(BigDecimal.TEN, null)));
        verifyNoInteractions(catalogos);
    }

    @Test
    void noPermiteDecidirSobrePiezaAjena() {
        when(antiguedades.bloquearDelPropietario(piezaId, propietarioId))
                .thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.abrirDecisionEnTaller(piezaId, propietarioId));
        verifyNoInteractions(catalogos);
    }
}
