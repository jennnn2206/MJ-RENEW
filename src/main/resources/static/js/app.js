'use strict';

// Los datos (AFD_STATES, CATALOG, RESTAURADORES, OWNER_PIECES_DEFAULT,
// RESTAURADOR_REQUESTS_DEFAULT, RESTAURADOR_ACTIVE_DEFAULT) viven ahora en
// js/data/*.js — se cargan antes que este archivo.

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

// ── TOAST ──────────────────────────────────────────────────────
const Toast = {
  container: null,

  init() {
    if (!this.container) {
      this.container = document.createElement('div');
      this.container.id = 'toast-container';
      document.body.appendChild(this.container);
    }
  },

  show(message, type = 'info', duration = 3500) {
    this.init();
    const icons = { success:'✓', error:'✕', info:'ℹ' };
    const t = document.createElement('div');
    t.className = `toast ${type}`;
    t.innerHTML = `<span>${icons[type]||'ℹ'}</span><span>${message}</span>`;
    this.container.appendChild(t);
    setTimeout(() => { t.style.opacity = '0'; t.style.transform = 'translateX(20px)'; t.style.transition = 'all 0.3s'; setTimeout(() => t.remove(), 300); }, duration);
  },
};

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

// ── HEADER DINÁMICO ────────────────────────────────────────────
function renderHeader(isRoot = false) {
  const header = document.getElementById('site-header');
  if (!header) return;
  const user = Auth.getCurrentUser();
  const base = '/';
  const home = '/';

  const navLinks = isRoot ? [
    { href: '#como-funciona', text: 'Cómo funciona' },
    { href: '#restauracion',  text: 'Restauración' },
    { href: `${base}catalogo`, text: 'Catálogo' },
    { href: '#nosotros',      text: 'Nosotros' },
  ] : [
    { href: `${home}`, text: 'Inicio' },
    { href: `${base}catalogo`, text: 'Catálogo' },
  ];

  let userSection;
  if (user) {
    const dashMap = { propietario:`${base}propietario`, restaurador:`${base}restaurador`, comprador:`${base}comprador` };
    userSection = `
      <div class="user-pill" onclick="window.location.href='${dashMap[user.tipo]||base+'comprador'}'">
        <div class="user-avatar">${Utils.initials(user.name)}</div>
        <span class="user-pill-name">${user.name.split(' ')[0]}</span>
      </div>
      <button class="btn nav-btn-login btn-sm" onclick="Auth.logout(); window.location.href='${home}'">Salir</button>
    `;
  } else {
    userSection = `
      <a href="${base}login" class="btn nav-btn-login btn-sm">Iniciar sesión</a>
          <a href="${base}registro" class="btn nav-btn-register btn-sm">Registrarme</a>
    `;
  }

  header.innerHTML = `
    <div class="container">
      <a class="site-logo" href="${home}">
        MJ Renew
        <span>Antigüedades &amp; Restauración</span>
      </a>
      <nav class="site-nav">
        ${navLinks.map(l => `<a href="${l.href}">${l.text}</a>`).join('')}
        ${userSection}
      </nav>
      <button class="hamburger" id="hamburger-btn">☰</button>
    </div>
  `;

  // Hamburger toggle for mobile nav
  const hBtn = document.getElementById('hamburger-btn');
  if (hBtn) {
    hBtn.addEventListener('click', () => {
      const nav = header.querySelector('.site-nav');
      nav.style.display = nav.style.display === 'flex' ? 'none' : 'flex';
      nav.style.flexDirection = 'column';
      nav.style.position = 'absolute';
      nav.style.top = '70px';
      nav.style.right = '0';
      nav.style.left = '0';
      nav.style.background = '#3B2A1A';
      nav.style.padding = '16px';
      nav.querySelectorAll('a').forEach(a => { a.style.display = 'block'; a.style.padding = '10px 16px'; });
    });
  }
}

// ── AFD TRACKER RENDERER ───────────────────────────────────────
function renderAFDTracker(containerId, currentState, piece) {
  const container = document.getElementById(containerId);
  if (!container) return;

  const mainPath = ['q0','q1','q2','q3','q4','q5','q6','q7','q8','q9','q10','q11','q14','qF1'];
  const currentIdx = mainPath.indexOf(currentState);

  const steps = mainPath.map((stateId, idx) => {
    const state = AFD_STATES[stateId];
    let status = 'pending';
    if (idx < currentIdx) status = 'done';
    else if (idx === currentIdx) status = 'current';
    return `
      <div class="afd-step ${status}">
        <div class="afd-step-dot">${status === 'done' ? '✓' : (idx + 1)}</div>
        <div class="afd-step-info">
          <div class="afd-step-label">${state.label}</div>
          <div class="afd-step-desc">${state.phase}</div>
          ${status === 'current' ? '<div class="afd-step-time">En proceso</div>' : ''}
        </div>
      </div>
    `;
  });

  container.innerHTML = `
    <div class="afd-tracker">
      <div class="afd-tracker-title">${piece?.nombre || 'Tu pieza'}</div>
      <div class="afd-tracker-subtitle">Estado: ${AFD_STATES[currentState]?.label || currentState}</div>
      <div class="afd-steps">${steps.join('')}</div>
    </div>
  `;
}

// ── CATALOG RENDERER ───────────────────────────────────────────
function renderCatalogGrid(containerId, pieces, onCardClick) {
  const container = document.getElementById(containerId);
  if (!container) return;

  if (!pieces.length) {
    container.innerHTML = `
      <div class="empty-state">
        <h3>Sin resultados</h3>
        <p>Intenta con otros filtros o términos de búsqueda.</p>
      </div>
    `;
    return;
  }

  const favs = JSON.parse(localStorage.getItem('mjrenew_favs') || '[]');

  container.innerHTML = pieces.map(p => `
    <div class="piece-card" onclick="${onCardClick ? `(${onCardClick})(${p.id})` : `viewPiece(${p.id})`}">
      <div class="piece-card-img-wrap">
        <img src="${p.img}" alt="${p.nombre}" loading="lazy">
        <button class="piece-card-fav ${favs.includes(p.id) ? 'active' : ''}"
          onclick="event.stopPropagation(); toggleFav(${p.id}, this)"
          aria-label="Agregar a favoritos">♡</button>
        <span class="piece-card-status"><span class="badge ${p.estado === 'Restaurado' ? 'badge-green' : 'badge-orange'}">${p.estado}</span></span>
        <span class="piece-card-era"><span class="badge badge-brown">${p.epoca.split('·')[0].trim()}</span></span>
      </div>
      <div class="piece-card-body">
        <div class="piece-card-tag">${p.categoria} · ${p.material}</div>
        <h4>${p.nombre}</h4>
        <div class="piece-card-meta">
          <span class="piece-card-price">${Utils.formatCurrency(p.precio)}</span>
        </div>
        <div class="piece-card-loc">${p.ubicacion}</div>
      </div>
    </div>
  `).join('');
}

function toggleFav(id, btn) {
  let favs = JSON.parse(localStorage.getItem('mjrenew_favs') || '[]');
  const idx = favs.indexOf(id);
  if (idx === -1) {
    favs.push(id);
    btn.textContent = '♥';
    btn.classList.add('active');
    Toast.show('Agregado a favoritos', 'success');
  } else {
    favs.splice(idx, 1);
    btn.textContent = '♡';
    btn.classList.remove('active');
    Toast.show('Eliminado de favoritos', 'info');
  }
  localStorage.setItem('mjrenew_favs', JSON.stringify(favs));
}

function viewPiece(id) {
  window.location.href = `/pieza?id=${id}`;
}

// ── CATALOG FILTERS ────────────────────────────────────────────
const CatalogFilter = {
  search: '',
  categoria: [],
  estado: [],
  precioMax: 999999,

  apply() {
    let result = CATALOG;
    if (this.search) {
      const q = this.search.toLowerCase();
      result = result.filter(p =>
          p.nombre.toLowerCase().includes(q) ||
          p.categoria.toLowerCase().includes(q) ||
          p.material.toLowerCase().includes(q) ||
          p.epoca.toLowerCase().includes(q) ||
          p.tags.some(t => t.includes(q))
      );
    }
    if (this.categoria.length) result = result.filter(p => this.categoria.includes(p.categoria));
    if (this.estado.length)    result = result.filter(p => this.estado.includes(p.estado));
    result = result.filter(p => p.precio <= this.precioMax);
    return result;
  },
};

// ── PIECE DETAIL ───────────────────────────────────────────────
function getPieceById(id) {
  return CATALOG.find(p => p.id === parseInt(id));
}

// ── GLOBAL EVENT LISTENERS ─────────────────────────────────────
document.addEventListener('click', e => {
  if (e.target.closest('.modal-overlay') === e.target) Modal.closeAll();
});
document.addEventListener('keydown', e => {
  if (e.key === 'Escape') Modal.closeAll();
});

// ── EXPORT (para uso en módulos si se migra a ESM) ────────────
window.Auth         = Auth;
window.Utils        = Utils;
window.Toast        = Toast;
window.Modal        = Modal;
window.CATALOG      = CATALOG;
window.RESTAURADORES= RESTAURADORES;
window.AFD_STATES   = AFD_STATES;
window.CatalogFilter= CatalogFilter;
window.renderHeader = renderHeader;
window.renderAFDTracker = renderAFDTracker;
window.renderCatalogGrid= renderCatalogGrid;
window.toggleFav    = toggleFav;
window.viewPiece    = viewPiece;
window.getPieceById = getPieceById;
window.OWNER_PIECES_DEFAULT = OWNER_PIECES_DEFAULT;
window.RESTAURADOR_REQUESTS_DEFAULT = RESTAURADOR_REQUESTS_DEFAULT;
window.RESTAURADOR_ACTIVE_DEFAULT   = RESTAURADOR_ACTIVE_DEFAULT;