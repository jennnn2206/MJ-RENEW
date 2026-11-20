async function cargarCobros(panel, avisar = false) {
  const lista = panel.querySelector('[data-movimientos]');
  try {
    const cobros = await Api.get('/api/' + panel.dataset.cobros + '/obtenerMisCobros');
    const mxn = new Intl.NumberFormat('es-MX', {style: 'currency', currency: 'MXN'});
    const importe = valor => valor == null ? 'pendiente' : mxn.format(valor);
    lista.replaceChildren();
    if (!cobros.length) lista.textContent = 'Todavía no tienes cobros.';
    for (const cobro of cobros) {
      const fila = document.createElement('p');
      fila.textContent = `Pieza ${cobro.antiguedadId} · ${cobro.estado === 'EXITOSA' ? 'Transferido a Stripe' : 'Pendiente'} · Total ${importe(cobro.bruto)} · Comisión MJ Renew ${importe(cobro.comision)} · Cargo Stripe ${importe(cobro.cargoStripe)} · Neto ${importe(cobro.neto)}`;
      lista.append(fila);
      if (avisar && usuarioCobros && (cobro.estado === 'EXITOSA' || panel.dataset.cobros === 'propietario')) {
        const clave = `mjrenew-aviso-${usuarioCobros}-${cobro.antiguedadId}-${panel.dataset.cobros}-${cobro.estado}`;
        if (!localStorage.getItem(clave)) {
          const mensaje = cobro.estado === 'EXITOSA'
            ? `Pago transferido. Neto: ${importe(cobro.neto)}. Consulta el desglose en Mis cobros.`
            : 'Se confirmó una venta. Puedes consultar el pago en Mis cobros.';
          Toast.show(mensaje, 'success', 7000);
          localStorage.setItem(clave, '1');
        }
      }
    }
  } catch (err) { lista.textContent = err.message; }
}

async function configurarCobros(boton) {
  const panel = boton.closest('[data-cobros]');
  boton.disabled = true;
  try {
    const enlace = await Api.post('/api/' + panel.dataset.cobros + '/iniciarOnboardingCuentaStripe');
    window.location.assign(enlace.urlOnboarding);
  } catch (err) {
    panel.querySelector('[data-movimientos]').textContent = err.message;
    boton.disabled = false;
  }
}

const usuarioCobros = Auth.getCurrentUser()?.id;
const panelCobros = document.querySelector('[data-cobros]');
if (panelCobros && usuarioCobros) {
  cargarCobros(panelCobros, true);
  setInterval(() => {
    if (!document.hidden) cargarCobros(panelCobros, true);
  }, 60000);
}
