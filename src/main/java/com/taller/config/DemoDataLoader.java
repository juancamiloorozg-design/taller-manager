package com.taller.config;

import com.taller.model.Cliente;
import com.taller.model.MetodoPago;
import com.taller.model.NivelCombustible;
import com.taller.model.OrdenServicio;
import com.taller.model.Rol;
import com.taller.model.Usuario;
import com.taller.model.Vehiculo;
import com.taller.repository.ClienteRepository;
import com.taller.service.ClienteService;
import com.taller.service.OrdenServicioService;
import com.taller.service.PagoService;
import com.taller.service.RepuestoService;
import com.taller.service.UsuarioService;
import com.taller.service.VehiculoService;
import com.taller.util.Tiempo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Datos ficticios para probar y mostrar la aplicacion. Solo corre con taller.demo-data=true
 * y con la base sin clientes. Ningun dato corresponde a personas reales.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "taller.demo-data", havingValue = "true")
public class DemoDataLoader implements CommandLineRunner {

    private static final String ACTOR = "demo";

    private final ClienteRepository clientes;
    private final ClienteService clienteService;
    private final VehiculoService vehiculoService;
    private final RepuestoService repuestoService;
    private final OrdenServicioService ordenService;
    private final PagoService pagoService;
    private final UsuarioService usuarioService;
    private final JdbcTemplate jdbc;
    private final String mecanicoEmail;
    private final String mecanicoPassword;

    public DemoDataLoader(ClienteRepository clientes, ClienteService clienteService,
                          VehiculoService vehiculoService, RepuestoService repuestoService,
                          OrdenServicioService ordenService, PagoService pagoService,
                          UsuarioService usuarioService, JdbcTemplate jdbc,
                          @Value("${taller.demo.mecanico-email:mecanico@taller.local}") String mecanicoEmail,
                          @Value("${taller.demo.mecanico-password:Mecanico12345!}") String mecanicoPassword) {
        this.clientes = clientes;
        this.clienteService = clienteService;
        this.vehiculoService = vehiculoService;
        this.repuestoService = repuestoService;
        this.ordenService = ordenService;
        this.pagoService = pagoService;
        this.usuarioService = usuarioService;
        this.jdbc = jdbc;
        this.mecanicoEmail = mecanicoEmail;
        this.mecanicoPassword = mecanicoPassword;
    }

    @Override
    public void run(String... args) {
        sembrar();
    }

    public void sembrar() {
        if (clientes.count() > 0) {
            return;
        }
        Usuario carlos = usuarioService.crear(mecanicoEmail, "Carlos Meza", mecanicoPassword, Rol.MECANICO, ACTOR);
        Usuario jorge = usuarioService.crear("jorge@taller.local", "Jorge Arrieta", mecanicoPassword, Rol.MECANICO, ACTOR);

        Map<String, Long> cli = new HashMap<>();
        cli.put("ana", cliente("1020304050", "Ana María Rojas", "3001234567", "ana.rojas@example.com", "Calle 76 # 54-20, Barranquilla"));
        cli.put("luis", cliente("79123456", "Luis Fernando Pardo", "3109876543", null, null));
        cli.put("marta", cliente("32456789", "Marta Lucía Vega", "3125550142", "marta.vega@example.com", "Carrera 43 # 84-15, Barranquilla"));
        cli.put("diego", cliente("1045678901", "Diego Alejandro Cantillo", "3015550188", null, null));
        cli.put("sandra", cliente("22345678", "Sandra Patricia Mejía", "3205550167", "sandra.mejia@example.com", null));
        cli.put("hector", cliente("72987654", "Héctor Julio Barrios", "3175550123", null, "Calle 30 # 17-45, Soledad"));
        cli.put("paola", cliente("1129876543", "Paola Andrea Ríos", "3045550199", "paola.rios@example.com", null));
        cli.put("ramiro", cliente("8765432", "Ramiro Ospino", "3135550111", null, null));

        Map<String, Long> veh = new HashMap<>();
        veh.put("mazda", vehiculo("ABC123", "Mazda", "3", 2018, "Gris", cli.get("ana")));
        veh.put("duster", vehiculo("XYZ98D", "Renault", "Duster", 2021, "Blanco", cli.get("luis")));
        veh.put("spark", vehiculo("KJH452", "Chevrolet", "Spark GT", 2016, "Rojo", cli.get("marta")));
        veh.put("hilux", vehiculo("TRM871", "Toyota", "Hilux", 2019, "Negro", cli.get("diego")));
        veh.put("picanto", vehiculo("LQP309", "Kia", "Picanto", 2020, "Azul", cli.get("sandra")));
        veh.put("sail", vehiculo("BNV624", "Chevrolet", "Sail", 2014, "Plata", cli.get("hector")));
        veh.put("clio", vehiculo("GFD118", "Renault", "Clio", 2012, "Blanco", cli.get("paola")));
        veh.put("accent", vehiculo("WSX507", "Hyundai", "Accent", 2017, "Gris", cli.get("ramiro")));
        veh.put("cx5", vehiculo("MNB236", "Mazda", "CX-5", 2022, "Rojo", cli.get("ana")));

        Map<String, Long> rep = new LinkedHashMap<>();
        rep.put("filtro", repuesto("FIL-001", "Filtro de aceite", "28000", 20, 5));
        rep.put("aceite", repuesto("ACE-5W30", "Aceite 5W-30 (litro)", "42000", 40, 10));
        rep.put("pastillas", repuesto("PAS-014", "Pastillas de freno delanteras", "145000", 6, 4));
        rep.put("discos", repuesto("DIS-022", "Discos de freno delanteros (par)", "320000", 3, 2));
        rep.put("bujias", repuesto("BUJ-004", "Bujías (juego de 4)", "96000", 8, 3));
        rep.put("aire", repuesto("FIL-020", "Filtro de aire", "35000", 12, 4));
        rep.put("bateria", repuesto("BAT-012", "Batería 12V 60Ah", "390000", 2, 3));
        rep.put("correa", repuesto("COR-310", "Correa de accesorios", "78000", 5, 3));
        rep.put("amort", repuesto("AMO-101", "Amortiguador delantero", "260000", 4, 2));
        rep.put("liquido", repuesto("LIQ-007", "Líquido de frenos DOT 4", "24000", 15, 5));
        rep.put("refrig", repuesto("REF-050", "Refrigerante (galón)", "38000", 10, 4));
        rep.put("limpia", repuesto("LIM-009", "Plumillas limpiaparabrisas (par)", "45000", 9, 3));

        // Historial de meses anteriores, para que los informes tengan con que comparar
        OrdenServicio h1 = orden(veh.get("sail"), "Cambio de aceite y filtro", 38200, "50000", NivelCombustible.MEDIO,
                null, null, carlos, false, Map.of(rep.get("filtro"), 1, rep.get("aceite"), 3), 3);
        pagar(h1, null, MetodoPago.EFECTIVO);
        OrdenServicio h2 = orden(veh.get("clio"), "Cambio de filtro de aire y revisión general", 91000, "70000", NivelCombustible.UN_CUARTO,
                null, null, jorge, false, Map.of(rep.get("aire"), 1), 3);
        pagar(h2, null, MetodoPago.TRANSFERENCIA);
        OrdenServicio h3 = orden(veh.get("picanto"), "Cambio de plumillas y alineación", 30100, "90000", NivelCombustible.MEDIO,
                null, null, carlos, false, Map.of(rep.get("limpia"), 1), 3);
        pagar(h3, null, MetodoPago.TARJETA);
        OrdenServicio h4 = orden(veh.get("accent"), "Cambio de bujías", 76500, "60000", NivelCombustible.TRES_CUARTOS,
                null, null, jorge, false, Map.of(rep.get("bujias"), 1), 3);
        pagar(h4, null, MetodoPago.EFECTIVO);

        // Finalizadas, a lo largo del mes
        OrdenServicio o1 = orden(veh.get("mazda"), "Cambio de aceite y filtro. Revisión de niveles", 52000, "60000", NivelCombustible.MEDIO,
                "Rayón leve en la puerta trasera derecha", null, carlos, false, Map.of(rep.get("filtro"), 1, rep.get("aceite"), 4), 3);
        pagar(o1, null, MetodoPago.EFECTIVO);
        OrdenServicio o2 = orden(veh.get("hilux"), "Cambio de pastillas y discos delanteros", 88400, "180000", NivelCombustible.UN_CUARTO,
                null, "Gato y llanta de repuesto en el baúl", jorge, false,
                Map.of(rep.get("pastillas"), 1, rep.get("discos"), 1, rep.get("liquido"), 1), 3);
        pagar(o2, "300000", MetodoPago.TRANSFERENCIA);
        OrdenServicio o3 = orden(veh.get("spark"), "Cambio de bujías y filtro de aire", 71200, "90000", NivelCombustible.MEDIO,
                null, null, carlos, false, Map.of(rep.get("bujias"), 1, rep.get("aire"), 1), 3);
        pagar(o3, null, MetodoPago.TARJETA);
        OrdenServicio o4 = orden(veh.get("duster"), "Cambio de amortiguadores delanteros", 64300, "150000", NivelCombustible.TRES_CUARTOS,
                "Golpe en el parachoques delantero", null, jorge, false, Map.of(rep.get("amort"), 2), 3);
        pagar(o4, "400000", MetodoPago.EFECTIVO);
        OrdenServicio o5 = orden(veh.get("accent"), "Cambio de correa de accesorios y refrigerante", 95800, "110000", NivelCombustible.MEDIO,
                null, null, carlos, false, Map.of(rep.get("correa"), 1, rep.get("refrig"), 2), 3);
        pagar(o5, null, MetodoPago.TRANSFERENCIA);

        // En proceso
        OrdenServicio o6 = orden(veh.get("picanto"), "Cambio de batería y revisión del sistema eléctrico", 41500, "50000", NivelCombustible.UN_CUARTO,
                null, null, carlos, false, Map.of(rep.get("bateria"), 1), 2);
        pagar(o6, "150000", MetodoPago.EFECTIVO);
        OrdenServicio o7 = orden(veh.get("cx5"), "Mantenimiento de 20.000 km", 20150, "120000", NivelCombustible.LLENO,
                null, "Paraguas en el asiento trasero", jorge, false,
                Map.of(rep.get("filtro"), 1, rep.get("aceite"), 5, rep.get("aire"), 1), 2);

        // Pendientes
        orden(veh.get("sail"), "Revisión de frenos y ruido al girar", 134000, "40000", NivelCombustible.MEDIO,
                "Espejo derecho flojo", null, null, false, Map.of(rep.get("liquido"), 1), 1);
        orden(veh.get("clio"), "Cambio de limpiaparabrisas y revisión de luces", 156700, "30000", NivelCombustible.UN_CUARTO,
                null, null, null, false, Map.of(rep.get("limpia"), 1), 1);

        // Cotizadas
        orden(veh.get("duster"), "Cambio de pastillas delanteras", 64500, "70000", NivelCombustible.MEDIO,
                null, null, null, true, Map.of(rep.get("pastillas"), 1), 0);
        orden(veh.get("hilux"), "Cambio de aceite, filtros y bujías", 89000, "100000", NivelCombustible.UN_CUARTO,
                null, null, null, true, Map.of(rep.get("aceite"), 6, rep.get("filtro"), 1, rep.get("aire"), 1, rep.get("bujias"), 1), 0);

        // Cancelada
        OrdenServicio cancelada = orden(veh.get("mazda"), "Cambio de amortiguador (el cliente decidió esperar)", 52500, "80000",
                NivelCombustible.MEDIO, null, null, null, true, Map.of(rep.get("amort"), 1), 0);
        ordenService.cancelar(cancelada.getId(), ACTOR);

        // Proximos servicios
        ordenService.programarRecordatorio(o1.getId(), Tiempo.ahora().toLocalDate().plusDays(60), 57000, ACTOR);
        ordenService.programarRecordatorio(o3.getId(), Tiempo.ahora().toLocalDate().plusDays(30), 76000, ACTOR);
        ordenService.programarRecordatorio(o5.getId(), Tiempo.ahora().toLocalDate().plusDays(90), 105000, ACTOR);
        // Estos dos ya vencieron: aparecen en la lista de vehiculos por contactar
        ordenService.programarRecordatorio(o2.getId(), Tiempo.ahora().toLocalDate().plusDays(1), 93000, ACTOR);
        ordenService.programarRecordatorio(o4.getId(), Tiempo.ahora().toLocalDate().plusDays(1), 69000, ACTOR);
        jdbc.update("UPDATE ordenes_servicio SET proxima_fecha = ? WHERE id IN (?, ?)",
                Tiempo.ahora().toLocalDate().minusDays(4), o2.getId(), o4.getId());

        // Reparte las fechas de las ordenes a lo largo de las ultimas semanas
        fechas(h1, 128, 127);
        fechas(h2, 97, 96);
        fechas(h3, 66, 65);
        fechas(h4, 41, 40);
        fechas(o1, 26, 25);
        fechas(o2, 19, 17);
        fechas(o3, 12, 11);
        fechas(o4, 8, 6);
        fechas(o5, 3, 2);
        fechas(o6, 1, null);
        fechas(o7, 0, null);
    }

    private Long cliente(String documento, String nombre, String telefono, String correo, String direccion) {
        Cliente c = clienteService.crear(new ClienteService.Datos("CC", documento, nombre, telefono, correo, direccion), true, ACTOR);
        return c.getId();
    }

    private Long vehiculo(String placa, String marca, String modelo, int anio, String color, Long clienteId) {
        Vehiculo v = vehiculoService.crear(new VehiculoService.Datos(placa, marca, modelo, anio, color, clienteId), ACTOR);
        return v.getId();
    }

    private Long repuesto(String codigo, String nombre, String precio, int stock, int minimo) {
        return repuestoService.crear(new RepuestoService.Datos(codigo, nombre, new BigDecimal(precio), stock, minimo), ACTOR).getId();
    }

    /** Crea la orden y la lleva al estado indicado: 0 queda como esta, 1 pendiente, 2 en proceso, 3 finalizada. */
    private OrdenServicio orden(Long vehiculoId, String descripcion, int km, String manoObra, NivelCombustible nivel,
                                String danos, String objetos, Usuario mecanico, boolean cotizacion,
                                Map<Long, Integer> repuestos, int pasos) {
        OrdenServicio o = ordenService.crearConRepuestos(vehiculoId,
                new OrdenServicioService.DatosOrden(descripcion, km, new BigDecimal(manoObra), nivel, danos, objetos,
                        mecanico == null ? null : mecanico.getId()),
                cotizacion, repuestos, ACTOR);
        int avances = cotizacion ? pasos : Math.max(0, pasos - 1);
        for (int i = 0; i < avances; i++) {
            ordenService.avanzar(o.getId(), ACTOR);
        }
        return o;
    }

    /** Abona el total (monto nulo) o un valor parcial. */
    private void pagar(OrdenServicio o, String monto, MetodoPago metodo) {
        BigDecimal valor = monto == null ? ordenService.obtener(o.getId()).getTotal() : new BigDecimal(monto);
        pagoService.registrar(o.getId(), valor, metodo, null, ACTOR);
    }

    private void fechas(OrdenServicio o, int diasCreada, Integer diasFinalizada) {
        LocalDateTime creada = Tiempo.ahora().minusDays(diasCreada);
        LocalDateTime fin = diasFinalizada == null ? null : Tiempo.ahora().minusDays(diasFinalizada);
        jdbc.update("UPDATE ordenes_servicio SET creado_en = ?, actualizado_en = ?, finalizado_en = ? WHERE id = ?",
                creada, fin == null ? creada : fin, fin, o.getId());
    }
}
