// Logica: formateo de moneda, iniciales, debounce

// ── UTILIDADES ─────────────────────────────────────────────────
const Utils = {
    formatCurrency(n) {
        return '$' + Number(n).toLocaleString('es-MX') + ' MXN';
    },

    initials(name) {
        return (name || '?').split(' ').map(w => w[0]).join('').slice(0,2).toUpperCase();
    },

    debounce(fn, delay = 300) {
        let t;
        return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), delay); };
    },

    getPath(isRoot = false) {
        return '/';
    },
};