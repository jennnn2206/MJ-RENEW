package com.mjrenew.mjrenew_backend.propietario.service;

import com.mjrenew.mjrenew_backend.nucleo.enums.TipoUsuario;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.propietario.dto.CuentaStripeEstadoResponse;
import com.mjrenew.mjrenew_backend.propietario.dto.StripeOnboardingResponse;
import com.mjrenew.mjrenew_backend.propietario.entity.CuentaStripeConectada;
import com.mjrenew.mjrenew_backend.propietario.repository.CuentaStripeConectadaRepository;
import com.mjrenew.mjrenew_backend.transaccion.dto.StripeCuentaConectadaEstado;
import com.mjrenew.mjrenew_backend.transaccion.service.StripeConnectService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Casos de uso de Stripe Connect correspondientes al rol propietario. */
@Service
public class CuentaStripeConectadaService {

    private final CuentaStripeConectadaRepository cuentas;
    private final StripeConnectService stripeConnect;

    public CuentaStripeConectadaService(
            CuentaStripeConectadaRepository cuentas,
            StripeConnectService stripeConnect
    ) {
        this.cuentas = cuentas;
        this.stripeConnect = stripeConnect;
    }

    /** Crea la cuenta una sola vez y genera un enlace nuevo para continuar el onboarding. */
    public StripeOnboardingResponse iniciarOnboarding(Usuario propietario) {
        validarPropietario(propietario);
        CuentaStripeConectada cuenta = obtenerOCrearCuenta(propietario);
        String url = stripeConnect.crearEnlaceOnboarding(cuenta.getStripeAccountId());
        return new StripeOnboardingResponse(url);
    }

    /** Devuelve un estado actualizado consultando Stripe, no datos controlados por el frontend. */
    public CuentaStripeEstadoResponse obtenerEstado(Usuario propietario) {
        validarPropietario(propietario);
        Optional<CuentaStripeConectada> encontrada = cuentas.findByUsuario_UsuariosId(propietario.getUsuariosId());
        if (encontrada.isEmpty()) {
            return new CuentaStripeEstadoResponse(
                    false, false, false, false, "NO_CONFIGURADA", null
            );
        }

        CuentaStripeConectada cuenta = encontrada.get();
        StripeCuentaConectadaEstado estadoStripe = stripeConnect.consultarEstado(cuenta.getStripeAccountId());
        cuenta.setDetallesEnviados(estadoStripe.detallesEnviados());
        cuenta.setTransferenciasHabilitadas(estadoStripe.transferenciasHabilitadas());
        cuenta.setPayoutsHabilitados(estadoStripe.payoutsHabilitados());
        CuentaStripeConectada actualizada = cuentas.saveAndFlush(cuenta);

        return respuestaEstado(actualizada);
    }

    private CuentaStripeConectada obtenerOCrearCuenta(Usuario propietario) {
        Optional<CuentaStripeConectada> encontrada = cuentas.findByUsuario_UsuariosId(propietario.getUsuariosId());
        if (encontrada.isPresent()) {
            return encontrada.get();
        }

        // La llamada externa ocurre antes de guardar, sin mantener abierta una transacción SQL.
        String stripeAccountId = stripeConnect.crearCuentaConectada(propietario);
        CuentaStripeConectada nueva = new CuentaStripeConectada();
        nueva.setUsuario(propietario);
        nueva.setStripeAccountId(stripeAccountId);

        try {
            return cuentas.saveAndFlush(nueva);
        } catch (DataIntegrityViolationException ex) {
            // Dos solicitudes simultáneas usan la misma idempotency key de Stripe.
            // Si una ganó la restricción UNIQUE, reutilizamos la fila ya persistida.
            return cuentas.findByUsuario_UsuariosId(propietario.getUsuariosId())
                    .orElseThrow(() -> ex);
        }
    }

    private CuentaStripeEstadoResponse respuestaEstado(CuentaStripeConectada cuenta) {
        String estado;
        if (cuenta.isTransferenciasHabilitadas() && cuenta.isPayoutsHabilitados()) {
            estado = "LISTA_PARA_RECIBIR_FONDOS";
        } else if (cuenta.isTransferenciasHabilitadas()) {
            estado = "TRANSFERENCIAS_HABILITADAS_PAYOUTS_PENDIENTES";
        } else if (cuenta.isDetallesEnviados()) {
            estado = "REVISION_O_REQUISITOS_PENDIENTES";
        } else {
            estado = "ONBOARDING_PENDIENTE";
        }

        return new CuentaStripeEstadoResponse(
                true,
                cuenta.isDetallesEnviados(),
                cuenta.isTransferenciasHabilitadas(),
                cuenta.isPayoutsHabilitados(),
                estado,
                cuenta.getActualizadaEn()
        );
    }

    private void validarPropietario(Usuario usuario) {
        if (usuario == null || usuario.getUsuariosId() == null) {
            throw new SolicitudInvalidaException("No se pudo identificar al usuario autenticado");
        }
        if (usuario.getTipoUsuario() != TipoUsuario.PROPIETARIO) {
            throw new SolicitudInvalidaException("Solo un propietario puede configurar una cuenta de cobros");
        }
        if (!Boolean.TRUE.equals(usuario.getActivoUsuario())) {
            throw new SolicitudInvalidaException("La cuenta de usuario no está activa");
        }
    }
}
