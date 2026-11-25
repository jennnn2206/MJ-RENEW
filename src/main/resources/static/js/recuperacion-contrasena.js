(() => {
    const mostrarResultado = (elemento, resultado) => {
        elemento.textContent = resultado.success ? resultado.mensaje : resultado.error;
        elemento.dataset.state = resultado.success ? 'success' : 'error';
    };

    const solicitud = document.getElementById('recuperacion-form');
    if (solicitud) {
        solicitud.addEventListener('submit', async evento => {
            evento.preventDefault();
            const boton = document.getElementById('recuperacion-submit');
            const correo = document.getElementById('email').value.trim();
            boton.disabled = true;
            const resultado = await Auth.solicitarRecuperacionContrasena(correo);
            mostrarResultado(document.getElementById('recuperacion-resultado'), resultado);
            boton.disabled = false;
        });
    }

    const formulario = document.getElementById('restablecer-form');
    if (!formulario) return;
    let token = new URLSearchParams(window.location.hash.slice(1)).get('token');
    // El secreto no se envía en la URL HTTP ni queda en la barra de direcciones.
    window.history.replaceState(null, '', window.location.pathname);
    const mensaje = document.getElementById('restablecer-resultado');
    if (!token || !/^[A-Za-z0-9_-]{43}$/.test(token)) {
        mostrarResultado(mensaje, { success: false, error: 'Abre el enlace de recuperación de tu correo o solicita uno nuevo.' });
        return;
    }
    formulario.hidden = false;
    formulario.addEventListener('submit', async evento => {
        evento.preventDefault();
        const nueva = document.getElementById('nueva-contrasena');
        const confirmacion = document.getElementById('confirmar-contrasena');
        if (nueva.value.length < 8) {
            mostrarResultado(mensaje, { success: false, error: 'Usa una contraseña de al menos 8 caracteres.' });
            return;
        }
        if (new TextEncoder().encode(nueva.value).length > 72) {
            mostrarResultado(mensaje, { success: false, error: 'La contraseña es demasiado larga. Usa una más corta.' });
            return;
        }
        if (nueva.value !== confirmacion.value) {
            mostrarResultado(mensaje, { success: false, error: 'Las contraseñas no coinciden.' });
            return;
        }
        const boton = document.getElementById('restablecer-submit');
        boton.disabled = true;
        const resultado = await Auth.restablecerContrasena(token, nueva.value);
        mostrarResultado(mensaje, resultado);
        if (resultado.success) {
            token = null;
            nueva.value = '';
            confirmacion.value = '';
            formulario.hidden = true;
            document.getElementById('solicitar-otro-enlace').hidden = true;
            localStorage.removeItem('mjrenew_user');
        } else {
            boton.disabled = false;
        }
    });
})();
