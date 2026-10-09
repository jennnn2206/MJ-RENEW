
// Logica: capa de acceso a la API real (/api/propietario, /api/restaurador,
// /api/catalogo). Centraliza fetch + manejo de errores para que las páginas
// no repitan la misma lógica. Se carga antes que app.js.

const Api = {
    async _req(url, options = {}) {
        const res = await fetch(url, {
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            ...options,
        });

        let body = null;
        try { body = await res.json(); } catch { /* sin cuerpo, p.ej. 204 */ }

        if (!res.ok) {
            const mensaje =
                (body && Array.isArray(body.detalles) && body.detalles.length && body.detalles[0]) ||
                (body && body.mensaje) ||
                `Ocurrió un error inesperado (HTTP ${res.status}).`;
            // MJRENEW-FLUJO: conservar el HTTP status para distinguir 404/403/409/500.
            const error = new Error(mensaje);
            error.status = res.status;
            throw error;
        }
        return body;
    },

    get(url) { return this._req(url); },
    post(url, data) {
        return this._req(url, {
            method: 'POST',
            body: data !== undefined ? JSON.stringify(data) : undefined,
        });
    },

    // ── PROPIETARIO ────────────────────────────────────────────
    misAntiguedades(page = 0, size = 50) {
        return this.get(`/api/propietario/obtenerMisAntiguedades?page=${page}&size=${size}`);
    },
    detalleAntiguedad(antiguedadId) {
        return this.get(`/api/propietario/obtenerDetalleAntiguedad/${antiguedadId}`);
    },
    registrarAntiguedad(data) {
        return this.post('/api/propietario/registrarAntiguedad', data);
    },
    buscarRestauradores(page = 0, size = 50) {
        return this.get(`/api/propietario/buscarRestauradores?page=${page}&size=${size}`);
    },
    seleccionarRestaurador(antiguedadId, restauradorId) {
        return this.post(`/api/propietario/seleccionarRestaurador/${antiguedadId}`, { restauradorId });
    },
    obtenerCotizacion(antiguedadId) {
        return this.get(`/api/propietario/obtenerCotizacion/${antiguedadId}`);
    },
    aceptarCotizacion(antiguedadId) {
        return this.post(`/api/propietario/aceptarCotizacion/${antiguedadId}`);
    },
    rechazarCotizacion(antiguedadId, motivo) {
        return this.post(`/api/propietario/rechazarCotizacion/${antiguedadId}`, { motivoRechazoCotizacion: motivo });
    },
    obtenerAvances(antiguedadId, page = 0, size = 20) {
        return this.get(`/api/propietario/obtenerAvancesRestauracion/${antiguedadId}?page=${page}&size=${size}`);
    },
    publicarEnCatalogo(antiguedadId, precioMxnCatalogo) {
        return this.post(`/api/propietario/publicarEnCatalogo/${antiguedadId}`, { precioMxnCatalogo });
    },

    // ── RESTAURADOR ────────────────────────────────────────────
    solicitudesAsignadas(page = 0, size = 50) {
        return this.get(`/api/restaurador/obtenerSolicitudesAsignadas?page=${page}&size=${size}`);
    },
    // MJRENEW-FLUJO: rutas del restaurador autenticado, no del propietario.
    misAntiguedadesRestaurador(page = 0, size = 50) {
        return this.get(`/api/restaurador/obtenerMisAntiguedades?page=${page}&size=${size}`);
    },
    detalleAntiguedadAsignada(antiguedadId) {
        return this.get(`/api/restaurador/obtenerDetalleAntiguedadAsignada/${antiguedadId}`);
    },
    confirmarEvaluacion(antiguedadId, dimensiones) {
        return this.post(`/api/restaurador/confirmarEvaluacionAntiguedad/${antiguedadId}`, dimensiones);
    },
    rechazarEvaluacion(antiguedadId, motivo) {
        return this.post(`/api/restaurador/rechazarEvaluacionAntiguedad/${antiguedadId}`, { motivoRechazoEvaluacion: motivo });
    },
    generarCotizacion(antiguedadId, data) {
        return this.post(`/api/restaurador/generarCotizacion/${antiguedadId}`, data);
    },
    iniciarRestauracion(antiguedadId) {
        return this.post(`/api/restaurador/iniciarRestauracion/${antiguedadId}`);
    },
    publicarAvanceRestauracion(antiguedadId, data) {
        return this.post(`/api/restaurador/publicarAvanceRestauracion/${antiguedadId}`, data);
    },
    marcarRestauracionLista(antiguedadId) {
        return this.post(`/api/restaurador/marcarRestauracionLista/${antiguedadId}`);
    },
    obtenerPerfilRestaurador() {
        return this.get('/api/restaurador/obtenerPerfilRestaurador');
    },
    actualizarPerfilRestaurador(data) {
        return this.post('/api/restaurador/actualizarPerfilRestaurador', data);
    },
    actualizarDisponibilidadRestaurador(disponibilidadRestaurador) {
        return this.post('/api/restaurador/actualizarDisponibilidadRestaurador', { disponibilidadRestaurador });
    },

    // ── CATÁLOGO (público) ─────────────────────────────────────
    explorarCatalogo(page = 0, size = 50) {
        return this.get(`/api/catalogo/explorarCatalogo?page=${page}&size=${size}`);
    },
    detallePiezaCatalogo(catalogoId) {
        return this.get(`/api/catalogo/obtenerDetallePiezaCatalogo/${catalogoId}`);
    },
    iniciarCompraPiezaCatalogo(catalogoId) {
        return this.post(`/api/catalogo/iniciarCompraPiezaCatalogo/${catalogoId}`);
    },
};

// ── ETIQUETAS: enums del backend -> texto/estilo en español ────
const TIPO_MUEBLE_LABELS = {
    ESCRITORIO: 'Escritorio', VITRINA: 'Vitrina', COMEDOR: 'Comedor', SILLON: 'Sillón',
    COMODA: 'Cómoda', APARADOR: 'Aparador', MESA: 'Mesa', OTRO: 'Otro',
};
const ESTILO_MUEBLE_LABELS = {
    COLONIAL: 'Colonial', VICTORIANO: 'Victoriano', ART_DECO: 'Art Déco', ART_NOUVEAU: 'Art Nouveau',
    PORFIRIATO: 'Porfiriato', EDUARDIANO: 'Eduardiano', LUIS_XV: 'Luis XV', OTRO: 'Otro',
};
const MATERIAL_MUEBLE_LABELS = {
    CEDRO: 'Cedro', NOGAL: 'Nogal', CAOBA: 'Caoba', ROBLE: 'Roble',
    HIERRO_FORJADO: 'Hierro forjado', MADERA_TALLADA: 'Madera tallada', MIXTO: 'Mixto', OTRO: 'Otro',
};

// Estado del AFD real -> { label visible, clase de badge }. No confundir con
// AFD_STATES (js/data/afd-estados.js), que describe el AFD del mockup con
// códigos q0..q16 ficticios y no corresponde al enum EstadoAntiguedad real.
const ESTADO_ANTIGUEDAD_INFO = {
    PIEZA_CAPTURADA: { label: 'Pieza capturada', badge: 'badge-blue' },
    EN_EVALUACION: { label: 'En evaluación', badge: 'badge-orange' },
    CALCULANDO_PRESUPUESTO: { label: 'Calculando presupuesto', badge: 'badge-orange' },
    PRESUPUESTO_PRESENTADO: { label: 'Cotización recibida', badge: 'badge-gold' },
    PAGO_EN_ESCROW: { label: 'Pago en garantía', badge: 'badge-gold' },
    HORARIO_RECOLECCION_AGENDADO: { label: 'Recolección agendada', badge: 'badge-blue' },
    EN_RECOLECCION: { label: 'En recolección', badge: 'badge-blue' },
    RECIBIDO_EN_TALLER: { label: 'Recibida en el taller', badge: 'badge-blue' },
    EN_RESTAURACION: { label: 'En restauración', badge: 'badge-orange' },
    AVANCE_PUBLICADO: { label: 'En restauración · avances publicados', badge: 'badge-orange' },
    RESTAURACION_LISTA: { label: 'Restauración lista', badge: 'badge-green' },
    DECISION_EN_TALLER: { label: 'Esperando tu decisión', badge: 'badge-gold' },
    HORARIO_ENTREGA_AGENDADO: { label: 'Entrega agendada', badge: 'badge-blue' },
    EN_DEVOLUCION_PROPIETARIO: { label: 'En camino de vuelta', badge: 'badge-blue' },
    ENTREGADO_AL_PROPIETARIO: { label: 'Entregada', badge: 'badge-green' },
    EN_CATALOGO: { label: 'Publicada en catálogo', badge: 'badge-green' },
    LINK_PAGO_ENVIADO: { label: 'Venta en proceso', badge: 'badge-gold' },
    SOLICITUD_DEVOLUCION_TALLER: { label: 'Solicitud de devolución', badge: 'badge-orange' },
    CANCELADA_NO_CUMPLE: { label: 'Cancelada: no cumple requisitos', badge: 'badge-red' },
    CANCELADA_DUENO_DECLINO: { label: 'Cancelada: cotización rechazada', badge: 'badge-red' },
    CANCELADA_RECOLECCION: { label: 'Cancelada en recolección', badge: 'badge-red' },
    CANCELADA_ENTREGA: { label: 'Cancelada en entrega', badge: 'badge-red' },
    RESTAURACION_COMPLETADA: { label: 'Restauración completada', badge: 'badge-green' },
    VENTA_COMPLETADA: { label: 'Venta completada', badge: 'badge-green' },
    DEVUELTA_DESDE_TALLER: { label: 'Devuelta desde el taller', badge: 'badge-blue' },
};

// Placeholder consistente para piezas sin fotografía todavía: hoy no existe
// endpoint para subir la foto de portada (ESTADO_INICIAL) de una antigüedad,
// así que urlFotoPortada llega null salvo que se haya cargado por otra vía.
const PIEZA_IMG_PLACEHOLDER = 'https://images.unsplash.com/photo-1449247709967-d4461a6a6103?w=500&q=70';

function estadoAntiguedadInfo(estado) {
    return ESTADO_ANTIGUEDAD_INFO[estado] || { label: estado, badge: 'badge-gray' };
}

function tipoMuebleLabel(tipo) { return TIPO_MUEBLE_LABELS[tipo] || tipo; }
function estiloMuebleLabel(estilo) { return estilo ? (ESTILO_MUEBLE_LABELS[estilo] || estilo) : ''; }
function materialMuebleLabel(material) { return material ? (MATERIAL_MUEBLE_LABELS[material] || material) : ''; }

function fotoConFallback(url) { return url || PIEZA_IMG_PLACEHOLDER; }

window.Api = Api;
window.TIPO_MUEBLE_LABELS = TIPO_MUEBLE_LABELS;
window.ESTILO_MUEBLE_LABELS = ESTILO_MUEBLE_LABELS;
window.MATERIAL_MUEBLE_LABELS = MATERIAL_MUEBLE_LABELS;
window.ESTADO_ANTIGUEDAD_INFO = ESTADO_ANTIGUEDAD_INFO;
window.PIEZA_IMG_PLACEHOLDER = PIEZA_IMG_PLACEHOLDER;
window.estadoAntiguedadInfo = estadoAntiguedadInfo;
window.tipoMuebleLabel = tipoMuebleLabel;
window.estiloMuebleLabel = estiloMuebleLabel;
window.materialMuebleLabel = materialMuebleLabel;
window.fotoConFallback = fotoConFallback;
