// Sin scripts en linea: la politica de seguridad del sitio solo permite archivos propios.
(function () {
    'use strict';

    // <form data-confirm="Mensaje"> pide confirmacion antes de enviar
    document.addEventListener('submit', function (evento) {
        var formulario = evento.target;
        var mensaje = formulario.getAttribute ? formulario.getAttribute('data-confirm') : null;
        if (mensaje && !window.confirm(mensaje)) {
            evento.preventDefault();
        }
    });

    // <button data-print> abre el cuadro de impresion
    document.addEventListener('click', function (evento) {
        if (evento.target.closest && evento.target.closest('[data-print]')) {
            window.print();
        }
    });

    // Marca en el menu lateral la seccion en la que esta el usuario
    var ruta = window.location.pathname;
    document.querySelectorAll('.sidebar .menu-link').forEach(function (enlace) {
        var destino = enlace.pathname;
        var activo = destino === '/' ? ruta === '/' : (ruta === destino || ruta.indexOf(destino + '/') === 0);
        if (activo) {
            enlace.setAttribute('aria-current', 'page');
        }
    });

    // Total estimado en el formulario de orden: repuestos (cantidad x precio) + mano de obra
    var etiquetaTotal = document.getElementById('total-estimado');
    if (etiquetaTotal) {
        var dar = function (numero) {
            return '$ ' + Math.round(numero).toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
        };
        var recalcular = function () {
            var suma = 0;
            document.querySelectorAll('input[data-precio]').forEach(function (campo) {
                var cantidad = parseInt(campo.value, 10);
                var precio = parseFloat(campo.getAttribute('data-precio'));
                if (!isNaN(cantidad) && cantidad > 0 && !isNaN(precio)) {
                    suma += cantidad * precio;
                }
            });
            var campoManoObra = document.getElementById('manoObra');
            var manoObra = campoManoObra ? parseFloat(campoManoObra.value) : 0;
            if (!isNaN(manoObra) && manoObra > 0) {
                suma += manoObra;
            }
            etiquetaTotal.textContent = dar(suma);
        };
        document.addEventListener('input', recalcular);
        recalcular();
    }
})();
