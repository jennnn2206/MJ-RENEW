package com.mjrenew.mjrenew_backend.nucleo.auth;

import com.mjrenew.mjrenew_backend.nucleo.auth.service.AuthService;
import com.mjrenew.mjrenew_backend.nucleo.enums.TipoUsuario;
import com.mjrenew.mjrenew_backend.nucleo.exception.SolicitudInvalidaException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.dto.UsuarioRegistroRequest;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.nucleo.usuario.mapper.UsuarioMapper;
import com.mjrenew.mjrenew_backend.nucleo.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegistroTest {
    @Mock UsuarioRepository usuarios;
    @Mock UsuarioMapper mapper;
    @Mock PasswordEncoder encoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock SecurityContextRepository securityContextRepository;
    @InjectMocks AuthService servicio;

    @ParameterizedTest
    @EnumSource(value = TipoUsuario.class, names = {"ADMINISTRADOR", "TRANSPORTISTA"})
    void registroPublicoNoCreaRolesInternos(TipoUsuario rol) {
        var solicitud = new UsuarioRegistroRequest("Usuario Interno", "interno@test.mx", "Password123", rol);

        assertThrows(SolicitudInvalidaException.class, () -> servicio.registrar(solicitud));

        verifyNoInteractions(usuarios, mapper, encoder);
    }

    @Test
    void registroPublicoConservaRolPermitido() {
        var solicitud = new UsuarioRegistroRequest("Cliente Prueba", "cliente@test.mx", "Password123", TipoUsuario.COMPRADOR);
        var usuario = new Usuario();
        usuario.setTipoUsuario(TipoUsuario.COMPRADOR);
        when(mapper.toEntity(solicitud)).thenReturn(usuario);
        when(encoder.encode("Password123")).thenReturn("hash");
        when(usuarios.save(usuario)).thenReturn(usuario);

        servicio.registrar(solicitud);

        verify(usuarios).save(usuario);
        verify(encoder).encode("Password123");
    }
}
