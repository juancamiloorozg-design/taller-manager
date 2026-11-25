'use client';

import { useEffect, useState } from 'react';
import { ajustarRepuesto, abonado, cambiarEstado, fechaCorta, guardarOrden, hoy, lunes,
  moverDia, pesos, registrarAbono, retirarRepuesto, saldo, total } from '../lib/taller.mjs';

const KEY = 'taller-manager-demo-v1';
const menu = [
  ['Panel', '▦'], ['Agenda', '▤'], ['Órdenes', '≡'], ['Clientes', '◉'],
  ['Vehículos', '◇'], ['Repuestos', '▧'], ['Recordatorios', '◷'], ['Informes', '▥'], ['Respaldos', '⇩']
];
const siguienteEstado = { Cotizada: 'Pendiente', Pendiente: 'En proceso', 'En proceso': 'Finalizada' };
const money = valor => pesos.format(valor);
const num = (form, key) => Number(form.get(key));
const str = (form, key) => String(form.get(key) ?? '').trim();
const patronPlaca = /^(?:[A-Z]{3}\d{3}|[A-Z]{3}\d{2}[A-Z])$/;

function Field({ label, name, type = 'text', value, required = true, min, max, maxLength, children }) {
  return <label className="field">{label}{children ?? <input name={name} type={type} defaultValue={value} required={required} min={min} max={max} maxLength={maxLength} />}</label>;
}

function Select({ label, name, value, options }) {
  return <Field label={label}><select name={name} defaultValue={value ?? ''} required>
    <option value="" disabled>Selecciona</option>
    {options.map(([id, texto]) => <option value={id} key={id}>{texto}</option>)}
  </select></Field>;
}

function Grid({ headers, children, empty = 'No hay registros.' }) {
  return <div className="table-wrap"><table><thead><tr>{headers.map(h => <th key={h}>{h}</th>)}</tr></thead>
    <tbody>{children.length ? children : <tr><td colSpan={headers.length} className="muted">{empty}</td></tr>}</tbody></table></div>;
}

function Badge({ estado }) {
  return <span className={'badge ' + (estado === 'Finalizada' ? 'green' : estado === 'Cancelada' ? 'gray' : estado === 'En proceso' ? 'amber' : '')}>{estado}</span>;
}

function download(name, content, type) {
  const url = URL.createObjectURL(new Blob([content], { type }));
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function csvCell(valor) {
  const texto = String(valor ?? '');
  return '"' + (/^[=+\-@\t\r]/.test(texto) ? "'" : '') + texto.replaceAll('"', '""') + '"';
}

const tipos = { cliente: 'clientes', vehiculo: 'vehiculos', cita: 'citas', repuesto: 'repuestos' };

export default function Taller({ inicial }) {
  const [db, setDb] = useState(inicial);
  const [cargado, setCargado] = useState(false);
  const [seccion, setSeccion] = useState('Panel');
  const [semana, setSemana] = useState(lunes(inicial.citas[0].fecha));
  const [busqueda, setBusqueda] = useState('');
  const [seleccion, setSeleccion] = useState(null);
  const [form, setForm] = useState(null);
  const [aviso, setAviso] = useState('');
  const [error, setError] = useState('');
  const [menuAbierto, setMenuAbierto] = useState(false);
  const [desde, setDesde] = useState('');
  const [hasta, setHasta] = useState('');

  useEffect(() => {
    try {
      const anterior = localStorage.getItem(KEY);
      if (anterior) setDb(JSON.parse(anterior));
    } catch {
      setError('La demo abrió con los datos de ejemplo.');
    }
    setCargado(true);
  }, []);

  const cliente = id => db.clientes.find(c => c.id === id);
  const vehiculo = id => db.vehiculos.find(v => v.id === id);
  const placa = id => vehiculo(id)?.placa ?? '—';
  const opcionesVehiculos = db.vehiculos.map(v => [v.id, v.placa + ' · ' + v.modelo + ' · ' + cliente(v.clienteId)?.nombre]);
  const orden = db.ordenes.find(o => o.id === seleccion);
  const listaInformes = db.ordenes.filter(o => o.estado === 'Finalizada' && (!desde || o.fecha >= desde) && (!hasta || o.fecha <= hasta));
  const abiertas = db.ordenes.filter(o => ['Cotizada', 'Pendiente', 'En proceso'].includes(o.estado));
  const recordatorios = db.ordenes.filter(o => o.estado === 'Finalizada' && o.proximo && o.proximo <= moverDia(hoy(), 30));
  const mostrar = (texto, ...valores) => valores.some(v => String(v ?? '').toLocaleLowerCase('es-CO').includes(texto.toLocaleLowerCase('es-CO')));
  const q = busqueda.trim();

  function navegar(nombre) {
    setSeccion(nombre); setSeleccion(null); setForm(null); setBusqueda(''); setMenuAbierto(false); setAviso(''); setError('');
  }

  function abrirOrden(id) {
    navegar('Órdenes'); setSeleccion(id);
  }

  function cambiar(tarea, mensaje) {
    setError(''); setAviso('');
    if (!cargado) return false;
    try {
      const copia = structuredClone(db);
      const resultado = tarea(copia);
      if (mensaje) copia.auditoria.unshift({ fecha: hoy(), texto: mensaje });
      localStorage.setItem(KEY, JSON.stringify(copia));
      setDb(copia); setAviso(mensaje);
      return resultado ?? true;
    } catch (e) {
      setError(e.name === 'QuotaExceededError' ? 'El almacenamiento del navegador está lleno. Retira algunas fotos.' : e.message);
      return false;
    }
  }

  function guardar(event) {
    event.preventDefault();
    const f = new FormData(event.currentTarget);
    const edicion = form.valor;
    const tipo = form.tipo;
    const resultado = cambiar(copia => {
      if (tipo === 'orden') {
        const descripcion = str(f, 'descripcion');
        if (!descripcion) throw Error('Describe el trabajo a realizar.');
        return guardarOrden(copia, {
          citaId: form.cita?.id, vehiculoId: num(f, 'vehiculoId'), descripcion, manoObra: num(f, 'manoObra'),
          kilometraje: str(f, 'kilometraje') ? num(f, 'kilometraje') : null,
          combustible: str(f, 'combustible'), danos: str(f, 'danos'), objetos: str(f, 'objetos'), cotizacion: f.has('cotizacion')
        });
      }
      let datos;
      if (tipo === 'cliente') {
        datos = { nombre: str(f, 'nombre'), documento: str(f, 'documento'), celular: str(f, 'celular'), correo: str(f, 'correo'), autoriza: f.has('autoriza') };
        if (!datos.nombre || !datos.documento || !datos.autoriza) throw Error('Completa los datos y registra la autorización.');
        if (copia.clientes.some(c => c.documento === datos.documento && c.id !== edicion?.id)) throw Error('El documento ya está registrado.');
      }
      if (tipo === 'vehiculo') {
        datos = { clienteId: num(f, 'clienteId'), placa: str(f, 'placa').toUpperCase(), modelo: str(f, 'modelo'), anio: num(f, 'anio') };
        if (!patronPlaca.test(datos.placa) || !datos.modelo) throw Error('Verifica la placa y el modelo.');
        if (copia.vehiculos.some(v => v.placa === datos.placa && v.id !== edicion?.id)) throw Error('La placa ya está registrada.');
      }
      if (tipo === 'cita') {
        datos = { vehiculoId: num(f, 'vehiculoId'), fecha: str(f, 'fecha'), hora: str(f, 'hora'), motivo: str(f, 'motivo'), ordenId: null };
        if (datos.fecha < hoy() || !datos.motivo) throw Error('Elige una fecha vigente y escribe el motivo.');
        if (copia.citas.some(c => c.vehiculoId === datos.vehiculoId && c.fecha === datos.fecha && c.hora === datos.hora && !c.cancelada && c.id !== edicion?.id)) throw Error('El vehículo ya tiene una cita a esa hora.');
      }
      if (tipo === 'repuesto') {
        datos = { nombre: str(f, 'nombre'), codigo: str(f, 'codigo'), precio: num(f, 'precio'), stock: num(f, 'stock'), minimo: num(f, 'minimo') };
        if (!datos.nombre || !datos.codigo) throw Error('Completa el nombre y el código.');
        if (copia.repuestos.some(r => r.codigo === datos.codigo && r.id !== edicion?.id)) throw Error('El código ya está registrado.');
      }
      const registros = copia[tipos[tipo]];
      if (edicion) Object.assign(registros.find(r => r.id === edicion.id), datos);
      else registros.unshift({ id: copia.siguiente++, ...datos });
      return datos.fecha ?? true;
    }, tipo === 'orden' ? 'Orden guardada.' : 'Registro guardado.');
    if (!resultado) return;
    setForm(null);
    if (tipo === 'orden') { setSeccion('Órdenes'); setSeleccion(resultado); setBusqueda(''); }
    if (tipo === 'cita') setSemana(lunes(resultado));
  }

  function editar(tipo, valor) { setAviso(''); setError(''); setForm({ tipo, valor }); }

  function actualizarOrden(tarea, mensaje) {
    return cambiar(copia => tarea(copia, copia.ordenes.find(o => o.id === orden.id)), mensaje);
  }

  async function foto(event) {
    const archivo = event.target.files[0];
    if (!archivo) return;
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(archivo.type) || archivo.size > 2_000_000) {
      setError('Selecciona una imagen JPG, PNG o WEBP de hasta 2 MB.'); return;
    }
    if (orden.fotos.length >= 5) { setError('Esta orden admite hasta 5 fotos en la demo.'); return; }
    const lector = new FileReader();
    lector.onload = () => actualizarOrden((_, o) => o.fotos.push({ nombre: archivo.name, url: lector.result }), 'Foto agregada.');
    lector.onerror = () => setError('No se pudo leer la imagen.');
    lector.readAsDataURL(archivo);
    event.target.value = '';
  }

  const gridOrdenes = filas => <Grid headers={['Orden', 'Vehículo', 'Trabajo', 'Estado', 'Total']}>
    {filas.map(o => <tr key={o.id}><td><button className="link" onClick={() => abrirOrden(o.id)}>#{o.id}</button></td><td>{placa(o.vehiculoId)}</td><td>{o.descripcion}</td><td><Badge estado={o.estado} /></td><td>{money(total(o))}</td></tr>)}
  </Grid>;

  const tituloForm = form && ({ cliente: 'Cliente', vehiculo: 'Vehículo', cita: 'Cita', repuesto: 'Repuesto', orden: 'Orden de servicio' })[form.tipo];

  return <div className="shell">
    <aside className={menuAbierto ? 'sidebar open' : 'sidebar'}>
      <button className="brand" onClick={() => navegar('Panel')}><span>TM</span><strong>Taller Manager<small>Gestión de taller</small></strong></button>
      <nav aria-label="Módulos">{menu.map(([nombre, icono]) => <button key={nombre} className={seccion === nombre ? 'active' : ''} onClick={() => navegar(nombre)}><span aria-hidden="true">{icono}</span>{nombre}</button>)}</nav>
      <div className="sidebar-foot">Datos ficticios · COP</div>
    </aside>
    <div className="main">
      <header className="top"><button className="mobile secondary" onClick={() => setMenuAbierto(!menuAbierto)} aria-expanded={menuAbierto}>Menú</button>
        <span>Taller Manager / {seccion}</span><span className="demo-label">Demo de portafolio</span></header>
      <main>
        <div className="heading"><div><h1>{orden && seccion === 'Órdenes' ? 'Orden #' + orden.id : seccion === 'Panel' ? 'Panel del taller' : seccion}</h1>
          <p>{seccion === 'Panel' ? 'Consulta el estado de las órdenes, las citas y el inventario.' : ({ Agenda: 'Consulta el calendario de atención en hora de Colombia.', Órdenes: 'Cotizaciones, recepción y seguimiento del servicio.', Clientes: 'Datos de contacto y autorización.', Vehículos: 'Registro por placa e historial de servicio.', Repuestos: 'Existencias y precios del taller.', Recordatorios: 'Próximos mantenimientos programados.', Informes: 'Órdenes finalizadas y cartera por cobrar.', Respaldos: 'Exportación de datos y registro de cambios.' })[seccion]}</p>
        </div>
        {!form && !seleccion && ['Agenda', 'Órdenes', 'Clientes', 'Vehículos', 'Repuestos'].includes(seccion) && <button className="primary" onClick={() => editar(({Agenda:'cita', Órdenes:'orden', Clientes:'cliente', Vehículos:'vehiculo', Repuestos:'repuesto'})[seccion])}>
          {({Agenda:'Agendar cita', Órdenes:'Nueva orden', Clientes:'Nuevo cliente', Vehículos:'Nuevo vehículo', Repuestos:'Nuevo repuesto'})[seccion]}</button>}
        </div>
        {aviso && <p className="notice" role="status">{aviso}</p>}
        {error && <p className="notice error" role="alert">{error}</p>}

        {form ? <section className="card form-card"><h2>{form.valor ? 'Editar' : 'Registrar'} {tituloForm.toLowerCase()}</h2>
          <form onSubmit={guardar} className="fields">
            {form.tipo === 'cliente' && <>
              <Field label="Nombre" name="nombre" value={form.valor?.nombre} maxLength={120} />
              <Field label="Documento" name="documento" value={form.valor?.documento} maxLength={30} />
              <Field label="Celular" name="celular" type="tel" value={form.valor?.celular} required={false} maxLength={20} />
              <Field label="Correo" name="correo" type="email" value={form.valor?.correo} required={false} />
              <label className="check"><input name="autoriza" type="checkbox" defaultChecked={form.valor?.autoriza} required /> El cliente autorizó el tratamiento de sus datos.</label>
            </>}
            {form.tipo === 'vehiculo' && <>
              <Select label="Cliente" name="clienteId" value={form.valor?.clienteId} options={db.clientes.map(c => [c.id, c.nombre])} />
              <Field label="Placa" name="placa" value={form.valor?.placa} maxLength={6} />
              <Field label="Marca y modelo" name="modelo" value={form.valor?.modelo} maxLength={100} />
              <Field label="Año" name="anio" type="number" min={1900} max={Number(hoy().slice(0, 4)) + 1} value={form.valor?.anio} />
            </>}
            {['cita', 'orden'].includes(form.tipo) && (form.cita ? <p className="wide">Vehículo de la cita <strong>{placa(form.cita.vehiculoId)}</strong></p> :
              <Select label="Vehículo" name="vehiculoId" value={form.valor?.vehiculoId} options={opcionesVehiculos} />)}
            {form.tipo === 'cita' && <>
              <Field label="Fecha" name="fecha" type="date" min={hoy()} value={form.valor?.fecha ?? hoy()} />
              <Field label="Hora" name="hora" type="time" value={form.valor?.hora ?? '09:00'} />
              <Field label="Motivo"><textarea name="motivo" defaultValue={form.valor?.motivo} maxLength={500} required /></Field>
            </>}
            {form.tipo === 'orden' && <>
              <Field label="Trabajo a realizar"><textarea name="descripcion" defaultValue={form.cita?.motivo} maxLength={500} required /></Field>
              <Field label="Mano de obra (COP)" name="manoObra" type="number" min={0} max={999999999} value={0} />
              <Field label="Kilometraje" name="kilometraje" type="number" min={0} max={3000000} required={false} />
              <Field label="Combustible"><select name="combustible"><option value="">Sin registrar</option>{['Vacío','Un cuarto','Medio','Tres cuartos','Lleno'].map(v => <option key={v}>{v}</option>)}</select></Field>
              <Field label="Daños visibles" name="danos" required={false} maxLength={500} />
              <Field label="Objetos recibidos" name="objetos" required={false} maxLength={500} />
              <label className="check"><input name="cotizacion" type="checkbox" /> Crear como cotización</label>
            </>}
            {form.tipo === 'repuesto' && <>
              <Field label="Nombre" name="nombre" value={form.valor?.nombre} maxLength={100} />
              <Field label="Código" name="codigo" value={form.valor?.codigo} maxLength={40} />
              {[['Precio (COP)','precio'],['Existencias','stock'],['Stock mínimo','minimo']].map(([label,name]) => <Field key={name} label={label} name={name} type="number" min={0} max={999999999} value={form.valor?.[name] ?? 0} />)}
            </>}
            <div className="actions wide"><button className="primary" disabled={!cargado}>Guardar</button><button type="button" className="secondary" onClick={() => { setForm(null); setError(''); }}>Volver</button></div>
          </form></section> : <>
          {seccion === 'Panel' && <>
            <div className="stats">
              {[['Órdenes abiertas', abiertas.length, 'Órdenes'], ['Citas agendadas', db.citas.filter(c => !c.cancelada && !c.ordenId && c.fecha >= hoy()).length, 'Agenda'],
                ['Saldo por cobrar', money(db.ordenes.filter(o => o.estado !== 'Cancelada').reduce((s, o) => s + saldo(o), 0)), 'Informes'],
                ['Repuestos con stock bajo', db.repuestos.filter(r => r.stock <= r.minimo).length, 'Repuestos']].map(([label,value,dest]) =>
                <button type="button" className="stat" key={label} onClick={() => navegar(dest)}><span>{label}</span><strong>{value}</strong></button>)}
            </div>
            <section className="card"><div className="card-head"><h2>Órdenes activas</h2><button className="link" onClick={() => navegar('Órdenes')}>Ver órdenes</button></div>{gridOrdenes(abiertas.slice(0, 6))}</section>
            <section className="card"><div className="card-head"><h2>Próximas citas</h2><button className="link" onClick={() => navegar('Agenda')}>Abrir agenda</button></div>
              <Grid headers={['Fecha', 'Vehículo', 'Motivo']}>{db.citas.filter(c => !c.cancelada && !c.ordenId && c.fecha >= hoy()).sort((a,b) => (a.fecha + a.hora).localeCompare(b.fecha + b.hora)).slice(0, 5).map(c => <tr key={c.id}><td>{fechaCorta(c.fecha)} · {c.hora}</td><td>{placa(c.vehiculoId)}</td><td>{c.motivo}</td></tr>)}</Grid>
            </section>
          </>}
          {seccion === 'Agenda' && <section className="card"><div className="card-head"><h2>{fechaCorta(semana)} al {fechaCorta(moverDia(semana, 6))}</h2>
            <div className="actions"><button className="secondary" onClick={() => setSemana(moverDia(semana, -7))}>Anterior</button><button className="secondary" onClick={() => setSemana(lunes(hoy()))}>Esta semana</button><button className="secondary" onClick={() => setSemana(moverDia(semana, 7))}>Siguiente</button></div></div>
            <Grid headers={['Fecha y hora', 'Vehículo y cliente', 'Motivo', 'Acciones']} empty="No hay citas en esta semana.">
              {db.citas.filter(c => c.fecha >= semana && c.fecha <= moverDia(semana, 6)).sort((a,b) => (a.fecha + a.hora).localeCompare(b.fecha + b.hora)).map(c => <tr key={c.id}>
                <td>{fechaCorta(c.fecha)} · {c.hora}</td><td><strong>{placa(c.vehiculoId)}</strong><small>{cliente(vehiculo(c.vehiculoId)?.clienteId)?.nombre}</small></td><td>{c.motivo}</td>
                <td>{c.cancelada ? <Badge estado="Cancelada" /> : c.ordenId ? <button className="link" onClick={() => abrirOrden(c.ordenId)}>Ver orden #{c.ordenId}</button> : <div className="actions">
                  <button className="link" onClick={() => { setForm({tipo:'orden', cita:c}); setError(''); }}>Abrir orden</button>
                  <button className="link" onClick={() => editar('cita', c)}>Editar</button>
                  <button className="link muted" onClick={() => cambiar(d => { d.citas.find(x => x.id === c.id).cancelada = true; }, 'Cita cancelada.')}>Cancelar cita</button></div>}</td>
              </tr>)}
            </Grid></section>}
          {['Órdenes', 'Clientes', 'Vehículos', 'Repuestos'].includes(seccion) && !seleccion && <section className="card">
            <div className="card-head"><h2>{seccion === 'Órdenes' ? 'Órdenes de servicio' : seccion + ' registrados'}</h2>
              <input type="search" aria-label={'Buscar en ' + seccion} value={busqueda} onChange={e => setBusqueda(e.target.value)} placeholder="Buscar…" /></div>
            {seccion === 'Órdenes' && gridOrdenes(db.ordenes.filter(o => mostrar(q, o.id, o.descripcion, o.estado, placa(o.vehiculoId))))}
            {seccion === 'Clientes' && <Grid headers={['Nombre','Documento','Contacto','Acción']}>{db.clientes.filter(c => mostrar(q,c.nombre,c.documento,c.celular,c.correo)).map(c =>
              <tr key={c.id}><td>{c.nombre}</td><td>{c.documento}</td><td>{c.celular || c.correo || '—'}</td><td><button className="link" onClick={() => editar('cliente',c)}>Editar</button></td></tr>)}</Grid>}
            {seccion === 'Vehículos' && <Grid headers={['Placa','Vehículo','Cliente','Acciones']}>{db.vehiculos.filter(v => mostrar(q,v.placa,v.modelo,cliente(v.clienteId)?.nombre)).map(v =>
              <tr key={v.id}><td>{v.placa}</td><td>{v.modelo} · {v.anio}</td><td>{cliente(v.clienteId)?.nombre}</td><td><div className="actions"><button className="link" onClick={() => setSeleccion(v.id)}>Historial</button><button className="link" onClick={() => editar('vehiculo',v)}>Editar</button></div></td></tr>)}</Grid>}
            {seccion === 'Repuestos' && <Grid headers={['Código','Repuesto','Precio','Existencias','Acción']}>{db.repuestos.filter(r => mostrar(q,r.codigo,r.nombre)).map(r =>
              <tr key={r.id}><td>{r.codigo}</td><td>{r.nombre}</td><td>{money(r.precio)}</td><td>{r.stock}{r.stock <= r.minimo && <> · <Badge estado="Stock bajo" /></>}</td><td><button className="link" onClick={() => editar('repuesto',r)}>Editar</button></td></tr>)}</Grid>}
          </section>}
          {seccion === 'Vehículos' && seleccion && <section className="card"><div className="card-head"><h2>Historial de {placa(seleccion)}</h2><button className="secondary" onClick={() => setSeleccion(null)}>Volver</button></div>{gridOrdenes(db.ordenes.filter(o => o.vehiculoId === seleccion))}</section>}

          {seccion === 'Órdenes' && orden && <div className="order">
            <section className="card"><div className="card-head"><h2>{placa(orden.vehiculoId)} · {orden.descripcion}</h2><Badge estado={orden.estado} /></div>
              <dl className="details"><div><dt>Fecha de ingreso</dt><dd>{fechaCorta(orden.fecha)}</dd></div><div><dt>Kilometraje</dt><dd>{orden.kilometraje ?? '—'}</dd></div><div><dt>Combustible</dt><dd>{orden.combustible || '—'}</dd></div><div><dt>Mano de obra</dt><dd>{money(orden.manoObra)}</dd></div><div><dt>Daños visibles</dt><dd>{orden.danos || '—'}</dd></div><div><dt>Objetos recibidos</dt><dd>{orden.objetos || '—'}</dd></div></dl>
              <div className="actions no-print"><button className="secondary" onClick={() => setSeleccion(null)}>Volver a órdenes</button><button className="secondary" onClick={() => window.print()}>Imprimir</button>
                {siguienteEstado[orden.estado] && <><button className="primary" onClick={() => actualizarOrden((d,o) => cambiarEstado(d,o,siguienteEstado[o.estado]), 'Estado actualizado.')}>{({Cotizada:'Aprobar cotización',Pendiente:'Iniciar trabajo','En proceso':'Finalizar orden'})[orden.estado]}</button>
                <button className="secondary" onClick={() => actualizarOrden((d,o) => cambiarEstado(d,o,'Cancelada'), 'Orden cancelada.')}>Cancelar orden</button></>}
              </div></section>
            <section className="card"><div className="card-head"><h2>Repuestos usados</h2></div><Grid headers={['Repuesto','Cantidad','Subtotal','Acción']}>{orden.items.map(i => <tr key={i.id}><td>{i.nombre}</td><td>{i.cantidad}</td><td>{money(i.precio * i.cantidad)}</td><td>{siguienteEstado[orden.estado] && <button className="link no-print" onClick={() => actualizarOrden((d,o) => retirarRepuesto(d,o,i.id), 'Repuesto retirado.')}>Retirar</button>}</td></tr>)}</Grid>
              {siguienteEstado[orden.estado] && <form className="inline-form no-print" onSubmit={e => { e.preventDefault(); const f = new FormData(e.currentTarget); if (actualizarOrden((d,o) => ajustarRepuesto(d,o,num(f,'repuesto'),num(f,'cantidad')), 'Repuesto agregado.')) e.currentTarget.reset(); }}>
                <Select label="Repuesto" name="repuesto" options={db.repuestos.filter(r => r.stock > 0).map(r => [r.id, r.nombre + ' · ' + r.stock + ' disponibles'])} /><Field label="Cantidad" name="cantidad" type="number" min={1} value={1} /><button className="primary">Agregar</button></form>}</section>
            <section className="card"><div className="card-head"><h2>Abonos</h2></div><dl className="details"><div><dt>Total</dt><dd>{money(total(orden))}</dd></div><div><dt>Abonado</dt><dd>{money(abonado(orden))}</dd></div><div><dt>Saldo</dt><dd>{money(saldo(orden))}</dd></div></dl>
              <Grid headers={['Fecha','Medio','Valor','Acción']}>{orden.abonos.map((a,i) => <tr key={i}><td>{fechaCorta(a.fecha)}</td><td>{a.medio}</td><td>{money(a.valor)}</td><td><button className="link no-print" onClick={() => actualizarOrden((_,o) => o.abonos.splice(i,1), 'Abono anulado.')}>Anular</button></td></tr>)}</Grid>
              {orden.estado !== 'Cancelada' && saldo(orden) > 0 && <form className="inline-form no-print" onSubmit={e => { e.preventDefault(); const f = new FormData(e.currentTarget); if (actualizarOrden((_,o) => registrarAbono(o,num(f,'valor'),str(f,'medio')), 'Abono registrado.')) e.currentTarget.reset(); }}>
                <Field label="Valor (COP)" name="valor" type="number" min={1} max={saldo(orden)} /><Select label="Medio de pago" name="medio" options={['Efectivo','Transferencia','Tarjeta'].map(x => [x,x])} /><button className="primary">Registrar abono</button></form>}</section>
            <section className="card"><div className="card-head"><h2>Fotos de recepción</h2></div><div className="inside">
              <div className="photos">{orden.fotos.map((f,i) => <figure key={i}><img src={f.url} alt={f.nombre} /><button className="link no-print" onClick={() => actualizarOrden((_,o) => o.fotos.splice(i,1), 'Foto retirada.')}>Retirar foto</button></figure>)}</div>
              {orden.estado !== 'Cancelada' && <label className="field no-print">Agregar foto<input type="file" accept="image/jpeg,image/png,image/webp" onChange={foto} /></label>}
            </div></section>
            {orden.estado === 'Finalizada' && <section className="card no-print"><div className="card-head"><h2>Próximo servicio</h2></div><form className="inline-form" onSubmit={e => { e.preventDefault(); const fecha = str(new FormData(e.currentTarget),'proximo'); actualizarOrden((_,o) => {o.proximo = fecha;}, 'Recordatorio guardado.'); }}>
              <Field label="Fecha del próximo mantenimiento" name="proximo" type="date" min={hoy()} value={orden.proximo} /><button className="primary">Guardar fecha</button></form></section>}
          </div>}

          {seccion === 'Recordatorios' && <section className="card"><div className="card-head"><h2>Mantenimientos por contactar</h2></div><Grid headers={['Vehículo','Cliente','Próximo servicio','Acciones']}>
            {recordatorios.map(o => { const c = cliente(vehiculo(o.vehiculoId)?.clienteId); const telefono = c?.celular?.replace(/\D/g,'').replace(/^57(?=3\d{9}$)/,''); return <tr key={o.id}><td>{placa(o.vehiculoId)}</td><td>{c?.nombre}</td><td>{fechaCorta(o.proximo)}</td><td><div className="actions"><button className="link" onClick={() => abrirOrden(o.id)}>Ver orden</button>
              {c?.autoriza && /^3\d{9}$/.test(telefono ?? '') && <a className="link" href={'https://wa.me/57' + telefono + '?text=' + encodeURIComponent('Hola, te recordamos el próximo mantenimiento de tu vehículo ' + placa(o.vehiculoId) + ' el ' + o.proximo + '.')} target="_blank" rel="noreferrer">WhatsApp</a>}</div></td></tr>; })}
          </Grid></section>}
          {seccion === 'Informes' && <><section className="card"><div className="card-head"><h2>Órdenes finalizadas</h2><div className="actions"><label>Desde <input aria-label="Desde" type="date" value={desde} onChange={e => setDesde(e.target.value)} /></label><label>Hasta <input aria-label="Hasta" type="date" value={hasta} onChange={e => setHasta(e.target.value)} /></label>
            <button className="secondary" onClick={() => download('informe-taller.csv', '\uFEFF' + [['Orden','Placa','Fecha','Total','Abonado','Saldo'],...listaInformes.map(o => [o.id,placa(o.vehiculoId),o.fecha,total(o),abonado(o),saldo(o)])].map(linea => linea.map(csvCell).join(';')).join('\r\n'), 'text/csv;charset=utf-8')}>Descargar CSV</button></div></div>
            <dl className="details"><div><dt>Órdenes</dt><dd>{listaInformes.length}</dd></div><div><dt>Total en órdenes</dt><dd>{money(listaInformes.reduce((s,o) => s + total(o), 0))}</dd></div><div><dt>Por cobrar</dt><dd>{money(listaInformes.reduce((s,o) => s + saldo(o), 0))}</dd></div></dl>{gridOrdenes(listaInformes)}</section>
            <section className="card"><div className="card-head"><h2>Saldos pendientes</h2></div><Grid headers={['Orden','Vehículo','Saldo']}>{db.ordenes.filter(o => o.estado !== 'Cancelada' && saldo(o) > 0).map(o => <tr key={o.id}><td><button className="link" onClick={() => abrirOrden(o.id)}>#{o.id}</button></td><td>{placa(o.vehiculoId)}</td><td>{money(saldo(o))}</td></tr>)}</Grid></section></>}
          {seccion === 'Respaldos' && <><section className="card"><div className="card-head"><h2>Exportar datos de la demo</h2><button className="primary" onClick={() => download('taller-manager-' + hoy() + '.json', JSON.stringify(db,null,2), 'application/json')}>Descargar respaldo</button></div><div className="inside"><p className="muted">Los cambios de esta demo se guardan en este navegador. Usa datos ficticios.</p></div></section>
            <section className="card"><div className="card-head"><h2>Registro de cambios</h2></div><Grid headers={['Fecha','Acción']}>{db.auditoria.map((a,i) => <tr key={i}><td>{fechaCorta(a.fecha)}</td><td>{a.texto}</td></tr>)}</Grid></section></>}
        </>}
      </main>
    </div>
  </div>;
}
