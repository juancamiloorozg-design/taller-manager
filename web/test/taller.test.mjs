import test from 'node:test';
import assert from 'node:assert/strict';
import { ajustarRepuesto, cambiarEstado, crearDemo, guardarOrden, registrarAbono, retirarRepuesto, saldo, lunes } from '../lib/taller.mjs';

test('convierte una cita en una sola orden y conserva el vehículo de la cita', () => {
  const db = crearDemo('2026-11-25');
  const datos = { citaId: 7, vehiculoId: 5, descripcion: 'Cambio de aceite', manoObra: 50000 };
  const id = guardarOrden(db, datos);
  assert.equal(guardarOrden(db, datos), id);
  assert.equal(db.ordenes.length, 3);
  assert.equal(db.ordenes[0].vehiculoId, 4);
  assert.equal(db.citas[0].ordenId, id);
});

test('actualiza existencias y las devuelve al retirar un repuesto o cancelar', () => {
  const db = crearDemo('2026-11-25');
  const orden = db.ordenes[0];
  ajustarRepuesto(db, orden, 9, 2);
  assert.equal(db.repuestos[0].stock, 10);
  assert.throws(() => ajustarRepuesto(db, orden, 9, 11));
  retirarRepuesto(db, orden, 9);
  assert.equal(db.repuestos[0].stock, 12);
  ajustarRepuesto(db, orden, 9, 3);
  assert.throws(() => cambiarEstado(db, orden, 'Cancelada'));
  orden.abonos.length = 0;
  cambiarEstado(db, orden, 'Cancelada');
  assert.equal(db.repuestos[0].stock, 12);
});

test('registra abonos hasta el saldo y empieza la semana en lunes', () => {
  const orden = crearDemo('2026-11-25').ordenes[0];
  assert.throws(() => registrarAbono(orden, 55001, 'Efectivo'));
  registrarAbono(orden, 55000, 'Efectivo');
  assert.equal(saldo(orden), 0);
  assert.equal(lunes('2026-11-29'), '2026-11-23');
});
