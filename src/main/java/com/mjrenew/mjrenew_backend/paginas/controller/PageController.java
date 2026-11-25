package com.mjrenew.mjrenew_backend.paginas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String inicio() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "pages/login";
    }

    @GetMapping("/registro")
    public String registro() {
        return "pages/registro";
    }

    @GetMapping("/verificar-correo")
    public String verificarCorreo() {
        return "pages/verificar-correo";
    }

    @GetMapping("/recuperar-contrasena")
    public String recuperarContrasena() { return "pages/recuperar-contrasena"; }

    @GetMapping("/restablecer-contrasena")
    public String restablecerContrasena() { return "pages/restablecer-contrasena"; }

    @GetMapping("/catalogo")
    public String catalogo() {
        return "pages/catalogo";
    }

    @GetMapping("/pieza")
    public String pieza() {
        return "pages/pieza";
    }

    @GetMapping("/comprador")
    public String comprador() {
        return "pages/comprador";
    }

    @GetMapping("/propietario")
    public String propietario() {
        return "pages/propietario";
    }

    @GetMapping("/restaurador")
    public String restaurador() {
        return "pages/restaurador";
    }

    @GetMapping("/transportista")
    public String transportista() {
        return "pages/transportista";
    }

    @GetMapping("/administrador")
    public String administrador() {
        return "pages/administrador";
    }
}
