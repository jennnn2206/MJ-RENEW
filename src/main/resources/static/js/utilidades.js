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

    mostrarFotosFinales(contenedor, fotos) {
        this.mostrarGaleria(contenedor, fotos, 'Estado final de la restauración');
    },

    mostrarGaleria(contenedor, fotos, encabezado) {
        if (!fotos?.length) return;
        const seccion = document.createElement('section');
        const titulo = document.createElement('h4');
        titulo.textContent = encabezado;
        seccion.append(titulo);
        const galeria = document.createElement('div');
        galeria.className = 'modal-gallery';
        fotos.forEach(url => {
            const imagen = document.createElement('img');
            imagen.src = url;
            imagen.alt = encabezado;
            imagen.dataset.lightbox = '';
            imagen.loading = 'lazy';
            galeria.append(imagen);
        });
        seccion.append(galeria);
        contenedor.append(seccion);
    },
};
