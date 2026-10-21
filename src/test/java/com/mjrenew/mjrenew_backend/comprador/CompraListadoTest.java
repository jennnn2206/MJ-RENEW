package com.mjrenew.mjrenew_backend.comprador;

import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import com.mjrenew.mjrenew_backend.catalogo.repository.CatalogoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.comprador.mapper.CompraMapper;
import com.mjrenew.mjrenew_backend.comprador.service.CompraService;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.ResultadoTraslado;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoFotografia;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTraslado;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.transportista.entity.TrasladoAntiguedad;
import com.mjrenew.mjrenew_backend.transportista.repository.TrasladoAntiguedadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompraListadoTest {
    @Mock CatalogoAntiguedadRepository catalogos;
    @Mock TrasladoAntiguedadRepository traslados;
    @Mock FotografiaAntiguedadRepository fotografias;
    @Mock CompraMapper mapper;
    @InjectMocks CompraService servicio;

    private final UUID compradorId = UUID.randomUUID();
    private final UUID antiguedadId = UUID.randomUUID();

    private CatalogoAntiguedad ventaPagada() {
        Antiguedad antiguedad = new Antiguedad();
        antiguedad.setAntiguedadesId(antiguedadId);
        CatalogoAntiguedad catalogo = new CatalogoAntiguedad();
        catalogo.setCatalogoAntiguedadId(UUID.randomUUID());
        catalogo.setAntiguedad(antiguedad);
        catalogo.setCompletadaEnCatalogo(OffsetDateTime.now());
        return catalogo;
    }

    private void prepararPagina(CatalogoAntiguedad catalogo) {
        var pageable = PageRequest.of(0, 10);
        when(catalogos.findByComprador_UsuariosIdAndCompletadaEnCatalogoIsNotNull(compradorId, pageable))
                .thenReturn(new PageImpl<>(List.of(catalogo)));
        when(fotografias.findByAntiguedad_AntiguedadesIdInAndTipoFotografiaOrderBySubidaEnFotografiaAsc(
                anyCollection(), eq(TipoFotografia.ESTADO_INICIAL))).thenReturn(List.of());
    }

    @Test
    void paginaVaciaNoConsultaTrasladosNiFotos() {
        var pageable = PageRequest.of(0, 10);
        when(catalogos.findByComprador_UsuariosIdAndCompletadaEnCatalogoIsNotNull(compradorId, pageable))
                .thenReturn(new PageImpl<>(List.of()));
        assertTrue(servicio.obtenerMisCompras(compradorId, pageable).isEmpty());
        verifyNoInteractions(traslados, fotografias);
    }

    @Test
    void noPermiteConfirmarSiAunNoSeAgendoEntrega() {
        CatalogoAntiguedad catalogo = ventaPagada();
        prepararPagina(catalogo);
        when(traslados.findByAntiguedad_AntiguedadesIdInAndTipoTrasladoOrderByAgendadoEnTrasladoDesc(
                anyCollection(), eq(TipoTraslado.ENTREGA_COMPRADOR))).thenReturn(List.of());

        servicio.obtenerMisCompras(compradorId, PageRequest.of(0, 10));
        verify(mapper).toResumen(same(catalogo), isNull(), isNull(), eq(false));
    }

    @Test
    void habilitaAcuseSoloConEntregaExitosaYCuatroFotos() {
        CatalogoAntiguedad catalogo = ventaPagada();
        prepararPagina(catalogo);
        TrasladoAntiguedad entrega = new TrasladoAntiguedad();
        entrega.setTrasladosAntiguedadId(UUID.randomUUID());
        entrega.setAntiguedad(catalogo.getAntiguedad());
        entrega.setResultadoTraslado(ResultadoTraslado.EXITOSO);
        when(traslados.findByAntiguedad_AntiguedadesIdInAndTipoTrasladoOrderByAgendadoEnTrasladoDesc(
                anyCollection(), eq(TipoTraslado.ENTREGA_COMPRADOR))).thenReturn(List.of(entrega));
        var conteo = mock(FotografiaAntiguedadRepository.EvidenciasTraslado.class);
        when(conteo.getTrasladoId()).thenReturn(entrega.getTrasladosAntiguedadId());
        when(conteo.getTotal()).thenReturn(4L);
        when(fotografias.contarEvidenciasPorTraslado(anyCollection(), eq(TipoFotografia.TRASLADO_LLEGADA)))
                .thenReturn(List.of(conteo));

        servicio.obtenerMisCompras(compradorId, PageRequest.of(0, 10));
        verify(mapper).toResumen(same(catalogo), same(entrega), isNull(), eq(true));
    }

    @Test
    void confirmacionAnteriorNuncaVuelveAHabilitarBoton() {
        CatalogoAntiguedad catalogo = ventaPagada();
        catalogo.setRecepcionConfirmadaEnCatalogo(OffsetDateTime.now());
        prepararPagina(catalogo);
        TrasladoAntiguedad entrega = new TrasladoAntiguedad();
        entrega.setTrasladosAntiguedadId(UUID.randomUUID());
        entrega.setAntiguedad(catalogo.getAntiguedad());
        entrega.setResultadoTraslado(ResultadoTraslado.EXITOSO);
        when(traslados.findByAntiguedad_AntiguedadesIdInAndTipoTrasladoOrderByAgendadoEnTrasladoDesc(
                anyCollection(), eq(TipoTraslado.ENTREGA_COMPRADOR))).thenReturn(List.of(entrega));
        var conteo = mock(FotografiaAntiguedadRepository.EvidenciasTraslado.class);
        when(conteo.getTrasladoId()).thenReturn(entrega.getTrasladosAntiguedadId());
        when(conteo.getTotal()).thenReturn(4L);
        when(fotografias.contarEvidenciasPorTraslado(anyCollection(), eq(TipoFotografia.TRASLADO_LLEGADA)))
                .thenReturn(List.of(conteo));

        servicio.obtenerMisCompras(compradorId, PageRequest.of(0, 10));
        verify(mapper).toResumen(same(catalogo), same(entrega), isNull(), eq(false));
    }
}
