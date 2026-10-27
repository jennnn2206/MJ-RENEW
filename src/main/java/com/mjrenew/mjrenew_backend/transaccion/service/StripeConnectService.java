
package com.mjrenew.mjrenew_backend.transaccion.service;

import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.transaccion.dto.StripeCuentaConectadaEstado;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.net.RequestOptions;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeConnectService {

    private static final Logger log =
            LoggerFactory.getLogger(StripeConnectService.class);

    private final String stripeSecretKey;
    private final String paisCuenta;
    private final String urlRefreshOnboarding;
    private final String urlRetornoOnboarding;

    public StripeConnectService(
            @Value("${stripe.secret-key}") String stripeSecretKey,
            @Value("${stripe.connect.country:MX}") String paisCuenta,
            @Value("${stripe.connect.refresh-url:http://localhost:8080/propietario?stripeOnboarding=refresh}")
            String urlRefreshOnboarding,
            @Value("${stripe.connect.return-url:http://localhost:8080/propietario?stripeOnboarding=return}")
            String urlRetornoOnboarding
    ) {
        this.stripeSecretKey = stripeSecretKey;
        this.paisCuenta = paisCuenta;
        this.urlRefreshOnboarding = urlRefreshOnboarding;
        this.urlRetornoOnboarding = urlRetornoOnboarding;
    }

    /**
     * Crea una cuenta Express para el propietario.
     */
    public String crearCuentaConectada(Usuario propietario) {

        AccountCreateParams params = AccountCreateParams.builder()
                .setType(AccountCreateParams.Type.EXPRESS)
                .setCountry(paisCuenta)
                .setEmail(propietario.getCorreoElectronicoUsuario())
                .setCapabilities(
                        AccountCreateParams.Capabilities.builder()
                                .setTransfers(
                                        AccountCreateParams.Capabilities.Transfers
                                                .builder()
                                                .setRequested(true)
                                                .build()
                                )
                                .build()
                )
                .putMetadata(
                        "mjrenewUsuarioId",
                        propietario.getUsuariosId().toString()
                )
                .build();

        try {
            Account cuenta = Account.create(
                    params,
                    opciones(
                            "mjrenew-connect-account-"
                                    + propietario.getUsuariosId()
                    )
            );

            if (cuenta.getId() == null || cuenta.getId().isBlank()) {
                throw new IllegalStateException(
                        "Stripe no devolvió el identificador de la cuenta conectada"
                );
            }

            return cuenta.getId();

        } catch (StripeException ex) {
            log.error(
                    "Stripe Connect no pudo crear una cuenta para usuarioId={}",
                    propietario.getUsuariosId(),
                    ex
            );

            throw new IllegalStateException(
                    "No fue posible crear la cuenta de pagos con Stripe",
                    ex
            );
        }
    }

    /**
     * Genera un enlace temporal para completar el onboarding.
     */
    public String crearEnlaceOnboarding(String stripeAccountId) {

        AccountLinkCreateParams params = AccountLinkCreateParams.builder()
                .setAccount(stripeAccountId)
                .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                .setRefreshUrl(urlRefreshOnboarding)
                .setReturnUrl(urlRetornoOnboarding)
                .build();

        try {
            AccountLink enlace = AccountLink.create(
                    params,
                    opciones(null)
            );

            if (enlace.getUrl() == null || enlace.getUrl().isBlank()) {
                throw new IllegalStateException(
                        "Stripe no devolvió el enlace de onboarding"
                );
            }

            return enlace.getUrl();

        } catch (StripeException ex) {
            log.error(
                    "Stripe Connect no pudo crear enlace de onboarding para accountId={}",
                    stripeAccountId,
                    ex
            );

            throw new IllegalStateException(
                    "No fue posible iniciar el registro de la cuenta con Stripe",
                    ex
            );
        }
    }

    /**
     * Consulta directamente a Stripe el estado de la cuenta conectada.
     * No confía en estados proporcionados por el frontend.
     */
    public StripeCuentaConectadaEstado consultarEstado(
            String stripeAccountId
    ) {
        try {
            Account cuenta = Account.retrieve(
                    stripeAccountId,
                    opciones(null)
            );

            // CORRECCIÓN: usar el tipo de capacidades del modelo Account.
            Account.Capabilities capacidades = cuenta.getCapabilities();

            String estadoTransfers = capacidades == null
                    ? null
                    : capacidades.getTransfers();

            return new StripeCuentaConectadaEstado(
                    Boolean.TRUE.equals(cuenta.getDetailsSubmitted()),
                    "active".equalsIgnoreCase(estadoTransfers),
                    Boolean.TRUE.equals(cuenta.getPayoutsEnabled())
            );

        } catch (StripeException ex) {
            log.error(
                    "Stripe Connect no pudo consultar accountId={}",
                    stripeAccountId,
                    ex
            );

            throw new IllegalStateException(
                    "No fue posible consultar el estado de la cuenta en Stripe",
                    ex
            );
        }
    }

    /**
     * Configura las opciones de la petición sin modificar Stripe.apiKey global.
     */
    private RequestOptions opciones(String idempotencyKey) {

        RequestOptions.RequestOptionsBuilder builder =
                RequestOptions.builder()
                        .setApiKey(stripeSecretKey);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            builder.setIdempotencyKey(idempotencyKey);
        }

        return builder.build();
    }
}
