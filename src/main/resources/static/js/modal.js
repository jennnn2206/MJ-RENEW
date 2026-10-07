
// MJ RENEW — modal.js
// Logica: abrir/cerrar ventanas modales
// ── MODAL ──────────────────────────────────────────────────────
const Modal = {
    open(id) {
        const m = document.getElementById(id);
        if (m) { m.classList.add('open'); document.body.style.overflow = 'hidden'; }
    },
    close(id) {
        const m = document.getElementById(id);
        if (m) { m.classList.remove('open'); document.body.style.overflow = ''; }
    },
    closeAll() {
        document.querySelectorAll('.modal-overlay.open').forEach(m => {
            m.classList.remove('open');
        });
        document.body.style.overflow = '';
    },
};