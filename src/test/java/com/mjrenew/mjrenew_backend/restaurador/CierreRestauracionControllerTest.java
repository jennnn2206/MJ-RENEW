package com.mjrenew.mjrenew_backend.restaurador;

import com.mjrenew.mjrenew_backend.nucleo.enums.TipoUsuario;
import com.mjrenew.mjrenew_backend.nucleo.security.SecurityConfig;
import com.mjrenew.mjrenew_backend.nucleo.security.UsuarioPrincipal;
import com.mjrenew.mjrenew_backend.nucleo.usuario.entity.Usuario;
import com.mjrenew.mjrenew_backend.restaurador.controller.RestauradorController;
import com.mjrenew.mjrenew_backend.restaurador.service.CierreRestauracionService;
import com.mjrenew.mjrenew_backend.restaurador.service.RestauradorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RestauradorController.class)
@Import(SecurityConfig.class)
class CierreRestauracionControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean RestauradorService restauraciones;
    @MockitoBean CierreRestauracionService cierres;
    @MockitoBean com.mjrenew.mjrenew_backend.nucleo.usuario.repository.UsuarioRepository usuarios;
    private final UUID id = UUID.randomUUID();
    private final UUID restauradorId = UUID.randomUUID();

    @BeforeEach
    void sesionConCredencialesActuales() {
        when(usuarios.conservaCredencialesActivas(any(), any())).thenReturn(true);
    }

    @Test
    void multipartConCuatroFotosUsaLaIdentidadDeLaSesion() throws Exception {
        mvc.perform(solicitud(4).with(user(principal(TipoUsuario.RESTAURADOR))).with(csrf()))
                .andExpect(status().isOk());
        verify(cierres).cerrar(eq(id), eq(restauradorId), argThat(fotos -> fotos.size() == 4));
    }

    @Test
    void beanValidationRechazaMenosDeCuatroFotos() throws Exception {
        mvc.perform(solicitud(3).with(user(principal(TipoUsuario.RESTAURADOR))).with(csrf()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cierres);
    }

    @Test
    void beanValidationRechazaAusenciaDeFotos() throws Exception {
        mvc.perform(solicitud(0).with(user(principal(TipoUsuario.RESTAURADOR))).with(csrf()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cierres);
    }

    @Test
    void propietarioNoPuedeCerrarUnaRestauracion() throws Exception {
        mvc.perform(solicitud(4).with(user(principal(TipoUsuario.PROPIETARIO))).with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cierres);
    }

    @Test
    void escrituraSinCsrfSeRechaza() throws Exception {
        mvc.perform(solicitud(4).with(user(principal(TipoUsuario.RESTAURADOR))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cierres);
    }

    @Test
    void elCierreExigeMultipartEnLugarDelAntiguoJson() throws Exception {
        mvc.perform(post("/api/restaurador/marcarRestauracionLista/{antiguedadId}", id)
                        .contentType("application/json").content("{}")
                        .with(user(principal(TipoUsuario.RESTAURADOR))).with(csrf()))
                .andExpect(status().isUnsupportedMediaType());
        verifyNoInteractions(cierres);
    }

    private MockMultipartHttpServletRequestBuilder solicitud(int cantidad) {
        var peticion = multipart("/api/restaurador/marcarRestauracionLista/{antiguedadId}", id);
        for (int i = 0; i < cantidad; i++) {
            peticion.file(new MockMultipartFile("fotos", "foto-" + i + ".jpg", "image/jpeg", new byte[]{1}));
        }
        return peticion;
    }

    private UsuarioPrincipal principal(TipoUsuario rol) {
        Usuario usuario = new Usuario();
        usuario.setUsuariosId(restauradorId);
        usuario.setTipoUsuario(rol);
        usuario.setActivoUsuario(true);
        usuario.setCorreoVerificadoUsuario(true);
        usuario.setCorreoElectronicoUsuario("sesion@example.invalid");
        return new UsuarioPrincipal(usuario);
    }
}
