
// Logica: sesion real contra la API del backend (/api/auth/*)

const Auth = {
    async login(email, password) {
        try {
            const res = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ correoElectronico: email, contrasena: password }),
            });
            if (!res.ok) {
                return { success: false, error: await extraerError(res, 'Correo o contraseña incorrectos.') };
            }
            const data = await res.json();
            const user = guardarSesionLocal(data);
            return { success: true, user };
        } catch {
            return { success: false, error: 'No se pudo conectar con el servidor. Intenta de nuevo.' };
        }
    },

    async register(name, email, password, tipo) {
        try {
            const res = await fetch('/api/auth/registro', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({
                    nombreCompleto: name,
                    correoElectronico: email,
                    contrasena: password,
                    tipoUsuario: tipo.toUpperCase(),
                }),
            });
            if (!res.ok) {
                return { success: false, error: await extraerError(res, 'No se pudo crear la cuenta.') };
            }
            // El registro solo crea la cuenta; iniciamos sesión justo después para obtenerla.
            return this.login(email, password);
        } catch {
            return { success: false, error: 'No se pudo conectar con el servidor. Intenta de nuevo.' };
        }
    },

    logout() {
        localStorage.removeItem('mjrenew_user');
        fetch('/api/auth/logout', { method: 'POST', credentials: 'include', keepalive: true }).catch(() => {});
    },

    getCurrentUser() {
        try { return JSON.parse(localStorage.getItem('mjrenew_user')); }
        catch { return null; }
    },

    requireAuth(redirectTo = '/login') {
        const user = this.getCurrentUser();
        if (!user) { window.location.href = redirectTo; return null; }
        return user;
    },

    redirectIfLoggedIn() {
        const user = this.getCurrentUser();
        if (!user) return;
        const map = { propietario:'/propietario', restaurador:'/restaurador', comprador:'/comprador' };
        window.location.href = map[user.tipo] || '/comprador';
    },
};

function guardarSesionLocal(usuarioResponse) {
    const user = {
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
    } catch {
        // el cuerpo no era JSON, usamos el mensaje por defecto
    }
    return mensajePorDefecto;
}
