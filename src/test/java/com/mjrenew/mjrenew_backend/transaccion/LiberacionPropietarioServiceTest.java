package com.mjrenew.mjrenew_backend.transaccion;

import com.mjrenew.mjrenew_backend.catalogo.entity.CatalogoAntiguedad;
import com.mjrenew.mjrenew_backend.nucleo.antiguedad.entity.Antiguedad;
import com.mjrenew.mjrenew_backend.nucleo.enums.EstadoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.enums.FlujoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoTransaccion;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.transaccion.entity.TransaccionBancaria;
import com.mjrenew.mjrenew_backend.transaccion.repository.TransaccionBancariaRepository;
import com.mjrenew.mjrenew_backend.transaccion.service.LiberacionPropietarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiberacionPropietarioServiceTest {
    @Mock TransaccionBancariaRepository transacciones;

    private LiberacionPropietarioService servicio;
    private TransaccionBancaria pagoVenta;
    private CatalogoAntiguedad catalogo;
    private Antiguedad antiguedad;
    private Usuario comprador;
    private Usuario propietario;

    @BeforeEach
    void preparar() {
        servicio = new LiberacionPropietarioService(transacciones);

        comprador = usuario();
        propietario = usuario();
        antiguedad = new Antiguedad();
        antiguedad.setAntiguedadesId(UUID.randomUUID());
        antiguedad.setPropietario(propietario);

        catalogo = new CatalogoAntiguedad();
        catalogo.setCatalogoAntiguedadId(UUID.randomUUID());
        catalogo.setAntiguedad(antiguedad);
        catalogo.setComprador(comprador);

        pagoVenta = new TransaccionBancaria();
        pagoVenta.setTransaccionId(UUID.randomUUID());
        pagoVenta.setTipoTransaccion(TipoTransaccion.PAGO_VENTA);
        pagoVenta.setEstadoTransaccion(EstadoTransaccion.EXITOSA);
        pagoVenta.setOriginator(comprador);
        pagoVenta.setBeneficiario(propietario);
        pagoVenta.setAntiguedad(antiguedad);
        pagoVenta.setCatalogoAntiguedad(catalogo);
        pagoVenta.setMontoBrutoMxnTransaccion(new BigDecimal("6500.00"));
    }

    @Test
    void creaRegistroPendienteVinculadoAlPagoOriginal() {
        when(transacciones.findByTransaccionPadre_TransaccionIdAndTipoTransaccion(
                pagoVenta.getTransaccionId(), TipoTransaccion.LIBERACION_PROPIETARIO))
                .thenReturn(Optional.empty());
        when(transacciones.save(any(TransaccionBancaria.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        TransaccionBancaria liberacion = servicio.registrarPendiente(pagoVenta);

        assertEquals(TipoTransaccion.LIBERACION_PROPIETARIO, liberacion.getTipoTransaccion());
        assertEquals(FlujoTransaccion.EGRESO, liberacion.getFlujoMjrenew());
        assertEquals(EstadoTransaccion.PENDIENTE, liberacion.getEstadoTransaccion());
        assertEquals(pagoVenta, liberacion.getTransaccionPadre());
        assertEquals(comprador, liberacion.getOriginator());
        assertEquals(propietario, liberacion.getBeneficiario());
        assertEquals(catalogo, liberacion.getCatalogoAntiguedad());
        assertEquals(new BigDecimal("6500.00"), liberacion.getMontoBrutoMxnTransaccion());
        assertNotNull(liberacion.getCreadaEnTransaccion());
        assertNull(liberacion.getReferenciaStripeTransaccion());
        assertNull(liberacion.getProcesadaEnTransaccion());
        verify(transacciones).save(liberacion);
    }

    @Test
    void noDuplicaLiberacionSiYaExiste() {
        TransaccionBancaria existente = new TransaccionBancaria();
        existente.setTipoTransaccion(TipoTransaccion.LIBERACION_PROPIETARIO);
        existente.setEstadoTransaccion(EstadoTransaccion.PENDIENTE);
        when(transacciones.findByTransaccionPadre_TransaccionIdAndTipoTransaccion(
                pagoVenta.getTransaccionId(), TipoTransaccion.LIBERACION_PROPIETARIO))
                .thenReturn(Optional.of(existente));

        assertSame(existente, servicio.registrarPendiente(pagoVenta));
        verify(transacciones, never()).save(any(TransaccionBancaria.class));
    }

    @Test
    void rechazaPagoDeRestauracion() {
        pagoVenta.setTipoTransaccion(TipoTransaccion.PAGO_RESTAURACION);
        assertThrows(SolicitudInvalidaException.class, () -> servicio.registrarPendiente(pagoVenta));
        verifyNoInteractions(transacciones);
    }

    @Test
    void rechazaPagoAunPendiente() {
        pagoVenta.setEstadoTransaccion(EstadoTransaccion.PENDIENTE);
        assertThrows(SolicitudInvalidaException.class, () -> servicio.registrarPendiente(pagoVenta));
        verifyNoInteractions(transacciones);
    }

    @Test
    void rechazaBeneficiarioQueNoEsElPropietarioDeLaPublicacion() {
        pagoVenta.setBeneficiario(usuario());
        assertThrows(SolicitudInvalidaException.class, () -> servicio.registrarPendiente(pagoVenta));
        verifyNoInteractions(transacciones);
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setUsuariosId(UUID.randomUUID());
        return usuario;
    }
}
