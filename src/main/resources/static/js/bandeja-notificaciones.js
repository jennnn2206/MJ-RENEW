(() => {
    document.addEventListener('DOMContentLoaded', async () => {
        const panel = document.querySelector('[data-notificaciones]');
        if (!panel) return;
        const buscar = nombre => panel.querySelector(`[data-notificaciones-${nombre}]`);
        const lista = buscar('lista');
        const estado = buscar('estado');
        let pagina = 0, cargando = false, sesionVigente = true;
        const usuario = await Auth.actualizarDesdeServidor();
        if (!usuario) return;
        panel.hidden = false;

        function limpiar() {
            sesionVigente = false;
            lista.replaceChildren();
            buscar('contador').textContent = '0 sin leer';
            panel.hidden = true;
        }

        async function cargar() {
            if (cargando) return;
            cargando = true;
            estado.textContent = 'Cargando notificaciones…';
            try {
                const soloNoLeidas = buscar('filtro').checked;
                const [datos, resumen] = await Promise.all([
                    Api.get(`/api/notificaciones/obtenerMisNotificaciones?page=${pagina}&size=10&soloNoLeidas=${soloNoLeidas}`),
                    Api.get('/api/notificaciones/obtenerResumen'),
                ]);
                if (!sesionVigente) return;
                if (pagina > 0 && !datos.content.length) {
                    pagina = Math.max(0, datos.totalPages - 1);
                    cargando = false;
                    return cargar();
                }
                buscar('contador').textContent = `${resumen.noLeidas} sin leer`;
                lista.replaceChildren();
                for (const n of datos.content) {
                    const articulo = document.createElement('article');
                    articulo.className = n.leidoEn ? 'notificacion-item' : 'notificacion-item notificacion-no-leida';
                    const titulo = document.createElement('h4'); titulo.textContent = n.titulo;
                    const texto = document.createElement('p'); texto.textContent = n.mensaje;
                    const fecha = document.createElement('time'); fecha.dateTime = n.creadoEn;
                    fecha.textContent = new Date(n.creadoEn).toLocaleString('es-MX');
                    const acciones = document.createElement('div'); acciones.className = 'notificacion-acciones';
                    const ver = document.createElement('button'); ver.type = 'button';
                    ver.className = 'btn btn-ghost btn-sm'; ver.textContent = 'Ver antigüedad';
                    ver.addEventListener('click', () => {
                        if (typeof window.abrirDetallePieza === 'function') window.abrirDetallePieza(n.antiguedadId);
                        else if (typeof window.abrirGestionAsignada === 'function') window.abrirGestionAsignada(n.antiguedadId);
                    });
                    acciones.append(ver);
                    if (!n.leidoEn) {
                        const leer = document.createElement('button'); leer.type = 'button';
                        leer.className = 'btn btn-primary btn-sm'; leer.textContent = 'Marcar como leída';
                        leer.addEventListener('click', async () => {
                            leer.disabled = true;
                            try {
                                await Api.post(`/api/notificaciones/marcarComoLeida/${encodeURIComponent(n.id)}`);
                                await cargar();
                            } catch (error) {
                                if (error.status === 401 || error.status === 403) limpiar();
                                estado.textContent = error.message;
                                leer.disabled = false;
                            }
                        });
                        acciones.append(leer);
                    } else {
                        const leida = document.createElement('span'); leida.textContent = 'Leída'; acciones.append(leida);
                    }
                    articulo.append(titulo, texto, fecha, acciones); lista.append(articulo);
                }
                estado.textContent = datos.content.length ? '' : 'No hay notificaciones en esta página.';
                buscar('anterior').disabled = pagina === 0;
                buscar('siguiente').disabled = datos.last;
                buscar('pagina').textContent = datos.totalPages ? `Página ${pagina + 1} de ${datos.totalPages}` : 'Sin resultados';
            } catch (error) {
                if (error.status === 401 || error.status === 403) limpiar();
                estado.textContent = error.message;
            } finally { cargando = false; }
        }

        buscar('actualizar').addEventListener('click', cargar);
        buscar('filtro').addEventListener('change', () => { if (!cargando) { pagina = 0; cargar(); } });
        buscar('anterior').addEventListener('click', () => { if (!cargando && pagina > 0) { pagina--; cargar(); } });
        buscar('siguiente').addEventListener('click', () => { if (!cargando) { pagina++; cargar(); } });
        window.addEventListener('storage', evento => { if (evento.key === 'mjrenew_user') limpiar(); });
        await cargar();
        setInterval(() => { if (!panel.hidden && document.visibilityState === 'visible') cargar(); }, 60000);
    });
})();
