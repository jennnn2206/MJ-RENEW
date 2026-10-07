MJ RENEW

- Ángela Avances - primer mapeo vista principal (03/09/2026)
- Ángela Avances - cambios en las rutas, aún falta corregir errores (04/09/2026)

# MJ Renew — Reorganización de arquitectura por roles

```text 

com.mjrenew.mjrenew_backend
│
├── nucleo/                                  (compartido por todos los roles)
│   ├── usuario/
│   │   ├── entity/Usuario.java
│   │   ├── repository/UsuarioRepository.java
│   │   ├── mapper/UsuarioMapper.java
│   │   └── dto/ (UsuarioRegistroRequest.java, LoginRequest.java, UsuarioResponse.java)
│   ├── antiguedad/
│   │   ├── entity/Antiguedad.java
│   │   └── repository/AntiguedadRepository.java
│   ├── enums/          (sin cambio)
│   ├── seguridad/       (SecurityConfig, CustomUserDetailsService, UsuarioPrincipal)
│   ├── excepcion/       (GlobalExceptionHandler, ErrorResponse, y las 4 excepciones)
│   └── auth/
│       ├── controller/AuthController.java
│       └── service/AuthService.java
│
├── propietario/                              (antes "piezas")
│   ├── controller/AntiguedadController.java
│   ├── service/AntiguedadService.java
│   ├── mapper/AntiguedadMapper.java
│   ├── dto/ (AntiguedadCreateRequest.java, AntiguedadResponse.java, CambiarEstadoAntiguedadRequest.java)
│   ├── entity/ (Dimension.java, FotografiaAntiguedad.java)
│   ├── repository/ (DimensionRepository.java, FotografiaAntiguedadRepository.java)
│   └── estado/ (EstadoAntiguedadBehavior, Resolver, FlujoActivo, TerminalExitoso, TerminalCancelado)
│
├── restaurador/                              (antes "restauracion")
│   ├── controller/ (pendiente)
│   ├── service/ (pendiente)
│   ├── dto/ (pendiente)
│   ├── entity/ (PerfilRestaurador.java, Cotizacion.java, AvanceRestauracion.java)
│   └── repository/ (PerfilRestauradorRepository.java, CotizacionRepository.java, AvanceRestauracionRepository.java)
│
├── transportista/                            (antes "traslado")
│   ├── controller/ (pendiente)
│   ├── service/ (pendiente)
│   ├── dto/ (pendiente)
│   ├── entity/TrasladoAntiguedad.java
│   └── repository/TrasladoAntiguedadRepository.java
│
├── catalogo/                                 (cruce propietario + comprador)
│   ├── controller/ (pendiente)
│   ├── service/ (pendiente)
│   ├── dto/ (pendiente)
│   ├── entity/CatalogoAntiguedad.java
│   └── repository/CatalogoAntiguedadRepository.java
│
├── administrador/                            (antes "disputa")
│   ├── controller/ (pendiente)
│   ├── service/ (pendiente)
│   ├── dto/ (pendiente)
│   ├── entity/ (DisputaAntiguedad.java, FotografiaReclamo.java)
│   └── repository/ (DisputaAntiguedadRepository.java, FotografiaReclamoRepository.java)
│
├── transaccion/                               (cruza los 5 roles — no se renombra a un rol)
│   ├── entity/TransaccionBancaria.java
│   └── repository/TransaccionBancariaRepository.java
│
└── paginas/
└── controller/PageController.java

```