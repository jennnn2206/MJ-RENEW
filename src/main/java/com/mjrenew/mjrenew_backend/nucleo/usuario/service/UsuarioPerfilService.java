package com.mjrenew.mjrenew_backend.nucleo.usuario.service;

import com.mjrenew.mjrenew_backend.nucleo.exception.RecursoNoEncontradoException;
import com.mjrenew.mjrenew_backend.nucleo.usuario.dto.ActualizarDomicilioRequest;
import com.mjrenew.mjrenew_backend.nucleo.usuario.dto.DomicilioUsuarioResponse;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.nucleo.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UsuarioPerfilService {
    private final UsuarioRepository usuarios;

    public UsuarioPerfilService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public DomicilioUsuarioResponse obtenerMiDomicilio(UUID usuarioId) {
        Usuario usuario = buscarUsuario(usuarioId);
        return new DomicilioUsuarioResponse(usuario.getDireccionTextoUsuario());
    }

    @Transactional
    public DomicilioUsuarioResponse actualizarMiDomicilio(UUID usuarioId, ActualizarDomicilioRequest request) {
        Usuario usuario = buscarUsuario(usuarioId);
        String nuevaDireccion = request.direccionTextoUsuario().trim();
        if (!nuevaDireccion.equals(usuario.getDireccionTextoUsuario())) {
            // Las coordenadas previas dejarían de representar la nueva dirección.
            usuario.setLatitudUsuario(null);
            usuario.setLongitudUsuario(null);
        }
        usuario.setDireccionTextoUsuario(nuevaDireccion);
        Usuario actualizado = usuarios.save(usuario);
        return new DomicilioUsuarioResponse(actualizado.getDireccionTextoUsuario());
    }

    private Usuario buscarUsuario(UUID usuarioId) {
        return usuarios.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario autenticado"));
    }
}
