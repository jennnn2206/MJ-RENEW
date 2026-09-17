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
}