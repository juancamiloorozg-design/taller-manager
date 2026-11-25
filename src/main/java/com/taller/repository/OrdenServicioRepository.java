package com.taller.repository;

import com.taller.model.EstadoOrden;
import com.taller.model.OrdenServicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrdenServicioRepository extends JpaRepository<OrdenServicio, Long> {

    // open-in-view esta desactivado: todo lo que la vista necesite debe venir cargado desde aqui.

    @Query(value = "SELECT o FROM OrdenServicio o JOIN FETCH o.vehiculo v JOIN FETCH v.cliente",
            countQuery = "SELECT COUNT(o) FROM OrdenServicio o")
    Page<OrdenServicio> listar(Pageable pageable);

    @Query(value = "SELECT o FROM OrdenServicio o JOIN FETCH o.vehiculo v JOIN FETCH v.cliente "
            + "WHERE o.estado = :estado",
            countQuery = "SELECT COUNT(o) FROM OrdenServicio o WHERE o.estado = :estado")
    Page<OrdenServicio> listarPorEstado(@Param("estado") EstadoOrden estado, Pageable pageable);

    @Query("SELECT o FROM OrdenServicio o JOIN FETCH o.vehiculo v JOIN FETCH v.cliente "
            + "LEFT JOIN FETCH o.mecanico LEFT JOIN FETCH o.detalle d LEFT JOIN FETCH d.repuesto WHERE o.id = :id")
    Optional<OrdenServicio> findByIdConDetalle(@Param("id") Long id);

    List<OrdenServicio> findByVehiculoIdOrderByIdDesc(Long vehiculoId);

    @Query("SELECT o FROM OrdenServicio o JOIN FETCH o.vehiculo v WHERE v.cliente.id = :clienteId ORDER BY o.id DESC")
    List<OrdenServicio> findByClienteId(@Param("clienteId") Long clienteId);

    boolean existsByVehiculoId(Long vehiculoId);

    long countByEstado(EstadoOrden estado);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM OrdenServicio o WHERE o.estado = :estado AND o.finalizadoEn >= :desde")
    BigDecimal sumaTotalDesde(@Param("estado") EstadoOrden estado, @Param("desde") LocalDateTime desde);

    @Query("SELECT o FROM OrdenServicio o JOIN FETCH o.vehiculo v JOIN FETCH v.cliente "
            + "WHERE o.estado = :estado AND o.proximaFecha IS NOT NULL AND o.recordatorioAtendido = false "
            + "AND o.proximaFecha <= :limite ORDER BY o.proximaFecha")
    List<OrdenServicio> recordatoriosPendientes(@Param("estado") EstadoOrden estado, @Param("limite") LocalDate limite);

    @Query("SELECT o FROM OrdenServicio o JOIN FETCH o.vehiculo v LEFT JOIN FETCH o.mecanico "
            + "WHERE o.estado = :estado AND o.finalizadoEn >= :desde AND o.finalizadoEn < :hasta "
            + "ORDER BY o.finalizadoEn")
    List<OrdenServicio> finalizadasEntre(@Param("estado") EstadoOrden estado,
                                         @Param("desde") LocalDateTime desde,
                                         @Param("hasta") LocalDateTime hasta);

    @Query("SELECT o FROM OrdenServicio o JOIN FETCH o.vehiculo v JOIN FETCH v.cliente "
            + "LEFT JOIN FETCH o.mecanico WHERE o.estado IN :estados ORDER BY o.id")
    List<OrdenServicio> abiertas(@Param("estados") java.util.Collection<EstadoOrden> estados);

    @Query("SELECT o FROM OrdenServicio o JOIN FETCH o.vehiculo v JOIN FETCH v.cliente "
            + "WHERE o.estado = :estado ORDER BY o.id")
    List<OrdenServicio> listarPorEstadoCompleto(@Param("estado") EstadoOrden estado);

    @Query("SELECT r.nombre, SUM(l.cantidad), SUM(l.cantidad * l.precioUnitario) "
            + "FROM OrdenRepuesto l JOIN l.repuesto r JOIN l.orden o "
            + "WHERE o.estado = :estado AND o.finalizadoEn >= :desde AND o.finalizadoEn < :hasta "
            + "GROUP BY r.id, r.nombre ORDER BY SUM(l.cantidad) DESC")
    List<Object[]> repuestosMasUsados(@Param("estado") EstadoOrden estado,
                                      @Param("desde") LocalDateTime desde,
                                      @Param("hasta") LocalDateTime hasta,
                                      Pageable limite);
}
