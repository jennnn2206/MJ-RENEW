// MJRENEW-COMPRADOR: historial real de compras pagadas y acuse de recepción.
// Nunca se determina la propiedad de la compra con un ID almacenado en el navegador:
// Spring Security resuelve al comprador a partir de la sesión HTTP.
const ComprasComprador = (() => {
    const listado = document.getElementById('compras-listado');
    const estado = document.getElementById('compras-estado');
    const botonMas = document.getElementById('compras-mas');
    const botonActualizar = document.getElementById('compras-actualizar');
    const TAMANO_PAGINA = 10;
    let pagina = 0;
    let cargando = false;
    let hayMas = false;
    let inicializada = false;
    let generacion = 0;

    function fechaLegible(valor) {
        if (!valor) return null;
        const fecha = new Date(valor);
        return Number.isNaN(fecha.getTime()) ? null : fecha.toLocaleDateString('es-MX', {
            year: 'numeric', month: 'long', day: 'numeric',
        });
    }

    function obtenerEstado(compra) {
        if (compra.recepcionConfirmadaEn) {
            return { texto: 'Recepción confirmada', clase: 'badge-green', descripcion: `Confirmaste la recepción el ${fechaLegible(compra.recepcionConfirmadaEn) || 'día indicado'}.` };
        }
        if (compra.puedeConfirmarRecepcion) {
            return { texto: 'Pendiente de tu confirmación', clase: 'badge-gold', descripcion: 'El transportista registró la entrega con evidencia fotográfica.' };
        }
        if (!compra.resultadoEntrega) {
            return { texto: 'Por agendar', clase: 'badge-gray', descripcion: 'Tu pago está confirmado. Aún no hay una entrega agendada.' };
        }
        switch (compra.resultadoEntrega) {
            case 'PENDIENTE':
                return compra.salidaConfirmadaEn
                    ? { texto: 'En camino', clase: 'badge-blue', descripcion: 'El transportista ha confirmado la salida.' }
                    : { texto: 'Entrega programada', clase: 'badge-blue', descripcion: compra.fechaEntregaAcordada ? `Fecha estimada: ${compra.fechaEntregaAcordada}.` : 'El transportista tiene una entrega pendiente.' };
            case 'EXITOSO':
                return { texto: 'Entrega registrada', clase: 'badge-orange', descripcion: 'La entrega está registrada, pero aún no se cumplen todos los requisitos de evidencia para confirmar recepción.' };
            case 'NADIE_EN_CASA':
                return { texto: 'Intento fallido', clase: 'badge-orange', descripcion: 'No fue posible realizar la entrega. Se deberá reagendar.' };
            case 'CANCELADO':
                return { texto: 'Entrega cancelada', clase: 'badge-red', descripcion: 'La entrega fue cancelada. Consulta el seguimiento.' };
            default:
                return { texto: 'En seguimiento', clase: 'badge-gray', descripcion: 'Consulta de nuevo para ver el estado de tu entrega.' };
        }
    }

    function elemento(tag, clase, texto) {
        const nodo = document.createElement(tag);
        if (clase) nodo.className = clase;
        if (texto != null) nodo.textContent = texto;
        return nodo;
    }

    function crearTarjeta(compra) {
        const tarjeta = elemento('article', 'compra-tarjeta');
        const imagen = elemento('img', 'compra-tarjeta-imagen');
        imagen.alt = `Antigüedad: ${tipoMuebleLabel(compra.tipoMueble)}`;
        imagen.loading = 'lazy';
        imagen.src = fotoConFallback(compra.urlFotoPortada);
        imagen.addEventListener('error', () => { imagen.onerror = null; imagen.src = PIEZA_IMG_PLACEHOLDER; }, { once: true });
        tarjeta.appendChild(imagen);

        const contenido = elemento('div', 'compra-tarjeta-cuerpo');
        contenido.appendChild(elemento('h3', '', tipoMuebleLabel(compra.tipoMueble)));
        contenido.appendChild(elemento('p', 'compra-tarjeta-precio', Utils.formatCurrency(compra.precioMxn)));
        if (compra.pagadaEn) {
            contenido.appendChild(elemento('p', 'compra-tarjeta-meta', `Pago confirmado: ${fechaLegible(compra.pagadaEn) || 'fecha no disponible'}`));
        }

        const info = obtenerEstado(compra);
        const estadoContenedor = elemento('div', 'compra-tarjeta-estado');
        estadoContenedor.appendChild(elemento('span', `badge ${info.clase}`, info.texto));
        estadoContenedor.appendChild(elemento('p', 'compra-tarjeta-meta', info.descripcion));
        contenido.appendChild(estadoContenedor);

        if (compra.puedeConfirmarRecepcion && !compra.recepcionConfirmadaEn) {
            const confirmar = elemento('button', 'btn btn-primary btn-sm', 'Confirmar recepción');
            confirmar.type = 'button';
            confirmar.addEventListener('click', async () => {
                if (!window.confirm('¿Confirmas que recibiste esta antigüedad? Esta confirmación no se puede revocar.')) return;
                confirmar.disabled = true;
                confirmar.textContent = 'Confirmando...';
                try {
                    await Api.confirmarRecepcionCompra(compra.catalogoId);
                    Toast.show('Recepción confirmada correctamente.', 'success');
                    await cargar(true);
                } catch (error) {
                    Toast.show(Utils.escapeHtml(error.message), 'error');
                    confirmar.disabled = false;
                    confirmar.textContent = 'Confirmar recepción';
                }
            });
            contenido.appendChild(confirmar);
        }
        tarjeta.appendChild(contenido);
        return tarjeta;
    }

    async function cargar(reiniciar = false) {
        if (cargando) return;
        cargando = true;
        const token = ++generacion;
        if (reiniciar) {
            pagina = 0;
            inicializada = false;
            listado.replaceChildren();
        }
        botonMas.disabled = true;
        botonActualizar.disabled = true;
        estado.textContent = 'Consultando tus compras...';
        try {
            const respuesta = await Api.obtenerMisCompras(pagina, TAMANO_PAGINA);
            if (token !== generacion) return;
            const compras = respuesta.content || [];
            compras.forEach(compra => listado.appendChild(crearTarjeta(compra)));
            pagina += 1;
            hayMas = respuesta.last === false;
            botonMas.hidden = !hayMas;
            inicializada = true;
            if (!listado.children.length) {
                estado.textContent = 'Aún no tienes compras con pago confirmado. Las piezas aparecerán aquí después de la confirmación de Stripe.';
            } else {
                estado.textContent = '';
            }
        } catch (error) {
            estado.textContent = `No se pudieron cargar tus compras: ${error.message}`;
            botonMas.hidden = true;
            if (error.status === 401) {
                window.location.assign('/login');
            }
        } finally {
            cargando = false;
            botonMas.disabled = false;
            botonActualizar.disabled = false;
        }
    }

    botonMas.addEventListener('click', () => { if (hayMas) cargar(); });
    botonActualizar.addEventListener('click', () => cargar(true));

    return { cargar: () => cargar(!inicializada) };
})();
window.ComprasComprador = ComprasComprador;
