async function cargarEntregasPropietario(pagina = 0) {
  const cont = document.getElementById('entregas-propietario-list');
  const navegacion = document.getElementById('entregas-propietario-paginacion');
  cont.textContent = 'Cargando solicitudes...';
  navegacion.replaceChildren();
  try {
    const [solicitudes, transportistas] = await Promise.all([
      Api.obtenerEntregasPropietarioPendientes(pagina), obtenerTransportistasParaEntrega()
    ]);
    cont.replaceChildren();
    if (!solicitudes.content.length) cont.textContent = 'No hay solicitudes en esta página.';
    solicitudes.content.forEach(solicitud => {
      const tarjeta = document.createElement('div');
      tarjeta.className = 'request-card';
      const datos = document.createElement('p');
      datos.textContent = `Antigüedad ${solicitud.antiguedadId} · Intento ${solicitud.numeroIntento} · ${solicitud.fechaAcordada}, de ${solicitud.horaInicio} a ${solicitud.horaFin}`;
      const direcciones = document.createElement('p');
      direcciones.textContent = `Recogida: ${solicitud.direccionOrigen}. Entrega: ${solicitud.direccionDestino}.`;
      const selector = document.createElement('select');
      selector.className = 'form-control';
      selector.setAttribute('aria-label', 'Transportista para esta entrega');
      selector.add(new Option('Selecciona un transportista', ''));
      transportistas.forEach(t => selector.add(new Option(t.nombreCompleto, t.id)));
      const error = document.createElement('p');
      error.setAttribute('role', 'alert');
      const asignar = document.createElement('button');
      asignar.className = 'btn btn-primary';
      asignar.textContent = 'Asignar transportista';
      asignar.onclick = async () => {
        if (!selector.value) { error.textContent = 'Selecciona un transportista.'; return; }
        asignar.disabled = true;
        try {
          await Api.asignarEntregaPropietario(solicitud.trasladoId, selector.value);
          Toast.show('Entrega agendada con el horario del propietario.', 'success');
          await cargarEntregasPropietario(pagina);
        } catch (err) { error.textContent = err.message; asignar.disabled = false; }
      };
      tarjeta.append(datos, direcciones, selector, asignar, error);
      cont.append(tarjeta);
    });
    for (const [texto, destino, visible] of [
      ['Anterior', pagina - 1, !solicitudes.first], ['Siguiente', pagina + 1, !solicitudes.last]
    ]) {
      if (visible) {
        const boton = document.createElement('button');
        boton.textContent = texto;
        boton.className = 'btn btn-ghost';
        boton.onclick = () => cargarEntregasPropietario(destino);
        navegacion.append(boton);
      }
    }
  } catch (err) { cont.textContent = err.message; }
}

async function obtenerTransportistasParaEntrega() {
  const transportistas = [];
  let pagina = 0;
  let usuarios;
  do {
    usuarios = await Api.obtenerUsuariosRegistrados(pagina++, 50);
    transportistas.push(...usuarios.content.filter(u => u.tipoUsuario === 'TRANSPORTISTA'));
  } while (!usuarios.last);
  return transportistas;
}
