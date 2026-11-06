package com.mjrenew.mjrenew_backend.nucleo.usuario;

import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.dto.ActualizarDomicilioRequest;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.nucleo.usuario.repository.UsuarioRepository;
import com.mjrenew.mjrenew_backend.nucleo.usuario.service.UsuarioPerfilService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioPerfilServiceTest {
    @Mock UsuarioRepository usuarios;
    @InjectMocks UsuarioPerfilService servicio;

    @Test
    void guardaDomicilioLimpioDelUsuarioAutenticado() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setUsuariosId(usuarioId);
        usuario.setLatitudUsuario(new java.math.BigDecimal("20.9674"));
        usuario.setLongitudUsuario(new java.math.BigDecimal("-89.6237"));
        when(usuarios.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(usuarios.save(usuario)).thenReturn(usuario);

        var respuesta = servicio.actualizarMiDomicilio(usuarioId,
                new ActualizarDomicilioRequest("  Calle 123, Mérida  "));

        assertEquals("Calle 123, Mérida", respuesta.direccionTextoUsuario());
        assertNull(usuario.getLatitudUsuario(), "Las coordenadas anteriores no deben quedar asociadas a otra dirección");
        assertNull(usuario.getLongitudUsuario(), "Las coordenadas anteriores no deben quedar asociadas a otra dirección");
        verify(usuarios).save(usuario);
    }

    @Test
    void noPermiteConsultarUnUsuarioInexistente() {
        UUID usuarioId = UUID.randomUUID();
        when(usuarios.findById(usuarioId)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtenerMiDomicilio(usuarioId));
    }
}
