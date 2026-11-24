// El fragmento del enlace no llega al servidor ni a sus logs. Retirarlo del historial al abrirlo.
const tokenVerificacion = new URLSearchParams(window.location.hash.slice(1)).get('token');
window.history.replaceState(null, '', window.location.pathname);
const botonVerificar = document.getElementById('verificar-btn');
const resultadoVerificacion = document.getElementById('resultado');
document.getElementById('email').value = sessionStorage.getItem('mjrenew_correo_verificacion') || '';
botonVerificar.hidden = !tokenVerificacion;

botonVerificar.addEventListener('click', async () => {
    botonVerificar.disabled = true;
    const resultado = await Auth.verificarCorreo(tokenVerificacion);
    resultadoVerificacion.textContent = resultado.success ? resultado.mensaje : resultado.error;
    if (resultado.success) {
        sessionStorage.removeItem('mjrenew_correo_verificacion');
        botonVerificar.hidden = true;
        document.getElementById('reenvio-form').hidden = true;
    } else {
        botonVerificar.disabled = false;
    }
});

document.getElementById('reenvio-form').addEventListener('submit', async evento => {
    evento.preventDefault();
    const boton = document.getElementById('reenviar-btn');
    const email = document.getElementById('email').value.trim();
    boton.disabled = true;
    const resultado = await Auth.reenviarVerificacion(email);
    resultadoVerificacion.textContent = resultado.success ? resultado.mensaje : resultado.error;
    boton.disabled = false;
});
