
// Logica: sesion simulada (login, registro, logout)

// ── AUTENTICACIÓN SIMULADA ─────────────────────────────────────
const DEMO_USERS = {
    'jennifer@mjrenew.mx': { name:'Jennifer Torres', tipo:'propietario', password:'123456' },
    'rafael@mjrenew.mx':   { name:'Rafael Mendoza',  tipo:'restaurador', password:'123456' },
    'ana@mjrenew.mx':      { name:'Ana García',       tipo:'comprador',   password:'123456' },
};

const Auth = {
    login(email, password) {
        // Check demo users
        if (DEMO_USERS[email] && DEMO_USERS[email].password === password) {
            const user = { ...DEMO_USERS[email], email };
            localStorage.setItem('mjrenew_user', JSON.stringify(user));
            return { success: true, user };
        }
        // Check registered users
        const users = JSON.parse(localStorage.getItem('mjrenew_users') || '{}');
        if (users[email] && users[email].password === password) {
            const user = { ...users[email], email };
            localStorage.setItem('mjrenew_user', JSON.stringify(user));
            return { success: true, user };
        }
        return { success: false, error: 'Correo o contraseña incorrectos.' };
    },

    register(name, email, password, tipo) {
        const users = JSON.parse(localStorage.getItem('mjrenew_users') || '{}');
        if (DEMO_USERS[email] || users[email]) {
            return { success: false, error: 'Este correo ya está registrado.' };
        }
        users[email] = { name, password, tipo };
        localStorage.setItem('mjrenew_users', JSON.stringify(users));
        const user = { name, email, tipo };
        localStorage.setItem('mjrenew_user', JSON.stringify(user));
        return { success: true, user };
    },

    logout() {
        localStorage.removeItem('mjrenew_user');
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