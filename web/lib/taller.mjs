export const estados = ['Cotizada', 'Pendiente', 'En proceso', 'Finalizada', 'Cancelada'];
export const pesos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
export const total = orden => orden.manoObra + orden.items.reduce((suma, item) => suma + item.precio * item.cantidad, 0);
export const abonado = orden => orden.abonos.reduce((suma, abono) => suma + abono.valor, 0);
export const saldo = orden => total(orden) - abonado(orden);

export function hoy(fecha = new Date()) {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Bogota', year: 'numeric', month: '2-digit', day: '2-digit' }).format(fecha);
}

export function moverDia(fecha, cantidad) {
  const dia = new Date(fecha + 'T12:00:00Z');
  dia.setUTCDate(dia.getUTCDate() + cantidad);
  return dia.toISOString().slice(0, 10);
}

export function lunes(fecha) {
  return moverDia(fecha, -((new Date(fecha + 'T12:00:00Z').getUTCDay() + 6) % 7));
}

export function fechaCorta(fecha) {
  if (!fecha) return '—';
  return new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short', timeZone: 'UTC' })
    .format(new Date(fecha.slice(0, 10) + 'T12:00:00Z'));
}

export function crearDemo(fecha = hoy()) {
  return {
    siguiente: 30,
    clientes: [
      {id: 1, nombre: 'Ana María Rojas', documento: '1000000001', celular: '', correo: 'ana@example.com', autoriza: true},
      {id: 2, nombre: 'Luis Fernando Pardo', documento: '1000000002', celular: '', correo: '', autoriza: true},
      {id: 3, nombre: 'Marta Lucía Vega', documento: '1000000003', celular: '', correo: '', autoriza: true}
    ],
    vehiculos: [
      {id: 4, clienteId: 1, placa: 'ABC123', modelo: 'JAC J3', anio: 2011},
      {id: 5, clienteId: 2, placa: 'XYZ981', modelo: 'Renault Duster', anio: 2021},
      {id: 6, clienteId: 3, placa: 'KJH452', modelo: 'Chevrolet Spark GT', anio: 2016}
    ],
    citas: [
      {id: 7, vehiculoId: 4, fecha, hora: '09:00', motivo: 'Cambio de aceite y revisión de niveles', ordenId: null},
      {id: 8, vehiculoId: 6, fecha, hora: '10:30', motivo: 'Revisión de frenos', ordenId: null}
    ],
    repuestos: [
      {id: 9, nombre: 'Filtro de aceite', codigo: 'FIL-001', precio: 28000, stock: 12, minimo: 4},
      {id: 10, nombre: 'Aceite 5W-30 (litro)', codigo: 'ACE-001', precio: 42000, stock: 24, minimo: 8},
      {id: 11, nombre: 'Pastillas delanteras', codigo: 'PAS-014', precio: 145000, stock: 3, minimo: 4}
    ],
    ordenes: [
      {id: 12, vehiculoId: 5, descripcion: 'Revisión de suspensión delantera', estado: 'En proceso', fecha,
        manoObra: 85000, items: [], abonos: [{valor: 30000, medio: 'Efectivo', fecha}], fotos: [], combustible: 'Medio', kilometraje: 64800},
      {id: 13, vehiculoId: 6, descripcion: 'Cambio de filtro y revisión general', estado: 'Finalizada', fecha,
        manoObra: 60000, items: [{id: 9, nombre: 'Filtro de aceite', precio: 28000, cantidad: 1}],
        abonos: [{valor: 88000, medio: 'Transferencia', fecha}], fotos: [], proximo: moverDia(fecha, 20)}
    ],
    auditoria: []
  };
}

export function guardarOrden(db, datos) {
  const cita = datos.citaId ? db.citas.find(c => c.id === datos.citaId) : null;
  if (datos.citaId && (!cita || cita.cancelada)) throw Error('La cita ya no está disponible.');
  if (cita?.ordenId) return cita.ordenId;
  const vehiculoId = cita?.vehiculoId ?? datos.vehiculoId;
  if (!db.vehiculos.some(v => v.id === vehiculoId)) throw Error('Selecciona un vehículo.');
  const id = db.siguiente++;
  db.ordenes.unshift({...datos, id, vehiculoId, estado: datos.cotizacion ? 'Cotizada' : 'Pendiente',
    fecha: hoy(), items: [], abonos: [], fotos: []});
  if (cita) cita.ordenId = id;
  return id;
}

export function ajustarRepuesto(db, orden, repuestoId, cantidad) {
  if (['Finalizada', 'Cancelada'].includes(orden.estado)) throw Error('La orden ya está cerrada.');
  const repuesto = db.repuestos.find(r => r.id === repuestoId);
  if (!repuesto || !Number.isInteger(cantidad) || cantidad <= 0 || repuesto.stock < cantidad) {
    throw Error('Verifica la cantidad y el stock del repuesto.');
  }
  const item = orden.items.find(i => i.id === repuestoId);
  repuesto.stock -= cantidad;
  if (item) item.cantidad += cantidad;
  else orden.items.push({ id: repuesto.id, nombre: repuesto.nombre, precio: repuesto.precio, cantidad });
}

export function retirarRepuesto(db, orden, repuestoId) {
  if (['Finalizada', 'Cancelada'].includes(orden.estado)) throw Error('La orden ya está cerrada.');
  const item = orden.items.find(i => i.id === repuestoId);
  if (!item) return;
  db.repuestos.find(r => r.id === repuestoId).stock += item.cantidad;
  orden.items = orden.items.filter(i => i.id !== repuestoId);
}

export function cambiarEstado(db, orden, estado) {
  const siguiente = { Cotizada: 'Pendiente', Pendiente: 'En proceso', 'En proceso': 'Finalizada' };
  if (!siguiente[orden.estado] || ![siguiente[orden.estado], 'Cancelada'].includes(estado)) {
    throw Error('El cambio de estado no está disponible.');
  }
  if (estado === 'Cancelada') {
    if (abonado(orden)) throw Error('Anula los abonos antes de cancelar esta orden.');
    for (const item of orden.items) db.repuestos.find(r => r.id === item.id).stock += item.cantidad;
  }
  orden.estado = estado;
}

export function registrarAbono(orden, valor, medio) {
  if (orden.estado === 'Cancelada' || !Number.isSafeInteger(valor) || valor <= 0 || valor > saldo(orden)) {
    throw Error('El abono debe ser mayor a cero y no superar el saldo.');
  }
  orden.abonos.push({ valor, medio, fecha: hoy() });
}
