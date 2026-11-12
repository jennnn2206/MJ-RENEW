package com.mjrenew.mjrenew_backend.nucleo.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mjrenew.mjrenew_backend.nucleo.auth.controller.AuthController;
import com.mjrenew.mjrenew_backend.nucleo.auth.service.AuthService;
import com.mjrenew.mjrenew_backend.nucleo.security.SecurityConfig;
import com.mjrenew.mjrenew_backend.nucleo.usuario.mapper.UsuarioMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class SeguridadCsrfTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean AuthService authService;
    @MockitoBean UsuarioMapper usuarioMapper;

    @Test
    void escrituraRequiereTokenDeLaMismaSesion() throws Exception {
        var respuestaCsrf = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        var sesion = (MockHttpSession) respuestaCsrf.getRequest().getSession(false);
        var token = json.readTree(respuestaCsrf.getResponse().getContentAsString());
        var registro = """
                {"nombreCompleto":"Cliente Prueba","correoElectronico":"cliente@test.mx",
                 "contrasena":"Password123","tipoUsuario":"COMPRADOR"}
                """;

        mvc.perform(post("/api/auth/registrarUsuario")
                        .session(sesion).contentType(MediaType.APPLICATION_JSON).content(registro))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/auth/registrarUsuario")
                        .header(token.get("headerName").asText(), token.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON).content(registro))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/auth/registrarUsuario")
                        .session(sesion)
                        .header(token.get("headerName").asText(), token.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON).content(registro))
                .andExpect(status().isCreated());
    }
}
