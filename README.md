MJ RENEW

- Ángela Avances - primer mapeo vista principal (03/09/2026)
- Ángela Avances - cambios en las rutas, aún falta corregir errores (04/09/2026)

# MJ Renew — Reorganización de arquitectura por roles

> Propuesta de reestructuración: de arquitectura por capas técnicas
> (`controller/`, `service/`, `dto/`...) a arquitectura híbrida por
> roles + núcleo compartido (`common/`).
>
> **Estado:** propuesta, aún no aplicada sobre el proyecto.

---

## Estructura completa

```
com.mjrenew.mjrenew_backend/
│
├── MjrenewBackendApplication.java
│
├── common/
│   ├── entity/
│   │   ├── Antiguedad.java
│   │   ├── AvanceRestauracion.java
│   │   ├── CatalogoAntiguedad.java
│   │   ├── Cotizacion.java
│   │   ├── Dimension.java
│   │   ├── DisputaAntiguedad.java
│   │   ├── FotografiaAntiguedad.java
│   │   ├── FotografiaReclamo.java
│   │   ├── TransaccionBancaria.java
│   │   ├── TrasladoAntiguedad.java
│   │   └── Usuario.java
│   │
│   ├── enums/
│   │   ├── DisponibilidadRestaurador.java
│   │   ├── EstadoAntiguedad.java
│   │   ├── EstadoCotizacion.java
│   │   ├── EstadoTransaccion.java
│   │   ├── EstiloMueble.java
│   │   ├── FlujoTransaccion.java
│   │   ├── MaterialMueble.java
│   │   ├── ResultadoTraslado.java
│   │   ├── TipoFotografia.java
│   │   ├── TipoMueble.java
│   │   ├── TipoTransaccion.java
│   │   ├── TipoTraslado.java
│   │   └── TipoUsuario.java
│   │
│   ├── state/
│   │   ├── EstadoAntiguedadBehavior.java
│   │   ├── EstadoAntiguedadBehaviorResolver.java
│   │   ├── FlujoActivoEstadoBehavior.java
│   │   ├── TerminalCanceladoEstadoBehavior.java
│   │   └── TerminalExitosoEstadoBehavior.java
│   │
│   ├── security/
│   │   ├── CustomUserDetailsService.java
│   │   ├── SecurityConfig.java
│   │   └── UsuarioPrincipal.java
│   │
│   ├── exception/
│   │   ├── CorreoYaRegistradoException.java
│   │   ├── CredencialesInvalidasException.java
│   │   ├── ErrorResponse.java
│   │   ├── GlobalExceptionHandler.java
│   │   └── TransicionEstadoInvalidaException.java
│   │
│   ├── repository/
│   │   ├── AntiguedadRepository.java
│   │   ├── AvanceRestauracionRepository.java
│   │   ├── CatalogoAntiguedadRepository.java
│   │   ├── CotizacionRepository.java
│   │   ├── DimensionRepository.java
│   │   ├── DisputaAntiguedadRepository.java
│   │   ├── FotografiaAntiguedadRepository.java
│   │   ├── FotografiaReclamoRepository.java
│   │   ├── TransaccionBancariaRepository.java
│   │   ├── TrasladoAntiguedadRepository.java
│   │   └── UsuarioRepository.java
│   │
│   ├── mapper/
│   │   ├── AntiguedadMapper.java
│   │   └── UsuarioMapper.java
│   │
│   ├── controller/
│   │   └── PageController.java
│   │
│   ├── dto/
│   │   └── antiguedad/
│   │       └── AntiguedadResponse.java
│   │
│   └── auth/
│       ├── controller/
│       │   └── AuthController.java
│       ├── service/
│       │   └── AuthService.java
│       └── dto/
│           ├── LoginRequest.java
│           ├── UsuarioRegistroRequest.java
│           └── UsuarioResponse.java
│
├── propietario/
│   ├── controller/
│   │   └── PropietarioController.java
│   ├── service/
│   │   └── PropietarioService.java
│   ├── dto/
│   │   └── AntiguedadCreateRequest.java
│   └── repository/
│       └── PerfilRestauradorRepository.java
│
├── restaurador/
│   ├── controller/
│   │   └── RestauradorController.java
│   ├── service/
│   │   └── RestauradorService.java
│   ├── dto/
│   │   ├── CotizarRequest.java            (pendiente de crear)
│   │   └── PublicarAvanceRequest.java     (pendiente de crear)
│   └── entity/
│       └── PerfilRestaurador.java
│
├── transportista/
│   ├── controller/
│   │   └── TransportistaController.java
│   ├── service/
│   │   └── TransportistaService.java
│   └── dto/
│       ├── ConfirmarRecoleccionRequest.java  (pendiente de crear)
│       └── ConfirmarEntregaRequest.java      (pendiente de crear)
│
├── comprador/
│   ├── controller/
│   │   └── CompradorController.java
│   ├── service/
│   │   └── CompradorService.java
│   └── dto/
│       └── SolicitarCompraRequest.java       (pendiente de crear)
│
└── administrador/
    ├── controller/
    │   └── AdministradorController.java
    ├── service/
    │   └── AdministradorService.java
    └── dto/                                  (pendiente de definir)
```

---

## Criterio usado para clasificar cada clase

- **`common/`** — todo lo que dos o más roles usan al mismo tiempo:
  entidades (una `Antiguedad` la tocan propietario, restaurador y
  transportista), la máquina de estados (es una sola, no una por
  rol), seguridad, excepciones, y el login/registro (ocurre *antes*
  de que exista un rol activo en sesión).
- **`propietario/` · `restaurador/` · `transportista/` · `comprador/` ·
  `administrador/`** — controller, service y DTOs de las acciones
  que solo ese rol puede ejecutar.

---

## Punto pendiente (no es solo mover archivos)

`AntiguedadController` y `AntiguedadService`, tal como existen hoy,
mezclan responsabilidades de propietario (`crear`, `listarMias`) con
un método genérico `cambiarEstado()` que acepta cualquier estado
destino sin restringir por rol.

Repartir esto por rol implica **dividir esa lógica en acciones
específicas**, no solo mover el archivo:

- `RestauradorController` → `evaluar()`, `calcularPresupuesto()`,
  `publicarAvance()`
- `PropietarioController` → `aceptarPresupuesto()`,
  `decidirDestino()`
- `TransportistaController` → `confirmarRecoleccion()`,
  `confirmarEntrega()`
- `CompradorController` → `solicitarCompra()`

---

## Orden sugerido de aplicación

1. Mover `common/` (solo cambia `package` + imports, riesgo bajo)
2. Armar `propietario/` completo (la lógica ya existe, solo se separa)
3. Armar `restaurador/` (aquí se escribe la validación de
   transiciones por acción)
4. Armar `transportista/`, `comprador/`, `administrador/`
