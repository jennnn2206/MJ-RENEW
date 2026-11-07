package com.mjrenew.mjrenew_backend.catalogo;

import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import com.mjrenew.mjrenew_backend.catalogo.mapper.CatalogoAntiguedadMapper;
import com.mjrenew.mjrenew_backend.catalogo.repository.CatalogoAntiguedadRepository;
import com.mjrenew.mjrenew_backend.catalogo.service.CatalogoAntiguedadService;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.propietario.repository.DimensionRepository;
import com.mjrenew.mjrenew_backend.propietario.repository.FotografiaAntiguedadRepository;
import com.mjrenew.mjrenew_backend.transaccion.repository.TransaccionBancariaRepository;
import com.mjrenew.mjrenew_backend.transaccion.service.StripeCheckoutService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoDemoCompraTest {

    @Mock CatalogoAntiguedadRepository catalogoRepository;
    @Mock FotografiaAntiguedadRepository fotografiaRepository;
    @Mock DimensionRepository dimensionRepository;
    @Mock TransaccionBancariaRepository transaccionRepository;
    @Mock CatalogoAntiguedadMapper catalogoMapper;
    @Mock StripeCheckoutService stripeCheckoutService;
    @InjectMocks CatalogoAntiguedadService service;

    @Test
    void piezaDeDemostracionNoIniciaCobro() {
        UUID catalogoId = UUID.randomUUID();
        Antiguedad pieza = new Antiguedad();
        pieza.setEstadoActualAntiguedad(EstadoAntiguedad.EN_CATALOGO);
        pieza.setProcedenciaMueble("DEMO: escritorio colonial");
        CatalogoAntiguedad catalogo = new CatalogoAntiguedad();
        catalogo.setAntiguedad(pieza);
        catalogo.setActivaCatalogo(true);
        when(catalogoRepository.buscarParaCompra(catalogoId)).thenReturn(Optional.of(catalogo));

        assertThrows(SolicitudInvalidaException.class,
                () -> service.iniciarCompra(catalogoId, new Usuario()));
        verifyNoInteractions(transaccionRepository, stripeCheckoutService);
    }
}
