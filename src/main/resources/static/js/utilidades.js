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

    // Para texto escrito por usuarios que se inserta con innerHTML.
    escapeHtml(texto) {
        return String(texto ?? '').replace(/[&<>"']/g, c => ({
            '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
        })[c]);
    },

    getPath(isRoot = false) {
        return '/';
    },
};