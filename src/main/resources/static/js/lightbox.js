// MJ RENEW — lightbox.js
// Visor de imágenes a pantalla completa con zoom (rueda, pellizco, doble clic o teclas + / -).
//
// Uso (una sola línea):
//   <img src="..." alt="..." data-lightbox>      ← en el HTML o en un template string
//   Lightbox.attach(imgElement)                   ← desde JavaScript
//   Lightbox.open(url, textoAlternativo)          ← abrir directamente
//
// Funciona también con imágenes que se insertan después con innerHTML,
// porque escucha los clics a nivel de documento.
const Lightbox = (() => {
  const ZOOM_MIN = 1;
  const ZOOM_MAX = 5;
  const PASO_TECLADO = 1.25;

  let overlay, stage, img, caption, errorMsg, closeBtn, zoomInBtn, zoomOutBtn, zoomLabel;
  let escala = 1, tx = 0, ty = 0;
  let abierto = false;
  let focoPrevio = null;
  let overflowPrevio = '';

  // Estado de punteros (mouse, dedo o lápiz) para arrastrar y pellizcar
  const punteros = new Map();
  let distanciaPrevia = 0;
  let puntoPrevio = null;
  let huboArrastre = false;

  function crear() {
    overlay = document.createElement('div');
    overlay.className = 'lightbox-overlay';
    overlay.setAttribute('role', 'dialog');
    overlay.setAttribute('aria-modal', 'true');
    overlay.setAttribute('aria-label', 'Imagen ampliada');
    overlay.innerHTML = `
      <div class="lightbox-toolbar">
        <button type="button" class="lightbox-btn" data-accion="alejar" aria-label="Alejar">−</button>
        <span class="lightbox-zoom-label" aria-live="polite">100%</span>
        <button type="button" class="lightbox-btn" data-accion="acercar" aria-label="Acercar">+</button>
        <button type="button" class="lightbox-btn lightbox-close" data-accion="cerrar" aria-label="Cerrar imagen">×</button>
      </div>
      <div class="lightbox-stage">
        <img class="lightbox-img" alt="" draggable="false">
        <p class="lightbox-error" hidden>No se pudo cargar esta imagen.</p>
      </div>
      <p class="lightbox-caption"></p>
    `;
    document.body.appendChild(overlay);

    stage      = overlay.querySelector('.lightbox-stage');
    img        = overlay.querySelector('.lightbox-img');
    caption    = overlay.querySelector('.lightbox-caption');
    errorMsg   = overlay.querySelector('.lightbox-error');
    closeBtn   = overlay.querySelector('[data-accion="cerrar"]');
    zoomInBtn  = overlay.querySelector('[data-accion="acercar"]');
    zoomOutBtn = overlay.querySelector('[data-accion="alejar"]');
    zoomLabel  = overlay.querySelector('.lightbox-zoom-label');

    closeBtn.addEventListener('click', close);
    zoomInBtn.addEventListener('click', () => zoomEn(centroStage(), escala * PASO_TECLADO));
    zoomOutBtn.addEventListener('click', () => zoomEn(centroStage(), escala / PASO_TECLADO));

    img.addEventListener('load', () => { errorMsg.hidden = true; img.hidden = false; });
    img.addEventListener('error', () => { errorMsg.hidden = false; img.hidden = true; });

    // Clic fuera de la imagen → cerrar (si no fue el final de un arrastre)
    stage.addEventListener('click', (e) => {
      if (e.target !== img && !huboArrastre) close();
    });
    img.addEventListener('dblclick', (e) => {
      zoomEn({ x: e.clientX, y: e.clientY }, escala > 1 ? 1 : 2.5);
    });

    stage.addEventListener('wheel', (e) => {
      e.preventDefault();
      const factor = Math.exp(-e.deltaY * 0.0015);
      zoomEn({ x: e.clientX, y: e.clientY }, escala * factor);
    }, { passive: false });

    stage.addEventListener('pointerdown', alPresionar);
    stage.addEventListener('pointermove', alMover);
    stage.addEventListener('pointerup', alSoltar);
    stage.addEventListener('pointercancel', alSoltar);
    stage.addEventListener('pointerleave', alSoltar);
  }

  // ── Zoom y desplazamiento ────────────────────────────────────
  function centroStage() {
    const r = stage.getBoundingClientRect();
    return { x: r.left + r.width / 2, y: r.top + r.height / 2 };
  }

  // Cambia la escala manteniendo fijo el punto de la pantalla indicado
  function zoomEn(punto, nuevaEscala) {
    nuevaEscala = Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, nuevaEscala));
    if (nuevaEscala === escala) return;
    const c = centroStage();
    const k = nuevaEscala / escala;
    tx = (punto.x - c.x) - ((punto.x - c.x) - tx) * k;
    ty = (punto.y - c.y) - ((punto.y - c.y) - ty) * k;
    escala = nuevaEscala;
    if (escala === 1) { tx = 0; ty = 0; }
    aplicar();
  }

  function aplicar() {
    img.style.transform = `translate(${tx}px, ${ty}px) scale(${escala})`;
    img.classList.toggle('is-zoomed', escala > 1);
    zoomLabel.textContent = `${Math.round(escala * 100)}%`;
    zoomOutBtn.disabled = escala <= ZOOM_MIN;
    zoomInBtn.disabled = escala >= ZOOM_MAX;
  }

  function reiniciar() {
    escala = 1; tx = 0; ty = 0;
    aplicar();
  }

  // ── Arrastrar (con zoom) y pellizcar (móvil) ─────────────────
  function alPresionar(e) {
    if (e.target.closest('.lightbox-btn')) return;
    punteros.set(e.pointerId, { x: e.clientX, y: e.clientY });
    huboArrastre = false;
    if (punteros.size === 1) {
      puntoPrevio = { x: e.clientX, y: e.clientY };
    } else if (punteros.size === 2) {
      const [a, b] = [...punteros.values()];
      distanciaPrevia = Math.hypot(a.x - b.x, a.y - b.y);
      puntoPrevio = { x: (a.x + b.x) / 2, y: (a.y + b.y) / 2 };
    }
  }

  function alMover(e) {
    if (!punteros.has(e.pointerId)) return;
    punteros.set(e.pointerId, { x: e.clientX, y: e.clientY });

    if (punteros.size === 2) {
      const [a, b] = [...punteros.values()];
      const distancia = Math.hypot(a.x - b.x, a.y - b.y);
      const medio = { x: (a.x + b.x) / 2, y: (a.y + b.y) / 2 };
      if (distanciaPrevia > 0) zoomEn(medio, escala * (distancia / distanciaPrevia));
      if (escala > 1) { tx += medio.x - puntoPrevio.x; ty += medio.y - puntoPrevio.y; aplicar(); }
      distanciaPrevia = distancia;
      puntoPrevio = medio;
      huboArrastre = true;
    } else if (punteros.size === 1 && escala > 1 && puntoPrevio) {
      const dx = e.clientX - puntoPrevio.x;
      const dy = e.clientY - puntoPrevio.y;
      if (Math.abs(dx) + Math.abs(dy) > 2) huboArrastre = true;
      tx += dx; ty += dy;
      puntoPrevio = { x: e.clientX, y: e.clientY };
      aplicar();
    }
  }

  function alSoltar(e) {
    punteros.delete(e.pointerId);
    if (punteros.size === 1) {
      const [p] = [...punteros.values()];
      puntoPrevio = { x: p.x, y: p.y };
    } else if (punteros.size === 0) {
      puntoPrevio = null;
      distanciaPrevia = 0;
    }
  }

  // ── Teclado ──────────────────────────────────────────────────
  function alTeclear(e) {
    if (!abierto) return;
    if (e.key === 'Escape') { e.preventDefault(); e.stopPropagation(); close(); return; }
    if (e.key === '+' || e.key === '=') { e.preventDefault(); zoomEn(centroStage(), escala * PASO_TECLADO); }
    if (e.key === '-' || e.key === '_') { e.preventDefault(); zoomEn(centroStage(), escala / PASO_TECLADO); }
    if (e.key === '0') { e.preventDefault(); reiniciar(); }
    if (e.key === 'Tab') {
      // Mantener el foco dentro del visor
      const botones = [...overlay.querySelectorAll('.lightbox-btn:not(:disabled)')];
      const i = botones.indexOf(document.activeElement);
      e.preventDefault();
      const siguiente = e.shiftKey ? (i <= 0 ? botones.length - 1 : i - 1) : (i + 1) % botones.length;
      botones[siguiente].focus();
    }
  }

  // ── API pública ──────────────────────────────────────────────
  function open(src, alt = '') {
    if (!src) return;
    if (!overlay) crear();
    focoPrevio = document.activeElement;
    overflowPrevio = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    errorMsg.hidden = true;
    img.hidden = false;
    img.src = src;
    img.alt = alt;
    caption.textContent = alt;
    caption.hidden = !alt;
    reiniciar();

    abierto = true;
    overlay.classList.add('open');
    document.addEventListener('keydown', alTeclear, true);
    closeBtn.focus();
  }

  function close() {
    if (!abierto) return;
    abierto = false;
    overlay.classList.remove('open');
    document.removeEventListener('keydown', alTeclear, true);
    // Si había un modal abierto debajo, se respeta su bloqueo de scroll
    document.body.style.overflow = overflowPrevio;
    punteros.clear();
    if (focoPrevio && typeof focoPrevio.focus === 'function') focoPrevio.focus();
  }

  // Hace que una imagen sea ampliable y accesible con teclado
  function attach(el) {
    if (!el || el.dataset.lightboxListo) return;
    el.setAttribute('data-lightbox', '');
    el.dataset.lightboxListo = '1';
    el.tabIndex = 0;
    el.setAttribute('role', 'button');
    el.setAttribute('aria-label', `Ampliar imagen${el.alt ? ': ' + el.alt : ''}`);
  }

  function prepararTodas(raiz = document) {
    raiz.querySelectorAll('img[data-lightbox]').forEach(attach);
  }

  // Delegación: un solo listener para todas las imágenes, presentes y futuras
  document.addEventListener('click', (e) => {
    const el = e.target.closest('img[data-lightbox]');
    if (!el) return;
    e.preventDefault();
    e.stopPropagation();
    open(el.currentSrc || el.src, el.alt);
  });
  document.addEventListener('keydown', (e) => {
    if (abierto || (e.key !== 'Enter' && e.key !== ' ')) return;
    const el = e.target.closest && e.target.closest('img[data-lightbox]');
    if (!el) return;
    e.preventDefault();
    open(el.currentSrc || el.src, el.alt);
  });

  // Las imágenes que se agregan con innerHTML reciben foco de teclado automáticamente
  function observar() {
    prepararTodas();
    new MutationObserver((cambios) => {
      for (const c of cambios) {
        c.addedNodes.forEach((n) => {
          if (n.nodeType !== 1) return;
          if (n.matches('img[data-lightbox]')) attach(n);
          else prepararTodas(n);
        });
      }
    }).observe(document.body, { childList: true, subtree: true });
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', observar);
  else observar();

  return { open, close, attach };
})();
