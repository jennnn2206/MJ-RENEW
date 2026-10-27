package com.mjrenew.mjrenew_backend.propietario;

import com.mjrenew.mjrenew_backend.nucleo.enums.TipoUsuario;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.propietario.dto.CuentaStripeEstadoResponse;
import com.mjrenew.mjrenew_backend.propietario.dto.StripeOnboardingResponse;
import com.mjrenew.mjrenew_backend.propietario.entity.CuentaStripeConectada;
import com.mjrenew.mjrenew_backend.propietario.repository.CuentaStripeConectadaRepository;
import com.mjrenew.mjrenew_backend.propietario.service.CuentaStripeConectadaService;
import com.mjrenew.mjrenew_backend.transaccion.dto.StripeCuentaConectadaEstado;
import com.mjrenew.mjrenew_backend.transaccion.service.StripeConnectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuentaStripeConectadaServiceTest {

    @Mock
    private CuentaStripeConectadaRepository cuentas;

    @Mock
    private StripeConnectService stripeConnect;

    private CuentaStripeConectadaService servicio;
    private Usuario propietario;

    @BeforeEach
    void preparar() {
        servicio = new CuentaStripeConectadaService(cuentas, stripeConnect);
        propietario = new Usuario();
        propietario.setUsuariosId(UUID.randomUUID());
        propietario.setCorreoElectronicoUsuario("propietario@example.test");
        propietario.setTipoUsuario(TipoUsuario.PROPIETARIO);
        propietario.setActivoUsuario(true);
    }

    @Test
    void creaCuentaUnaVezYDevuelveUrlDeOnboarding() {
        when(cuentas.findByUsuario_UsuariosId(propietario.getUsuariosId()))
                .thenReturn(Optional.empty());
        when(stripeConnect.crearCuentaConectada(propietario)).thenReturn("acct_test_123");
        when(stripeConnect.crearEnlaceOnboarding("acct_test_123"))
                .thenReturn("https://connect.stripe.test/onboarding");
        when(cuentas.saveAndFlush(any(CuentaStripeConectada.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        StripeOnboardingResponse respuesta = servicio.iniciarOnboarding(propietario);

        assertEquals("https://connect.stripe.test/onboarding", respuesta.urlOnboarding());
        verify(stripeConnect).crearCuentaConectada(propietario);
        verify(cuentas).saveAndFlush(argThat(cuenta ->
                cuenta.getUsuario() == propietario
                        && "acct_test_123".equals(cuenta.getStripeAccountId())
        ));
    }

    @Test
    void reutilizaLaCuentaExistenteYGeneraUnEnlaceNuevo() {
        CuentaStripeConectada existente = cuentaExistente();
        when(cuentas.findByUsuario_UsuariosId(propietario.getUsuariosId()))
                .thenReturn(Optional.of(existente));
        when(stripeConnect.crearEnlaceOnboarding("acct_test_existente"))
                .thenReturn("https://connect.stripe.test/nuevo-enlace");

        StripeOnboardingResponse respuesta = servicio.iniciarOnboarding(propietario);

        assertEquals("https://connect.stripe.test/nuevo-enlace", respuesta.urlOnboarding());
        verify(stripeConnect, never()).crearCuentaConectada(any());
        verify(cuentas, never()).saveAndFlush(any());
    }

    @Test
    void devuelveNoConfiguradaSinLlamarAStripeCuandoNoHayCuenta() {
        when(cuentas.findByUsuario_UsuariosId(propietario.getUsuariosId()))
                .thenReturn(Optional.empty());

        CuentaStripeEstadoResponse respuesta = servicio.obtenerEstado(propietario);

        assertFalse(respuesta.cuentaCreada());
        assertEquals("NO_CONFIGURADA", respuesta.estado());
        assertNull(respuesta.actualizadaEn());
        verifyNoInteractions(stripeConnect);
    }

    @Test
    void sincronizaElEstadoDesdeStripeYNoDesdeElCliente() {
        CuentaStripeConectada existente = cuentaExistente();
        when(cuentas.findByUsuario_UsuariosId(propietario.getUsuariosId()))
                .thenReturn(Optional.of(existente));
        when(stripeConnect.consultarEstado("acct_test_existente"))
                .thenReturn(new StripeCuentaConectadaEstado(true, true, true));
        when(cuentas.saveAndFlush(existente)).thenReturn(existente);

        CuentaStripeEstadoResponse respuesta = servicio.obtenerEstado(propietario);

        assertTrue(respuesta.cuentaCreada());
        assertTrue(respuesta.detallesEnviados());
        assertTrue(respuesta.transferenciasHabilitadas());
        assertTrue(respuesta.payoutsHabilitados());
        assertEquals("LISTA_PARA_RECIBIR_FONDOS", respuesta.estado());
        verify(stripeConnect).consultarEstado("acct_test_existente");
        verify(cuentas).saveAndFlush(existente);
    }

    @Test
    void rechazaUnUsuarioQueNoSeaPropietario() {
        propietario.setTipoUsuario(TipoUsuario.COMPRADOR);

        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.iniciarOnboarding(propietario));
        verifyNoInteractions(cuentas, stripeConnect);
    }

    private CuentaStripeConectada cuentaExistente() {
        CuentaStripeConectada cuenta = new CuentaStripeConectada();
        cuenta.setCuentaStripeConectadaId(UUID.randomUUID());
        cuenta.setUsuario(propietario);
        cuenta.setStripeAccountId("acct_test_existente");
        return cuenta;
    }
}
