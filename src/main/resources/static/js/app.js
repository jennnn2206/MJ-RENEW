/* ═══════════════════════════════════════════════════════════════
   MJ RENEW — app.js
   Lógica compartida: auth, catálogo, AFD, IA simulada, utilidades
═══════════════════════════════════════════════════════════════ */

'use strict';

// ── AFD: ESTADOS DEL PROCESO ───────────────────────────────────
const AFD_STATES = {
  q0:  { id:'q0',  label:'Pieza capturada',          icon:'📷', phase:'Captura' },
  q1:  { id:'q1',  label:'En evaluación',             icon:'🔍', phase:'Evaluación' },
  q2:  { id:'q2',  label:'Calculando presupuesto',    icon:'💰', phase:'Evaluación' },
  q3:  { id:'q3',  label:'Presupuesto presentado',    icon:'📋', phase:'Evaluación' },
  q4:  { id:'q4',  label:'Pago en escrow',            icon:'🔒', phase:'Evaluación' },
  q5:  { id:'q5',  label:'Recolección agendada',      icon:'📅', phase:'Logística ida' },
  q6:  { id:'q6',  label:'En recolección',            icon:'🚚', phase:'Logística ida' },
  q7:  { id:'q7',  label:'Recibido en taller',        icon:'🏭', phase:'Logística ida' },
  q8:  { id:'q8',  label:'En restauración',           icon:'🔧', phase:'Restauración' },
  q9:  { id:'q9',  label:'Avance publicado',          icon:'📸', phase:'Restauración' },
  q10: { id:'q10', label:'Restauración lista',        icon:'✅', phase:'Restauración' },
  q11: { id:'q11', label:'Decisión en taller',        icon:'🤔', phase:'Restauración' },
  q12: { id:'q12', label:'Entrega agendada',          icon:'📅', phase:'Logística vuelta' },
  q13: { id:'q13', label:'En devolución',             icon:'🚚', phase:'Logística vuelta' },
  q14: { id:'q14', label:'Entregado al dueño',        icon:'🏠', phase:'Logística vuelta' },
  q15: { id:'q15', label:'En catálogo',               icon:'🛒', phase:'Venta' },
  q16: { id:'q16', label:'Link de pago enviado',      icon:'💳', phase:'Venta' },
  qD1: { id:'qD1', label:'Disputa abierta',           icon:'⚠️', phase:'Disputa', cancel:true },
  qD2: { id:'qD2', label:'En revisión admin',         icon:'👤', phase:'Disputa', cancel:true },
  qD3: { id:'qD3', label:'Disputa resuelta',          icon:'⚖️', phase:'Disputa', cancel:true },
  qC1: { id:'qC1', label:'No cumple requisitos',      icon:'❌', phase:'Cancelación', cancel:true },
  qC2: { id:'qC2', label:'Dueño no quiso restaurar', icon:'🚫', phase:'Cancelación', cancel:true },
  qC3: { id:'qC3', label:'Nadie en casa – recolección',icon:'🏚️', phase:'Cancelación', cancel:true },
  qC4: { id:'qC4', label:'Nadie en casa – entrega',  icon:'🏚️', phase:'Cancelación', cancel:true },
  qF1: { id:'qF1', label:'Restauración completada',  icon:'🎉', phase:'Finalizado', success:true },
  qF2: { id:'qF2', label:'Venta completada',          icon:'🎊', phase:'Finalizado', success:true },
};

// Secuencia visual del tracker (main happy path)
const AFD_TRACKER_STEPS = [
  'q0','q1','q2','q3','q4','q5','q6','q7','q8','q9','q10','q11','q12','q13','q14','q15','qF1'
];

// ── CATÁLOGO DE PIEZAS ─────────────────────────────────────────
const CATALOG = [
  {
    id: 1,
    nombre: 'Escritorio Colonial',
    categoria: 'Escritorios',
    epoca: 'Colonial · ~1890s',
    material: 'Cedro macizo',
    estado: 'Restaurado',
    precio: 38000,
    ubicacion: 'Mérida, Yucatán',
    descripcion: 'Escritorio de cedro macizo con herrajes originales de bronce. Pieza de finales del siglo XIX proveniente de una hacienda henequenera yucateca. Restauración completa de estructura y acabado con barniz natural de origen.',
    dimensiones: '140 × 70 × 80 cm',
    peso: 'Aprox. 45 kg',
    restaurador: 'Rafael Mendoza',
    img: 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=600&q=80',
    imgBefore: 'https://images.unsplash.com/photo-1506439773649-6e0eb8cfb237?w=400&q=70',
    imgAfter: 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=400&q=70',
    tags: ['colonial','cedro','escritorio','siglo xix'],
    vendido: false,
  },
  {
    id: 2,
    nombre: 'Vitrina Victoriana',
    categoria: 'Vitrinas',
    epoca: 'Victoriano · ~1880s',
    material: 'Nogal con vidrio biselado',
    estado: 'Restaurado',
    precio: 52000,
    ubicacion: 'Ciudad de México',
    descripcion: 'Vitrina de nogal con vidrio biselado original en perfectas condiciones. Piezas de hierro forjado artesanal en tiradores y bisagras. Restauración mínima respetando la pátina original de la madera.',
    dimensiones: '90 × 45 × 195 cm',
    peso: 'Aprox. 68 kg',
    restaurador: 'Ana Berzunza',
    img: 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=600&q=80',
    imgBefore: 'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=400&q=70',
    imgAfter: 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=400&q=70',
    tags: ['victoriano','nogal','vitrina','siglo xix'],
    vendido: false,
  },
  {
    id: 3,
    nombre: 'Comedor Art Nouveau',
    categoria: 'Comedores',
    epoca: 'Art Nouveau · ~1905',
    material: 'Caoba con incrustaciones',
    estado: 'Restaurado',
    precio: 89000,
    ubicacion: 'Guadalajara, Jalisco',
    descripcion: 'Juego de comedor Art Nouveau en caoba con incrustaciones de marquetería floral. Mesa para 8 personas con 6 sillas tapizadas en terciopelo color vino. Certificado de autenticidad incluido.',
    dimensiones: 'Mesa: 200 × 100 × 78 cm',
    peso: 'Aprox. 120 kg (conjunto)',
    restaurador: 'Carlos Estrada',
    img: 'https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?w=600&q=80',
    imgBefore: 'https://images.unsplash.com/photo-1505409628601-edc9af17fda6?w=400&q=70',
    imgAfter: 'https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?w=400&q=70',
    tags: ['art nouveau','caoba','comedor','siglo xx'],
    vendido: false,
  },
  {
    id: 4,
    nombre: 'Buró Art Déco',
    categoria: 'Burós',
    epoca: 'Art Déco · ~1928',
    material: 'Roble con herrajes niquelados',
    estado: 'Restaurado',
    precio: 18500,
    ubicacion: 'Monterrey, Nuevo León',
    descripcion: 'Buró de noche Art Déco con dos cajones y compartimiento inferior. Herrajes originales niquelados en perfecto estado. El acabado natural resalta las vetas geométricas características del estilo Déco.',
    dimensiones: '50 × 40 × 70 cm',
    peso: 'Aprox. 18 kg',
    restaurador: 'Rafael Mendoza',
    img: 'https://images.unsplash.com/photo-1595526114035-0d45ed16cfbf?w=600&q=80',
    imgBefore: 'https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=400&q=70',
    imgAfter: 'https://images.unsplash.com/photo-1595526114035-0d45ed16cfbf?w=400&q=70',
    tags: ['art deco','roble','buro','1920s'],
    vendido: false,
  },
  {
    id: 5,
    nombre: 'Sillón Luis XV',
    categoria: 'Sillas y sillones',
    epoca: 'Estilo Luis XV · ~1890s',
    material: 'Nogal tallado, tapiz brocado',
    estado: 'Restaurado',
    precio: 24000,
    ubicacion: 'Mérida, Yucatán',
    descripcion: 'Sillón estilo Luis XV en nogal tallado a mano. Retapizado con tela brocada en tonos dorados y crema. Patas cabriola originales con detalle de flor de lis tallado. Pieza de colección en excelente estado.',
    dimensiones: '70 × 65 × 95 cm',
    peso: 'Aprox. 12 kg',
    restaurador: 'Ana Berzunza',
    img: 'https://images.unsplash.com/photo-1567538096630-e0c55bd6374c?w=600&q=80',
    imgBefore: 'https://images.unsplash.com/photo-1484101403633-562f891dc89a?w=400&q=70',
    imgAfter: 'https://images.unsplash.com/photo-1567538096630-e0c55bd6374c?w=400&q=70',
    tags: ['luis xv','nogal','sillon','tapizado'],
    vendido: false,
  },
  {
    id: 6,
    nombre: 'Cómoda Porfiriato',
    categoria: 'Cómodas',
    epoca: 'Porfiriato · ~1900s',
    material: 'Cedro con jaladeras de bronce',
    estado: 'En restauración',
    precio: 31000,
    ubicacion: 'Ciudad de México',
    descripcion: 'Cómoda de cuatro cajones época Porfiriato con jaladeras originales de bronce. Madera de cedro con acabado natural. Actualmente en proceso de restauración estructural y limpieza profunda. Disponible en 3-4 semanas.',
    dimensiones: '110 × 55 × 90 cm',
    peso: 'Aprox. 38 kg',
    restaurador: 'Carlos Estrada',
    img: 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=600&q=80',
    imgBefore: 'https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=400&q=70',
    imgAfter: null,
    tags: ['porfiriato','cedro','comoda','bronce'],
    vendido: false,
  },
  {
    id: 7,
    nombre: 'Aparador Inglés',
    categoria: 'Aparadores',
    epoca: 'Eduardiano · ~1870s',
    material: 'Caoba con espejo biselado',
    estado: 'Restaurado',
    precio: 67000,
    ubicacion: 'Guadalajara, Jalisco',
    descripcion: 'Aparador inglés de estilo Eduardiano con espejo biselado original y tres compartimientos. Caoba maciza con incrustaciones de marfil sintético. Pieza de museo en condición excepcional.',
    dimensiones: '160 × 55 × 200 cm',
    peso: 'Aprox. 95 kg',
    restaurador: 'Rafael Mendoza',
    img: 'https://images.unsplash.com/photo-1618220179428-22790b461013?w=600&q=80',
    imgBefore: 'https://images.unsplash.com/photo-1505409628601-edc9af17fda6?w=400&q=70',
    imgAfter: 'https://images.unsplash.com/photo-1618220179428-22790b461013?w=400&q=70',
    tags: ['eduardiano','caoba','aparador','espejo'],
    vendido: false,
  },
  {
    id: 8,
    nombre: 'Mesa de Centro Colonial',
    categoria: 'Mesas',
    epoca: 'Colonial · ~1850s',
    material: 'Madera tallada, hierro forjado',
    estado: 'Restaurado',
    precio: 22500,
    ubicacion: 'Oaxaca, Oaxaca',
    descripcion: 'Mesa de centro colonial con base de hierro forjado artesanal y tablero de madera tallada con motivos florales. Combinación única de herrería y carpintería de la región oaxaqueña. Pieza única.',
    dimensiones: '120 × 60 × 48 cm',
    peso: 'Aprox. 28 kg',
    restaurador: 'Ana Berzunza',
    img: 'https://images.unsplash.com/photo-1449824913935-59a10b8d2000?w=600&q=80',
    imgBefore: 'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=400&q=70',
    imgAfter: 'https://images.unsplash.com/photo-1449824913935-59a10b8d2000?w=400&q=70',
    tags: ['colonial','hierro','mesa','oaxaca'],
    vendido: false,
  },
];

// ── RESTAURADORES ──────────────────────────────────────────────
const RESTAURADORES = [
  {
    id: 'r1',
    nombre: 'Rafael Mendoza Uc',
    especialidad: 'Ebanistería y restauración de madera',
    rating: 4.9,
    proyectos: 47,
    img: null,
    bio: 'Ebanista con 12 años de experiencia en restauración de mobiliario de los siglos XVIII y XIX. Especialista en técnicas de dorado al pan y talla en madera.',
    ubicacion: 'Mérida, Yucatán',
  },
  {
    id: 'r2',
    nombre: 'Ana Berzunza Torres',
    especialidad: 'Tapicería y restauración textil',
    rating: 4.8,
    proyectos: 31,
    img: null,
    bio: 'Especialista en tapicería tradicional y restauración de piezas con tejidos históricos. Certificada por el INAH en conservación de patrimonio mueble.',
    ubicacion: 'Mérida, Yucatán',
  },
  {
    id: 'r3',
    nombre: 'Carlos Estrada Villanueva',
    especialidad: 'Estructura y acabados',
    rating: 4.7,
    proyectos: 28,
    img: null,
    bio: 'Carpintero estructural especializado en restauración de mobiliario Art Nouveau y Art Déco. Experto en consolidación de ensambles históricos.',
    ubicacion: 'Ciudad de México',
  },
];

// ── DATOS SIMULADOS: PIEZAS DEL PROPIETARIO ───────────────────
const OWNER_PIECES_DEFAULT = [
  {
    id: 'p1',
    nombre: 'Vitrina colonial de nogal',
    estado_afd: 'q9',
    imagen: 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=400&q=70',
    costo_estimado: '$14,500 MXN',
    tiempo_restante: '2 semanas',
    restaurador: 'Rafael Mendoza',
    fecha_inicio: '2025-03-10',
  },
  {
    id: 'p2',
    nombre: 'Sillón tapizado Luís XV',
    estado_afd: 'q3',
    imagen: 'https://images.unsplash.com/photo-1567538096630-e0c55bd6374c?w=400&q=70',
    costo_estimado: '$8,200 MXN',
    tiempo_restante: '4-6 semanas',
    restaurador: 'Ana Berzunza',
    fecha_inicio: '2025-03-18',
  },
];

// ── SOLICITUDES PARA RESTAURADOR ───────────────────────────────
const RESTAURADOR_REQUESTS_DEFAULT = [
  {
    id: 'req1',
    pieza: 'Cómoda victoriana con marquetería',
    propietario: 'Patricia García',
    fecha: '2025-03-20',
    estado: 'pendiente',
    descripcion: 'Cómoda de finales del siglo XIX con marquetería deteriorada. Requiere consolidación estructural y restauración de superficies.',
    presupuesto_estimado: '$12,000 – $16,000 MXN',
    img: 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=300&q=70',
  },
  {
    id: 'req2',
    pieza: 'Mesa de centro Art Déco',
    propietario: 'Roberto Suárez',
    fecha: '2025-03-19',
    estado: 'pendiente',
    descripcion: 'Mesa de centro con tablero de mármol y base en hierro forjado. Presenta oxidación en la base y una fisura en el mármol.',
    presupuesto_estimado: '$6,000 – $9,000 MXN',
    img: 'https://images.unsplash.com/photo-1449824913935-59a10b8d2000?w=300&q=70',
  },
];

const RESTAURADOR_ACTIVE_DEFAULT = [
  {
    id: 'act1',
    pieza: 'Vitrina colonial de nogal',
    propietario: 'Jennifer Torres',
    estado_afd: 'q9',
    progreso: 65,
    inicio: '2025-03-10',
    fin_estimado: '2025-04-01',
    img: 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=300&q=70',
  },
];

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
    const isFinal = state.success;
    return `
      <div class="afd-step ${status}">
        <div class="afd-step-dot">${status === 'done' ? '✓' : state.icon}</div>
        <div class="afd-step-info">
          <div class="afd-step-label">${state.label}</div>
          <div class="afd-step-desc">${state.phase}</div>
          ${status === 'current' ? '<div class="afd-step-time">● En proceso</div>' : ''}
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

// ── IA SIMULADA ────────────────────────────────────────────────
const AIAnalyzer = {
  RESULTS: [
    { tipo:'Cómoda antigua', material:'Madera de caoba', estilo:'Colonial', estado:'Regular – daños moderados', restauracion:'Restauración estructural + acabado lacado', costo_min:4500, costo_max:6500, tiempo_min:3, tiempo_max:5, valor_potencial:28000 },
    { tipo:'Sillón de época', material:'Nogal con tapiz deteriorado', estilo:'Luis XV', estado:'Bueno – requiere tapizado', restauracion:'Retapizado + limpieza de madera', costo_min:3200, costo_max:4800, tiempo_min:2, tiempo_max:4, valor_potencial:18500 },
    { tipo:'Mesa de comedor', material:'Cedro macizo', estilo:'Art Déco', estado:'Regular – fisuras en tablero', restauracion:'Consolidación estructural + barnizado', costo_min:5000, costo_max:7500, tiempo_min:4, tiempo_max:6, valor_potencial:35000 },
    { tipo:'Escritorio antiguo', material:'Roble con herrajes de bronce', estilo:'Eduardiano', estado:'Malo – daños estructurales', restauracion:'Restauración completa de estructura y acabado', costo_min:8000, costo_max:12000, tiempo_min:5, tiempo_max:8, valor_potencial:45000 },
    { tipo:'Vitrina', material:'Nogal con vidrio biselado', estilo:'Victoriano', estado:'Bueno – pátina original', restauracion:'Limpieza profunda + consolidación menor', costo_min:2500, costo_max:4000, tiempo_min:2, tiempo_max:3, valor_potencial:22000 },
  ],

  analyze(imageFile, callback) {
    // Simulate AI analysis delay
    setTimeout(() => {
      const result = this.RESULTS[Math.floor(Math.random() * this.RESULTS.length)];
      callback(result);
    }, 2800);
  },
};

// ── CATALOG RENDERER ───────────────────────────────────────────
function renderCatalogGrid(containerId, pieces, onCardClick) {
  const container = document.getElementById(containerId);
  if (!container) return;

  if (!pieces.length) {
    container.innerHTML = `
      <div class="empty-state">
        <span class="empty-state-icon">🔍</span>
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
        <div class="piece-card-loc">📍 ${p.ubicacion}</div>
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
window.AIAnalyzer   = AIAnalyzer;
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