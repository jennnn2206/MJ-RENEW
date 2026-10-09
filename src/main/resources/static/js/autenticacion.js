// MJRENEW-FLUJO: Spring Security es la fuente de verdad. localStorage solo acelera la UI.
// Nunca usar el nombre/correo guardados en otro navegador para decidir a qué usuario pertenece una antigüedad.
const Auth = {
    async login(email, password) {
        try {
            const res = await fetch('/api/auth/iniciarSesion', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'same-origin',
                body: JSON.stringify({ correoElectronico: email, contrasena: password }),
            });
            if (!res.ok) return { success: false, error: await extraerError(res, 'Correo o contraseña incorrectos.') };
            const user = guardarSesionLocal(await res.json());
            return { success: true, user };
        } catch {
            return { success: false, error: 'No se pudo conectar con el servidor. Intenta de nuevo.' };
        }
    },

    async register(name, email, password, tipo) {
        try {
            const res = await fetch('/api/auth/registrarUsuario', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'same-origin',
                body: JSON.stringify({ nombreCompleto: name, correoElectronico: email, contrasena: password, tipoUsuario: tipo.toUpperCase() }),
            });
            if (!res.ok) return { success: false, error: await extraerError(res, 'No se pudo crear la cuenta.') };
            // TODO ERS RF-002: sustituir inicio automático por verificación real de correo.
            return this.login(email, password);
        } catch {
            return { success: false, error: 'No se pudo conectar con el servidor. Intenta de nuevo.' };
        }
    },

    async logout() {
        // MJRENEW-FLUJO: esperar la invalidación de JSESSIONID antes de redirigir.
        try {
            await fetch('/api/auth/cerrarSesion', { method: 'POST', credentials: 'same-origin' });
        } finally {
            localStorage.removeItem('mjrenew_user');
        }
    },

    getCurrentUser() {
        try { return JSON.parse(localStorage.getItem('mjrenew_user')); }
        catch { return null; }
    },

    async actualizarDesdeServidor() {
        try {
            const res = await fetch('/api/auth/obtenerUsuarioActual', { credentials: 'same-origin', cache: 'no-store' });
            if (!res.ok) {
                localStorage.removeItem('mjrenew_user');
                return null;
            }
            return guardarSesionLocal(await res.json());
        } catch {
            // Ante errores de red no se considera una sesión validada.
            return null;
        }
    },

    requireAuth(redirectTo = '/login', rolEsperado = null) {
        const cached = this.getCurrentUser();
        // No se renderiza información de usuario previo sin esperar a la comprobación.
        this.actualizarDesdeServidor().then(user => {
            if (!user) { window.location.replace(redirectTo); return; }
            if (rolEsperado && user.tipo !== rolEsperado) {
                window.location.replace({ propietario:'/propietario', restaurador:'/restaurador', comprador:'/comprador' }[user.tipo] || '/login');
                return;
            }
            // Datos de la cabecera actuales, si existían antes de validar la sesión.
            if (!cached || cached.email !== user.email || cached.tipo !== user.tipo || cached.id !== user.id || cached.name !== user.name) window.location.reload();
        });
        if (!cached || (rolEsperado && cached.tipo !== rolEsperado)) {
            // No aceptar sesión almacenada de otro rol como autenticación.
            return null;
        }
        return cached;
    },

    async redirectIfLoggedIn() {
        const user = await this.actualizarDesdeServidor();
        if (!user) return;
        window.location.replace({ propietario:'/propietario', restaurador:'/restaurador', comprador:'/comprador' }[user.tipo] || '/login');
    },
};

function guardarSesionLocal(usuarioResponse) {
    const user = {
        id: usuarioResponse.id,
        name: usuarioResponse.nombreCompleto,
        email: usuarioResponse.correoElectronico,
        tipo: usuarioResponse.tipoUsuario.toLowerCase(),
    };
    localStorage.setItem('mjrenew_user', JSON.stringify(user));
    return user;
}

async function extraerError(res, mensajePorDefecto) {
    try {
        const body = await res.json();
        if (Array.isArray(body.detalles) && body.detalles.length) return body.detalles[0];
        if (body.mensaje) return body.mensaje;
    } catch { /* no JSON */ }
    return mensajePorDefecto;
}
