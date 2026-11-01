
package com.mjrenew.mjrenew_backend.transaccion;

import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.transaccion.dto.DesgloseComisionVenta;
import com.mjrenew.mjrenew_backend.transaccion.service.ComisionVentaCatalogoService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ComisionVentaCatalogoServiceTest {

    private final ComisionVentaCatalogoService servicio =
            new ComisionVentaCatalogoService();

    @Test
    void calculaComisionDelSietePorCiento() {

        DesgloseComisionVenta resultado =
                servicio.calcular(new BigDecimal("6500.00"));

        assertEquals(
                new BigDecimal("6500.00"),
                resultado.montoBrutoMxn()
        );

        assertEquals(
                new BigDecimal("455.00"),
                resultado.comisionMjrenewMxn()
        );

        assertEquals(
                new BigDecimal("6045.00"),
                resultado.montoRestanteAntesCargoStripeMxn()
        );
    }

    @Test
    void redondeaLaComisionADosDecimales() {

        DesgloseComisionVenta resultado =
                servicio.calcular(new BigDecimal("99.99"));

        assertEquals(
                new BigDecimal("7.00"),
                resultado.comisionMjrenewMxn()
        );

        assertEquals(
                new BigDecimal("92.99"),
                resultado.montoRestanteAntesCargoStripeMxn()
        );
    }

    @Test
    void rechazaUnMontoNulo() {

        assertThrows(
                SolicitudInvalidaException.class,
                () -> servicio.calcular(null)
        );
    }

    @Test
    void rechazaUnMontoCero() {

        assertThrows(
                SolicitudInvalidaException.class,
                () -> servicio.calcular(new BigDecimal("0.00"))
        );
    }

    @Test
    void rechazaUnMontoNegativo() {

        assertThrows(
                SolicitudInvalidaException.class,
                () -> servicio.calcular(new BigDecimal("-100.00"))
        );
    }
}
